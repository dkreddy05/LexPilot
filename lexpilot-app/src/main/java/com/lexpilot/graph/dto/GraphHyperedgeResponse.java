package com.lexpilot.graph.dto;

import java.util.List;

/**
 * Response for a hyperedge with its member nodes.
 */
public record GraphHyperedgeResponse(
        String hyperedgeId,
        String label,
        String description,
        String edgeType,
        List<HyperedgeMember> members
) {

    /**
     * A member node within a hyperedge, with its assigned role.
     */
    public record HyperedgeMember(
            String nodeId,
            String label,
            String role
    ) {}
}
