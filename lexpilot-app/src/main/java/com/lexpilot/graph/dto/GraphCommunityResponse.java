package com.lexpilot.graph.dto;

import java.util.List;

/**
 * Response for a community cluster containing member node summaries.
 */
public record GraphCommunityResponse(
        int communityId,
        int memberCount,
        List<MemberNode> members
) {

    /**
     * Lightweight summary of a node within a community.
     */
    public record MemberNode(
            String nodeId,
            String label,
            String fileType,
            String sourceFile
    ) {}
}
