package com.lexpilot.retrieval.dto;

import java.util.UUID;

/**
 * A document chunk scored by cosine similarity against a query embedding.
 * Returned by {@link com.lexpilot.retrieval.repository.VectorSearchRepository}
 * and surfaced through the search API.
 *
 * @param chunkId    primary key of the document_chunks row
 * @param documentId foreign key to the parent document
 * @param content    the chunk's text content
 * @param score      cosine similarity in [0, 1] (1 = identical)
 */
public record ScoredChunk(
        UUID chunkId,
        UUID documentId,
        String content,
        double score
) {}
