package com.lexpilot.graph.dto;

/**
 * Request parameters for searching graph nodes.
 */
public record GraphSearchRequest(
        String query,
        Integer community,
        String relation,
        int maxResults
) {
    public GraphSearchRequest {
        if (maxResults <= 0) maxResults = 20;
        if (maxResults > 100) maxResults = 100;
    }
}
