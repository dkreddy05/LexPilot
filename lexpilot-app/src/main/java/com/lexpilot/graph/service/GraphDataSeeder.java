package com.lexpilot.graph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexpilot.graph.entity.*;
import com.lexpilot.graph.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Seeds the graph database from {@code graphify-out/graph.json} on first boot.
 * <p>
 * This component runs once when the application starts. If the
 * {@code graph_repositories} table already has data, it does nothing
 * (idempotent). It reads the static graph.json file produced by the
 * external {@code graphifyy} CLI and persists all nodes, edges,
 * and hyperedges into PostgreSQL.
 */
@Component
public class GraphDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(GraphDataSeeder.class);

    private final GraphService graphService;
    private final GraphRepositoryJpaRepo repositoryRepo;
    private final GraphHyperedgeJpaRepo hyperedgeRepo;
    private final ObjectMapper objectMapper;

    @Value("${lexpilot.graph.seed-file:graphify-out/graph.json}")
    private String seedFilePath;

    @Value("${lexpilot.graph.auto-seed:true}")
    private boolean autoSeed;

    public GraphDataSeeder(GraphService graphService,
                           GraphRepositoryJpaRepo repositoryRepo,
                           GraphHyperedgeJpaRepo hyperedgeRepo,
                           ObjectMapper objectMapper) {
        this.graphService = graphService;
        this.repositoryRepo = repositoryRepo;
        this.hyperedgeRepo = hyperedgeRepo;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedOnStartup() {
        if (!autoSeed) {
            log.info("Graph auto-seeding is disabled (lexpilot.graph.auto-seed=false)");
            return;
        }

        if (repositoryRepo.count() > 0) {
            log.info("Graph data already exists ({} repositories) — skipping seed",
                    repositoryRepo.count());
            return;
        }

        Path graphFile = Path.of(seedFilePath);
        if (!Files.exists(graphFile)) {
            log.warn("Seed file not found at '{}' — skipping graph seeding", seedFilePath);
            return;
        }

        try {
            log.info("Seeding graph data from '{}'…", seedFilePath);
            seedFromGraphJson(graphFile);
            log.info("Graph seeding completed successfully");
        } catch (Exception e) {
            log.error("Failed to seed graph data from '{}': {}", seedFilePath, e.getMessage(), e);
        }
    }

    /**
     * Parses the graphify graph.json file and persists all entities.
     */
    public void seedFromGraphJson(Path graphJsonPath) throws IOException {
        String json = Files.readString(graphJsonPath);
        JsonNode root = objectMapper.readTree(json);

        // 1. Create the repository entry
        GraphRepositoryEntity repo = graphService.createRepository(
                "lexpilot", null, "main");
        UUID repoId = repo.getId();

        // 2. Parse and save nodes
        Map<String, UUID> externalIdToUuid = new HashMap<>();
        JsonNode nodesArray = root.get("nodes");

        if (nodesArray != null && nodesArray.isArray()) {
            List<GraphNodeEntity> nodes = new ArrayList<>();
            for (JsonNode nodeJson : nodesArray) {
                String externalId = nodeJson.get("id").asText();
                String label = nodeJson.has("label") ? nodeJson.get("label").asText() : externalId;

                GraphNodeEntity node = new GraphNodeEntity(repoId, externalId, label);
                node.setFileType(getTextOrNull(nodeJson, "file_type"));
                node.setSourceFile(getTextOrNull(nodeJson, "source_file"));
                node.setSourceLocation(getTextOrNull(nodeJson, "source_location"));
                node.setCommunity(nodeJson.has("community") ? nodeJson.get("community").asInt(-1) : -1);

                nodes.add(node);
            }

            List<GraphNodeEntity> savedNodes = graphService.saveAllNodes(nodes);
            for (GraphNodeEntity saved : savedNodes) {
                externalIdToUuid.put(saved.getExternalId(), saved.getId());
            }
            log.info("Seeded {} graph nodes", savedNodes.size());
        }

        // 3. Parse and save edges
        JsonNode linksArray = root.get("links");
        if (linksArray != null && linksArray.isArray()) {
            List<GraphEdgeEntity> edges = new ArrayList<>();
            int skipped = 0;

            for (JsonNode linkJson : linksArray) {
                String sourceExtId = linkJson.get("source").asText();
                String targetExtId = linkJson.get("target").asText();

                UUID sourceUuid = externalIdToUuid.get(sourceExtId);
                UUID targetUuid = externalIdToUuid.get(targetExtId);

                if (sourceUuid == null || targetUuid == null) {
                    skipped++;
                    continue;
                }

                String relation = getTextOrNull(linkJson, "relation");
                if (relation == null) relation = "unknown";

                GraphEdgeEntity edge = new GraphEdgeEntity(repoId, sourceUuid, targetUuid, relation);
                edge.setConfidence(getTextOrNull(linkJson, "confidence"));
                edge.setConfidenceScore(linkJson.has("confidence_score")
                        ? linkJson.get("confidence_score").asDouble(1.0) : 1.0);
                edge.setWeight(linkJson.has("weight")
                        ? linkJson.get("weight").asDouble(1.0) : 1.0);
                edge.setSourceFile(getTextOrNull(linkJson, "source_file"));
                edge.setSourceLocation(getTextOrNull(linkJson, "source_location"));

                edges.add(edge);
            }

            graphService.saveAllEdges(edges);
            log.info("Seeded {} graph edges (skipped {} with unresolved node references)",
                    edges.size(), skipped);
        }

        // 4. Parse and save hyperedges (if present)
        JsonNode hyperedgesArray = root.get("hyperedges");
        if (hyperedgesArray != null && hyperedgesArray.isArray() && !hyperedgesArray.isEmpty()) {
            int count = 0;
            for (JsonNode heJson : hyperedgesArray) {
                GraphHyperedgeEntity he = new GraphHyperedgeEntity(repoId,
                        heJson.has("label") ? heJson.get("label").asText() : "Unnamed");
                he.setDescription(getTextOrNull(heJson, "description"));
                he.setEdgeType(getTextOrNull(heJson, "edge_type"));
                hyperedgeRepo.save(he);

                // Save members
                JsonNode membersArray = heJson.get("members");
                if (membersArray != null && membersArray.isArray()) {
                    for (JsonNode memberJson : membersArray) {
                        String nodeExtId = memberJson.has("node_id")
                                ? memberJson.get("node_id").asText()
                                : memberJson.asText();
                        UUID nodeUuid = externalIdToUuid.get(nodeExtId);
                        if (nodeUuid != null) {
                            String role = memberJson.has("role")
                                    ? memberJson.get("role").asText() : "member";
                            GraphHyperedgeMemberEntity member =
                                    new GraphHyperedgeMemberEntity(he.getId(), nodeUuid, role);
                            // Save via entity manager (no dedicated repo method needed)
                            hyperedgeRepo.flush();
                        }
                    }
                }
                count++;
            }
            log.info("Seeded {} hyperedges", count);
        }

        // 5. Update repository counts and status
        repo.setAnalysisStatus(AnalysisStatus.COMPLETED);
        repositoryRepo.save(repo);
        graphService.updateRepositoryCounts(repoId);
    }

    private String getTextOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : null;
    }
}
