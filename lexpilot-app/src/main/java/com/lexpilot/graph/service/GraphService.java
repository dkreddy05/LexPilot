package com.lexpilot.graph.service;

import com.lexpilot.graph.dto.*;
import com.lexpilot.graph.entity.*;
import com.lexpilot.graph.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for graph read operations — node/edge lookups, community
 * listing, neighbor traversal, BFS shortest-path, and entity-to-DTO mapping.
 */
@Service
public class GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphService.class);

    private final GraphRepositoryJpaRepo repositoryRepo;
    private final GraphNodeJpaRepo nodeRepo;
    private final GraphEdgeJpaRepo edgeRepo;
    private final GraphHyperedgeJpaRepo hyperedgeRepo;

    public GraphService(GraphRepositoryJpaRepo repositoryRepo,
                        GraphNodeJpaRepo nodeRepo,
                        GraphEdgeJpaRepo edgeRepo,
                        GraphHyperedgeJpaRepo hyperedgeRepo) {
        this.repositoryRepo = repositoryRepo;
        this.nodeRepo = nodeRepo;
        this.edgeRepo = edgeRepo;
        this.hyperedgeRepo = hyperedgeRepo;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Repository operations
    // ────────────────────────────────────────────────────────────────────────

    public List<GraphOverviewResponse> listRepositories() {
        return repositoryRepo.findAll().stream()
                .map(this::toOverview)
                .toList();
    }

    public Optional<GraphOverviewResponse> getRepository(UUID repoId) {
        return repositoryRepo.findById(repoId).map(this::toOverview);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Node operations
    // ────────────────────────────────────────────────────────────────────────

    public Page<GraphNodeResponse> getNodes(UUID repoId, Pageable pageable) {
        return nodeRepo.findByRepositoryId(repoId, pageable)
                .map(this::toNodeResponseWithNeighbors);
    }

    public Optional<GraphNodeResponse> getNodeDetail(UUID nodeId) {
        return nodeRepo.findById(nodeId).map(this::toNodeResponseWithNeighbors);
    }

    public List<GraphNodeResponse> searchNodes(UUID repoId, String searchTerm, int maxResults) {
        return nodeRepo.searchByLabel(repoId, searchTerm.toLowerCase(), maxResults)
                .stream()
                .map(this::toNodeResponseWithNeighbors)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Edge operations
    // ────────────────────────────────────────────────────────────────────────

    public Page<GraphEdgeResponse> getEdges(UUID repoId, String relation, Pageable pageable) {
        Page<GraphEdgeEntity> edges;
        if (relation != null && !relation.isBlank()) {
            edges = edgeRepo.findByRepositoryIdAndRelation(repoId, relation, pageable);
        } else {
            edges = edgeRepo.findByRepositoryId(repoId, pageable);
        }
        return edges.map(this::toEdgeResponse);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Community operations
    // ────────────────────────────────────────────────────────────────────────

    public List<GraphCommunityResponse> getCommunities(UUID repoId) {
        List<Integer> communityIds = nodeRepo.findDistinctCommunitiesByRepositoryId(repoId);
        return communityIds.stream()
                .map(cid -> {
                    List<GraphNodeEntity> members = nodeRepo.findByRepositoryIdAndCommunity(repoId, cid);
                    return new GraphCommunityResponse(
                            cid,
                            members.size(),
                            members.stream()
                                    .map(n -> new GraphCommunityResponse.MemberNode(
                                            n.getId().toString(),
                                            n.getLabel(),
                                            n.getFileType(),
                                            n.getSourceFile()))
                                    .toList());
                })
                .toList();
    }

    public Optional<GraphCommunityResponse> getCommunityDetail(UUID repoId, int communityId) {
        List<GraphNodeEntity> members = nodeRepo.findByRepositoryIdAndCommunity(repoId, communityId);
        if (members.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new GraphCommunityResponse(
                communityId,
                members.size(),
                members.stream()
                        .map(n -> new GraphCommunityResponse.MemberNode(
                                n.getId().toString(),
                                n.getLabel(),
                                n.getFileType(),
                                n.getSourceFile()))
                        .toList()));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Hyperedge operations
    // ────────────────────────────────────────────────────────────────────────

    public List<GraphHyperedgeResponse> getHyperedges(UUID repoId) {
        return hyperedgeRepo.findByRepositoryId(repoId).stream()
                .map(this::toHyperedgeResponse)
                .toList();
    }

    // ────────────────────────────────────────────────────────────────────────
    // BFS Shortest Path
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Computes the shortest path between two nodes using BFS over edge adjacency.
     *
     * @return ordered list of node IDs from {@code fromId} to {@code toId},
     *         or empty if no path exists.
     */
    public List<String> shortestPath(UUID fromId, UUID toId) {
        if (fromId.equals(toId)) {
            return List.of(fromId.toString());
        }

        Map<UUID, UUID> parentMap = new HashMap<>();
        Queue<UUID> queue = new LinkedList<>();
        Set<UUID> visited = new HashSet<>();

        queue.add(fromId);
        visited.add(fromId);
        parentMap.put(fromId, null);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            List<GraphEdgeEntity> neighborEdges = edgeRepo.findNeighborEdges(current);

            for (GraphEdgeEntity edge : neighborEdges) {
                UUID neighbor = edge.getSourceNodeId().equals(current)
                        ? edge.getTargetNodeId()
                        : edge.getSourceNodeId();

                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);
                parentMap.put(neighbor, current);

                if (neighbor.equals(toId)) {
                    return reconstructPath(parentMap, toId);
                }
                queue.add(neighbor);
            }
        }

        return List.of(); // no path found
    }

    private List<String> reconstructPath(Map<UUID, UUID> parentMap, UUID target) {
        List<String> path = new ArrayList<>();
        UUID current = target;
        while (current != null) {
            path.add(current.toString());
            current = parentMap.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Full graph payload for visualization
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Returns the complete graph data for a repository in a visualization-friendly
     * format containing all nodes and edges.
     */
    public Map<String, Object> getFullGraph(UUID repoId) {
        List<GraphNodeEntity> allNodes = nodeRepo.findByRepositoryId(repoId, Pageable.unpaged()).getContent();
        List<GraphEdgeEntity> allEdges = edgeRepo.findByRepositoryId(repoId, Pageable.unpaged()).getContent();

        List<Map<String, Object>> nodes = allNodes.stream()
                .map(n -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", n.getId().toString());
                    m.put("externalId", n.getExternalId());
                    m.put("label", n.getLabel());
                    m.put("fileType", n.getFileType());
                    m.put("sourceFile", n.getSourceFile());
                    m.put("sourceLocation", n.getSourceLocation());
                    m.put("community", n.getCommunity());
                    return m;
                })
                .toList();

        List<Map<String, Object>> edges = allEdges.stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId().toString());
                    m.put("source", e.getSourceNodeId().toString());
                    m.put("target", e.getTargetNodeId().toString());
                    m.put("relation", e.getRelation());
                    m.put("confidence", e.getConfidence());
                    m.put("confidenceScore", e.getConfidenceScore());
                    m.put("weight", e.getWeight());
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("nodeCount", nodes.size());
        result.put("edgeCount", edges.size());
        return result;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Graph data persistence (used by the data seeder and future analyzers)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public GraphRepositoryEntity createRepository(String name, String url, String branch) {
        GraphRepositoryEntity repo = new GraphRepositoryEntity(name, url, branch);
        return repositoryRepo.save(repo);
    }

    @Transactional
    public GraphNodeEntity saveNode(GraphNodeEntity node) {
        return nodeRepo.save(node);
    }

    @Transactional
    public List<GraphNodeEntity> saveAllNodes(List<GraphNodeEntity> nodes) {
        return nodeRepo.saveAll(nodes);
    }

    @Transactional
    public GraphEdgeEntity saveEdge(GraphEdgeEntity edge) {
        return edgeRepo.save(edge);
    }

    @Transactional
    public List<GraphEdgeEntity> saveAllEdges(List<GraphEdgeEntity> edges) {
        return edgeRepo.saveAll(edges);
    }

    @Transactional
    public void updateRepositoryCounts(UUID repoId) {
        repositoryRepo.findById(repoId).ifPresent(repo -> {
            repo.setNodeCount((int) nodeRepo.countByRepositoryId(repoId));
            repo.setEdgeCount((int) edgeRepo.countByRepositoryId(repoId));

            List<Integer> communities = nodeRepo.findDistinctCommunitiesByRepositoryId(repoId);
            repo.setCommunityCount(communities.size());
            repositoryRepo.save(repo);
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    // Entity → DTO mapping
    // ────────────────────────────────────────────────────────────────────────

    private GraphOverviewResponse toOverview(GraphRepositoryEntity entity) {
        return new GraphOverviewResponse(
                entity.getId().toString(),
                entity.getName(),
                entity.getUrl(),
                entity.getBranch(),
                entity.getCommitHash(),
                entity.getAnalysisStatus(),
                entity.getNodeCount(),
                entity.getEdgeCount(),
                entity.getCommunityCount(),
                entity.getErrorDetail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private GraphNodeResponse toNodeResponseWithNeighbors(GraphNodeEntity node) {
        List<GraphEdgeEntity> neighborEdges = edgeRepo.findNeighborEdges(node.getId());

        List<GraphNodeResponse.NeighborSummary> neighbors = neighborEdges.stream()
                .map(edge -> {
                    boolean isSource = edge.getSourceNodeId().equals(node.getId());
                    UUID neighborId = isSource ? edge.getTargetNodeId() : edge.getSourceNodeId();
                    String direction = isSource ? "outgoing" : "incoming";

                    String neighborLabel = nodeRepo.findById(neighborId)
                            .map(GraphNodeEntity::getLabel)
                            .orElse("unknown");

                    return new GraphNodeResponse.NeighborSummary(
                            neighborId.toString(),
                            neighborLabel,
                            edge.getRelation(),
                            direction);
                })
                .toList();

        return new GraphNodeResponse(
                node.getId().toString(),
                node.getExternalId(),
                node.getLabel(),
                node.getFileType(),
                node.getSourceFile(),
                node.getSourceLocation(),
                node.getCommunity(),
                neighbors);
    }

    private GraphEdgeResponse toEdgeResponse(GraphEdgeEntity edge) {
        String sourceLabel = nodeRepo.findById(edge.getSourceNodeId())
                .map(GraphNodeEntity::getLabel).orElse("unknown");
        String targetLabel = nodeRepo.findById(edge.getTargetNodeId())
                .map(GraphNodeEntity::getLabel).orElse("unknown");

        return new GraphEdgeResponse(
                edge.getId().toString(),
                edge.getSourceNodeId().toString(),
                sourceLabel,
                edge.getTargetNodeId().toString(),
                targetLabel,
                edge.getRelation(),
                edge.getConfidence(),
                edge.getConfidenceScore(),
                edge.getWeight(),
                edge.getSourceFile(),
                edge.getSourceLocation());
    }

    private GraphHyperedgeResponse toHyperedgeResponse(GraphHyperedgeEntity hyperedge) {
        List<GraphHyperedgeMemberEntity> memberEntities =
                hyperedgeRepo.findMembersByHyperedgeId(hyperedge.getId());

        List<GraphHyperedgeResponse.HyperedgeMember> members = memberEntities.stream()
                .map(m -> {
                    String label = nodeRepo.findById(m.getNodeId())
                            .map(GraphNodeEntity::getLabel)
                            .orElse("unknown");
                    return new GraphHyperedgeResponse.HyperedgeMember(
                            m.getNodeId().toString(), label, m.getRole());
                })
                .toList();

        return new GraphHyperedgeResponse(
                hyperedge.getId().toString(),
                hyperedge.getLabel(),
                hyperedge.getDescription(),
                hyperedge.getEdgeType(),
                members);
    }
}
