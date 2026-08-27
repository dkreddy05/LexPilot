package com.lexpilot.conversation.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * JPA entity mapping the {@code conversation_messages} table.
 * Each row is a single turn (USER or ASSISTANT) within a conversation.
 */
@Entity
@Table(name = "conversation_messages")
public class ConversationMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationEntity conversation;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected ConversationMessageEntity() {
        // JPA requires a no-arg constructor
    }

    public ConversationMessageEntity(ConversationEntity conversation, String role, String content) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
    }

    // ---- Getters ----

    public Long getId() {
        return id;
    }

    public ConversationEntity getConversation() {
        return conversation;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
