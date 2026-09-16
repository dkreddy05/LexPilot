package com.lexpilot.common.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log entity — one row per tracked user action.
 * Designed for append-only writes (no updates/deletes in normal operation).
 */
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String action;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "query_text")
    private String queryText;

    @Column(name = "retrieved_chunk_ids", columnDefinition = "jsonb")
    private String retrievedChunkIds;

    @Column(name = "generated_answer")
    private String generatedAnswer;

    @Column(name = "low_confidence")
    private boolean lowConfidence;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditLogEntity() {}

    public AuditLogEntity(UUID tenantId, String action) {
        this.tenantId = tenantId;
        this.action = action;
        this.createdAt = Instant.now();
    }

    // ---- Fluent setters for builder-style construction ----

    public AuditLogEntity userId(String userId) {
        this.userId = userId;
        return this;
    }

    public AuditLogEntity queryText(String queryText) {
        this.queryText = queryText;
        return this;
    }

    public AuditLogEntity retrievedChunkIds(String retrievedChunkIds) {
        this.retrievedChunkIds = retrievedChunkIds;
        return this;
    }

    public AuditLogEntity generatedAnswer(String answer) {
        // Truncate to 500 chars for storage efficiency
        this.generatedAnswer = answer != null && answer.length() > 500
                ? answer.substring(0, 500) + "..."
                : answer;
        return this;
    }

    public AuditLogEntity lowConfidence(boolean lowConfidence) {
        this.lowConfidence = lowConfidence;
        return this;
    }

    public AuditLogEntity ipAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    // ---- Getters ----
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getAction() { return action; }
    public String getUserId() { return userId; }
    public String getQueryText() { return queryText; }
    public String getRetrievedChunkIds() { return retrievedChunkIds; }
    public String getGeneratedAnswer() { return generatedAnswer; }
    public boolean isLowConfidence() { return lowConfidence; }
    public String getIpAddress() { return ipAddress; }
    public Instant getCreatedAt() { return createdAt; }
}
