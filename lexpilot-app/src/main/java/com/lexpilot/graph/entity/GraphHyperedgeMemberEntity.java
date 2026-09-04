package com.lexpilot.graph.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity mapping the {@code graph_hyperedge_members} join table.
 * Associates a graph node with a hyperedge, optionally specifying
 * the node's role within the group (e.g. "member", "entry", "exit").
 */
@Entity
@Table(name = "graph_hyperedge_members")
@IdClass(GraphHyperedgeMemberEntity.HyperedgeMemberId.class)
public class GraphHyperedgeMemberEntity {

    @Id
    @Column(name = "hyperedge_id", nullable = false)
    private UUID hyperedgeId;

    @Id
    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    @Column(length = 64)
    private String role;

    protected GraphHyperedgeMemberEntity() {
        // JPA requires a no-arg constructor
    }

    public GraphHyperedgeMemberEntity(UUID hyperedgeId, UUID nodeId, String role) {
        this.hyperedgeId = hyperedgeId;
        this.nodeId = nodeId;
        this.role = role;
    }

    // ---- Getters ----

    public UUID getHyperedgeId() { return hyperedgeId; }
    public UUID getNodeId() { return nodeId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    /**
     * Composite primary key class for the hyperedge-member join table.
     */
    public static class HyperedgeMemberId implements Serializable {

        private UUID hyperedgeId;
        private UUID nodeId;

        public HyperedgeMemberId() {}

        public HyperedgeMemberId(UUID hyperedgeId, UUID nodeId) {
            this.hyperedgeId = hyperedgeId;
            this.nodeId = nodeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HyperedgeMemberId that)) return false;
            return Objects.equals(hyperedgeId, that.hyperedgeId)
                && Objects.equals(nodeId, that.nodeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hyperedgeId, nodeId);
        }
    }
}
