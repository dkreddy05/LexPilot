package com.lexpilot.common.dto;

import java.util.List;

/**
 * Response payload for the generation endpoint ({@code POST /api/v1/query/answer}).
 * Contains the LLM-generated answer grounded in retrieved context, with citations.
 *
 * @param sessionId the conversation session ID — use this in subsequent requests
 *                  to maintain multi-turn conversation context
 */
public record QueryResponse(
        String answer,
        List<CitationDto> citations,
        boolean lowConfidence,
        String sessionId
) {

    /**
     * A citation linking a marker in the answer text to a source document chunk.
     *
     * @param marker      the [n] number used in the answer text (1-based)
     * @param chunkId     UUID of the cited chunk
     * @param documentId  UUID of the parent document
     * @param sourceLabel human-readable source name (e.g. filename)
     */
    public record CitationDto(
            int marker,
            String chunkId,
            String documentId,
            String sourceLabel
    ) {}
}
