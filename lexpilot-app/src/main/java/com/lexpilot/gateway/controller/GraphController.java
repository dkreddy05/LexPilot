package com.lexpilot.gateway.controller;

import com.lexpilot.graph.dto.*;
import com.lexpilot.graph.service.GraphService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller exposing the Graphify code knowledge graph API.
 * All endpoints are prefixed with {@code /api/v1/graph}.
 */
@RestController
@RequestMapping("/api/v1/graph")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Repository endpoints
    // ────────────────────────────────────────────────────────────────────────

    /**
     * List all analyzed repositories.
     */
    @GetMapping("/repositories")
    public ResponseEntity<List<GraphOverviewResponse>> listRepositories() {
        return ResponseEntity.ok(graphService.listRepositories());
    }

    /**
     * Get repository overview by ID.
     */
    @GetMapping("/repositories/{repoId}")
    public ResponseEntity<GraphOverviewResponse> getRepository(@PathVariable String repoId) {
        return graphService.getRepository(UUID.fromString(repoId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Node endpoints
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Paginated list of nodes in a repository.
     * Optional query param {@code search} triggers trigram fuzzy search.
     */
    @GetMapping("/repositories/{repoId}/nodes")
    public ResponseEntity<?> getNodes(
            @PathVariable String repoId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        UUID repoUuid = UUID.fromString(repoId);

        if (search != null && !search.isBlank()) {
            List<GraphNodeResponse> results = graphService.searchNodes(repoUuid, search, size);
            return ResponseEntity.ok(results);
        }

        Page<GraphNodeResponse> nodes = graphService.getNodes(repoUuid, PageRequest.of(page, size));
        return ResponseEntity.ok(nodes);
    }

    /**
     * Get detailed information about a single node, including its neighbors.
     */
    @GetMapping("/repositories/{repoId}/nodes/{nodeId}")
    public ResponseEntity<GraphNodeResponse> getNodeDetail(
            @PathVariable String repoId,
            @PathVariable String nodeId) {

        return graphService.getNodeDetail(UUID.fromString(nodeId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Edge endpoints
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Paginated list of edges in a repository.
     * Optional query param {@code relation} filters by edge relation type.
     */
    @GetMapping("/repositories/{repoId}/edges")
    public ResponseEntity<Page<GraphEdgeResponse>> getEdges(
            @PathVariable String repoId,
            @RequestParam(required = false) String relation,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<GraphEdgeResponse> edges = graphService.getEdges(
                UUID.fromString(repoId), relation, PageRequest.of(page, size));
        return ResponseEntity.ok(edges);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Community endpoints
    // ────────────────────────────────────────────────────────────────────────

    /**
     * List all communities in a repository with member counts.
     */
    @GetMapping("/repositories/{repoId}/communities")
    public ResponseEntity<List<GraphCommunityResponse>> getCommunities(
            @PathVariable String repoId) {

        return ResponseEntity.ok(graphService.getCommunities(UUID.fromString(repoId)));
    }

    /**
     * Get detailed community info with all member nodes.
     */
    @GetMapping("/repositories/{repoId}/communities/{communityId}")
    public ResponseEntity<GraphCommunityResponse> getCommunityDetail(
            @PathVariable String repoId,
            @PathVariable int communityId) {

        return graphService.getCommunityDetail(UUID.fromString(repoId), communityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Hyperedge endpoints
    // ────────────────────────────────────────────────────────────────────────

    /**
     * List all hyperedges in a repository.
     */
    @GetMapping("/repositories/{repoId}/hyperedges")
    public ResponseEntity<List<GraphHyperedgeResponse>> getHyperedges(
            @PathVariable String repoId) {

        return ResponseEntity.ok(graphService.getHyperedges(UUID.fromString(repoId)));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Path query
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Compute the shortest path between two nodes using BFS.
     */
    @GetMapping("/repositories/{repoId}/path")
    public ResponseEntity<Map<String, Object>> shortestPath(
            @PathVariable String repoId,
            @RequestParam String from,
            @RequestParam String to) {

        List<String> path = graphService.shortestPath(
                UUID.fromString(from), UUID.fromString(to));

        return ResponseEntity.ok(Map.of(
                "from", from,
                "to", to,
                "path", path,
                "length", path.size()));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Full graph payload (for visualization)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Returns the complete graph data for client-side visualization.
     */
    @GetMapping("/repositories/{repoId}/full")
    public ResponseEntity<Map<String, Object>> getFullGraph(@PathVariable String repoId) {
        return ResponseEntity.ok(graphService.getFullGraph(UUID.fromString(repoId)));
    }
}
