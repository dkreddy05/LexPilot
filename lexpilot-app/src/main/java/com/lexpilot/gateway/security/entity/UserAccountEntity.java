package com.lexpilot.gateway.security.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import java.time.ZonedDateTime;

@Entity
@Table(name = "user_accounts")
public class UserAccountEntity {

    @Id
    private UUID id;
    private String email;
    private String name;
    private String passwordHash;
    private String role;
    private UUID tenantId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public UserAccountEntity() {}

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public UUID getTenantId() { return tenantId; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public ZonedDateTime getUpdatedAt() { return updatedAt; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
