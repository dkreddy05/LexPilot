package com.lexpilot.graph.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code graph_repositories} table.
 * Represents a code repository that has been (or is being) analyzed
 * by the Graphify analysis engine.
 */
@Entity
@Table(name = "graph_repositories")
public class GraphRepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2048)
    private String url;

    @Column(length = 255)
    private String branch = "main";

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(name = "analysis_status", nullable = false, length = 32)
    private String analysisStatus = AnalysisStatus.PENDING;

    @Column(name = "node_count")
    private int nodeCount;

    @Column(name = "edge_count")
    private int edgeCount;

    @Column(name = "community_count")
    private int communityCount;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    protected GraphRepositoryEntity() {
        // JPA requires a no-arg constructor
    }

    public GraphRepositoryEntity(String name) {
        this.name = name;
    }

    public GraphRepositoryEntity(String name, String url, String branch) {
        this.name = name;
        this.url = url;
        this.branch = branch;
    }

    // ---- Getters & setters ----

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
        this.updatedAt = OffsetDateTime.now();
    }

    public int getNodeCount() { return nodeCount; }
    public void setNodeCount(int nodeCount) { this.nodeCount = nodeCount; }

    public int getEdgeCount() { return edgeCount; }
    public void setEdgeCount(int edgeCount) { this.edgeCount = edgeCount; }

    public int getCommunityCount() { return communityCount; }
    public void setCommunityCount(int communityCount) { this.communityCount = communityCount; }

    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
