package com.lexpilot.graph.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code graph_edges} table.
 * Represents a directed or undirected relationship between two graph nodes.
 */
@Entity
@Table(name = "graph_edges")
public class GraphEdgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "source_node_id", nullable = false)
    private UUID sourceNodeId;

    @Column(name = "target_node_id", nullable = false)
    private UUID targetNodeId;

    @Column(nullable = false, length = 64)
    private String relation;

    @Column(length = 32)
    private String confidence;

    @Column(name = "confidence_score")
    private double confidenceScore = 1.0;

    @Column
    private double weight = 1.0;

    @Column(name = "source_file", length = 1024)
    private String sourceFile;

    @Column(name = "source_location", length = 64)
    private String sourceLocation;

    @Column(columnDefinition = "JSONB DEFAULT '{}'")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected GraphEdgeEntity() {
        // JPA requires a no-arg constructor
    }

    public GraphEdgeEntity(UUID repositoryId, UUID sourceNodeId, UUID targetNodeId, String relation) {
        this.repositoryId = repositoryId;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.relation = relation;
    }

    // ---- Getters & setters ----

    public UUID getId() { return id; }

    public UUID getRepositoryId() { return repositoryId; }

    public UUID getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(UUID sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public UUID getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(UUID targetNodeId) { this.targetNodeId = targetNodeId; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    public String getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
