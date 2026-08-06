package com.lexpilot.ingestion.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code document_chunks} table.
 * <p>
 * The {@code embedding} column (pgvector VECTOR(384)) is not mapped directly
 * since Hibernate has no built-in pgvector type support. Embedding reads/writes
 * go through native queries in {@link com.lexpilot.ingestion.repository.DocumentChunkRepository}.
 */
@Entity
@Table(name = "document_chunks")
public class DocumentChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // embedding is handled via native queries — not mapped here

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected DocumentChunkEntity() {
        // JPA requires a no-arg constructor
    }

    public DocumentChunkEntity(UUID documentId, int chunkIndex, String content) {
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.content = content;
    }

    // ---- Getters ----

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
