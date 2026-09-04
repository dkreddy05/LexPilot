package com.lexpilot.graph.dto;

/**
 * Response for a single graph edge.
 */
public record GraphEdgeResponse(
        String edgeId,
        String sourceNodeId,
        String sourceLabel,
        String targetNodeId,
        String targetLabel,
        String relation,
        String confidence,
        double confidenceScore,
        double weight,
        String sourceFile,
        String sourceLocation
) {}
