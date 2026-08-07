package com.lexpilot.common.dto;

import java.util.List;

/**
 * Response payload for the retrieval-only search endpoint.
 * Returns raw scored chunks without LLM generation.
 * <p>
 * This will be replaced or supplemented by {@link QueryResponse} once
 * the generation slice is implemented.
 */
public record SearchResultsResponse(List<Result> results) {

    public record Result(
            String chunkId,
            String documentId,
            String content,
            double score
    ) {}
}
