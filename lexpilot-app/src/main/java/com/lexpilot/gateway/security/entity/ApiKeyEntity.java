package com.lexpilot.gateway.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
public class ApiKeyEntity {

    @Id
    private UUID id;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "label")
    private String label;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ApiKeyEntity() {}

    public ApiKeyEntity(UUID id, String keyHash, UUID tenantId, String label) {
        this.id = id;
        this.keyHash = keyHash;
        this.tenantId = tenantId;
        this.label = label;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isRevoked() {
        return revokedAt != null && Instant.now().isAfter(revokedAt);
    }
}
