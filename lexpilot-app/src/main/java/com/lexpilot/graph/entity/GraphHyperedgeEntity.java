package com.lexpilot.graph.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code graph_hyperedges} table.
 * Represents a multi-node architectural grouping such as a subsystem,
 * transaction boundary, or design pattern.
 */
@Entity
@Table(name = "graph_hyperedges")
public class GraphHyperedgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(nullable = false, length = 512)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "edge_type", length = 64)
    private String edgeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB DEFAULT '{}'")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected GraphHyperedgeEntity() {
        // JPA requires a no-arg constructor
    }

    public GraphHyperedgeEntity(UUID repositoryId, String label) {
        this.repositoryId = repositoryId;
        this.label = label;
    }

    // ---- Getters & setters ----

    public UUID getId() { return id; }

    public UUID getRepositoryId() { return repositoryId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEdgeType() { return edgeType; }
    public void setEdgeType(String edgeType) { this.edgeType = edgeType; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
