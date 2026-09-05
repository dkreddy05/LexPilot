package com.lexpilot.graph.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code graph_nodes} table.
 * Represents a single node in the code knowledge graph — a file, class,
 * function, module, or rationale entry.
 */
@Entity
@Table(name = "graph_nodes")
public class GraphNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "repository_id", nullable = false)
    private UUID repositoryId;

    @Column(name = "external_id", nullable = false, length = 512)
    private String externalId;

    @Column(nullable = false, length = 512)
    private String label;

    @Column(name = "file_type", length = 64)
    private String fileType;

    @Column(name = "source_file", length = 1024)
    private String sourceFile;

    @Column(name = "source_location", length = 64)
    private String sourceLocation;

    @Column
    private int community = -1;

    @Column(name = "norm_label", length = 512)
    private String normLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB DEFAULT '{}'")
    private String metadata = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected GraphNodeEntity() {
        // JPA requires a no-arg constructor
    }

    public GraphNodeEntity(UUID repositoryId, String externalId, String label) {
        this.repositoryId = repositoryId;
        this.externalId = externalId;
        this.label = label;
        this.normLabel = label.toLowerCase();
    }

    // ---- Getters & setters ----

    public UUID getId() { return id; }

    public UUID getRepositoryId() { return repositoryId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getLabel() { return label; }
    public void setLabel(String label) {
        this.label = label;
        this.normLabel = label.toLowerCase();
    }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    public String getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }

    public int getCommunity() { return community; }
    public void setCommunity(int community) { this.community = community; }

    public String getNormLabel() { return normLabel; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
