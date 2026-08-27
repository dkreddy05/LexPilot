package com.lexpilot.conversation.repository;

import com.lexpilot.conversation.entity.ConversationMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ConversationMessageEntity}.
 * Provides ordered retrieval of messages within a conversation.
 */
public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, Long> {

    /**
     * Retrieve all messages for a conversation, ordered by insertion order (ascending).
     *
     * @param conversationId the conversation's UUID
     * @return messages in chronological order
     */
    List<ConversationMessageEntity> findByConversationIdOrderByIdAsc(UUID conversationId);
}
