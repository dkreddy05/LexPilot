package com.lexpilot.conversation.repository;

import com.lexpilot.conversation.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ConversationEntity}.
 */
public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
}
