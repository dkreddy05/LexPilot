package com.lexpilot.conversation.service;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.conversation.entity.ConversationEntity;
import com.lexpilot.conversation.entity.ConversationMessageEntity;
import com.lexpilot.conversation.repository.ConversationMessageRepository;
import com.lexpilot.conversation.repository.ConversationRepository;
import com.lexpilot.generation.prompt.PromptMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages conversation sessions and chat history for multi-turn interactions.
 * <p>
 * Conversations are persisted in PostgreSQL. Each query within a session
 * appends a USER message and (after generation) an ASSISTANT message.
 * The {@link #getHistory} method returns the most recent turns as
 * {@link PromptMessage} objects ready for injection into the LLM prompt.
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final int maxHistoryTurns;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMessageRepository messageRepository,
                               AppConfig appConfig) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.maxHistoryTurns = appConfig.conversation().maxHistoryTurns();
    }

    /**
     * Resolve or create a conversation for the given session ID.
     * <p>
     * If {@code sessionId} is null or blank, a brand-new conversation is created.
     * If provided, the existing conversation is looked up. Returns the conversation's UUID.
     *
     * @param sessionId the client-supplied session ID, or null/blank for a new session
     * @return the conversation UUID (newly created or existing)
     * @throws IllegalArgumentException if the sessionId is provided but does not exist
     */
    @Transactional
    public UUID getOrCreateConversation(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            ConversationEntity conversation = ConversationEntity.create();
            conversation = conversationRepository.save(conversation);
            log.debug("Created new conversation: {}", conversation.getId());
            return conversation.getId();
        }

        UUID conversationId;
        try {
            conversationId = UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid sessionId format: " + sessionId);
        }

        if (!conversationRepository.existsById(conversationId)) {
            throw new IllegalArgumentException("Conversation not found: " + sessionId);
        }

        log.debug("Resuming conversation: {}", conversationId);
        return conversationId;
    }

    /**
     * Retrieve the conversation history as prompt messages, limited to the
     * most recent N turns (where a "turn" is one USER + one ASSISTANT message pair).
     *
     * @param conversationId the conversation UUID
     * @return ordered list of USER/ASSISTANT prompt messages (may be empty for new conversations)
     */
    @Transactional(readOnly = true)
    public List<PromptMessage> getHistory(UUID conversationId) {
        List<ConversationMessageEntity> allMessages =
                messageRepository.findByConversationIdOrderByIdAsc(conversationId);

        if (allMessages.isEmpty()) {
            return List.of();
        }

        // Each "turn" is 2 messages (USER + ASSISTANT), so limit to maxHistoryTurns * 2
        int maxMessages = maxHistoryTurns * 2;
        List<ConversationMessageEntity> recentMessages;
        if (allMessages.size() > maxMessages) {
            recentMessages = allMessages.subList(allMessages.size() - maxMessages, allMessages.size());
        } else {
            recentMessages = allMessages;
        }

        return recentMessages.stream()
                .map(msg -> new PromptMessage(
                        mapRole(msg.getRole()),
                        msg.getContent()))
                .toList();
    }

    /**
     * Persist a user message in the conversation.
     *
     * @param conversationId the conversation UUID
     * @param content        the user's query text
     */
    @Transactional
    public void appendUserMessage(UUID conversationId, String content) {
        ConversationEntity conversation = conversationRepository.getReferenceById(conversationId);
        ConversationMessageEntity message = new ConversationMessageEntity(conversation, "USER", content);
        messageRepository.save(message);
        conversation.touch();
        conversationRepository.save(conversation);
        log.debug("Appended USER message to conversation {}", conversationId);
    }

    /**
     * Persist an assistant (LLM) response in the conversation.
     *
     * @param conversationId the conversation UUID
     * @param content        the LLM-generated answer text
     */
    @Transactional
    public void appendAssistantMessage(UUID conversationId, String content) {
        ConversationEntity conversation = conversationRepository.getReferenceById(conversationId);
        ConversationMessageEntity message = new ConversationMessageEntity(conversation, "ASSISTANT", content);
        messageRepository.save(message);
        conversation.touch();
        conversationRepository.save(conversation);
        log.debug("Appended ASSISTANT message to conversation {}", conversationId);
    }

    private PromptMessage.Role mapRole(String role) {
        return switch (role) {
            case "USER" -> PromptMessage.Role.USER;
            case "ASSISTANT" -> PromptMessage.Role.ASSISTANT;
            default -> throw new IllegalStateException("Unknown message role: " + role);
        };
    }
}
