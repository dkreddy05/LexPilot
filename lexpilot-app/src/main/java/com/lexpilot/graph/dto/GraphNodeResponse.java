package com.lexpilot.graph.dto;

import java.util.List;

/**
 * Detailed response for a single graph node, including its 1-hop neighbors.
 */
public record GraphNodeResponse(
        String nodeId,
        String externalId,
        String label,
        String fileType,
        String sourceFile,
        String sourceLocation,
        int community,
        List<NeighborSummary> neighbors
) {

    /**
     * Summary of a neighboring node reached via an edge.
     */
    public record NeighborSummary(
            String nodeId,
            String label,
            String relation,
            String direction
    ) {}
}
