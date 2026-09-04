package com.lexpilot.graph.dto;

import java.time.OffsetDateTime;

/**
 * Summary response for a graph repository.
 */
public record GraphOverviewResponse(
        String repositoryId,
        String name,
        String url,
        String branch,
        String commitHash,
        String analysisStatus,
        int nodeCount,
        int edgeCount,
        int communityCount,
        String errorDetail,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
