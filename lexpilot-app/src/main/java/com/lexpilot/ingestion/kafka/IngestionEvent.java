package com.lexpilot.ingestion.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kafka event published to {@code document-ingestion-events}.
 * <p>
 * One event per chunk, with {@code expectedChunkCount} so the consumer
 * knows when all chunks for a document have been processed.
 */
public record IngestionEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("documentId") String documentId,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("payload") ChunkPayload payload,
        @JsonProperty("schemaVersion") int schemaVersion
) {
    /** Default schema version for this event shape. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public record ChunkPayload(
            @JsonProperty("chunkId") String chunkId,
            @JsonProperty("chunkIndex") int chunkIndex,
            @JsonProperty("content") String content,
            @JsonProperty("expectedChunkCount") int expectedChunkCount
    ) {}
}
