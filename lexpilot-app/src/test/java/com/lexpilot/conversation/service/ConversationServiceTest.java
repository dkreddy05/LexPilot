package com.lexpilot.conversation.service;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.exception.ConversationNotFoundException;
import com.lexpilot.conversation.entity.ConversationEntity;
import com.lexpilot.conversation.entity.ConversationMessageEntity;
import com.lexpilot.conversation.repository.ConversationMessageRepository;
import com.lexpilot.conversation.repository.ConversationRepository;
import com.lexpilot.generation.prompt.PromptMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ConversationService}.
 * All repository interactions are mocked — no database required.
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationMessageRepository messageRepository;

    private ConversationService conversationService;

    private static final UUID CONV_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void setUp() {
        // maxHistoryTurns = 3 for testing the windowing logic
        AppConfig appConfig = new AppConfig(
                "test-key",
                new AppConfig.EmbeddingServiceConfig("http://localhost:8000"),
                new AppConfig.LlmConfig("key", "http://llm", "test-model", 1024, 0.2, 30),
                new AppConfig.IngestionConfig("topic", 500, 75, "./uploads", 20),
                new AppConfig.RetrievalConfig(20, 20, 10),
                new AppConfig.RateLimitingConfig(60, 1000),
                new AppConfig.ConversationConfig(3)
        );
        conversationService = new ConversationService(conversationRepository, messageRepository, appConfig);
    }

    /**
     * Helper to create a ConversationEntity with a pre-set ID (simulating JPA save).
     */
    private ConversationEntity entityWithId(UUID id) {
        ConversationEntity entity = ConversationEntity.create();
        try {
            Field idField = ConversationEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    @Test
    void getOrCreateConversation_whenNullSessionId_shouldCreateNew() {
        // Arrange
        UUID newId = UUID.randomUUID();
        when(conversationRepository.save(any(ConversationEntity.class))).thenReturn(entityWithId(newId));

        // Act
        UUID result = conversationService.getOrCreateConversation(null);

        // Assert
        assertThat(result).isEqualTo(newId);
        verify(conversationRepository).save(any(ConversationEntity.class));
    }

    @Test
    void getOrCreateConversation_whenBlankSessionId_shouldCreateNew() {
        // Arrange
        UUID newId = UUID.randomUUID();
        when(conversationRepository.save(any(ConversationEntity.class))).thenReturn(entityWithId(newId));

        // Act
        UUID result = conversationService.getOrCreateConversation("   ");

        // Assert
        assertThat(result).isEqualTo(newId);
        verify(conversationRepository).save(any(ConversationEntity.class));
    }

    @Test
    void getOrCreateConversation_whenValidSessionId_shouldReturnExisting() {
        // Arrange
        when(conversationRepository.existsById(CONV_ID)).thenReturn(true);

        // Act
        UUID result = conversationService.getOrCreateConversation(CONV_ID.toString());

        // Assert
        assertThat(result).isEqualTo(CONV_ID);
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getOrCreateConversation_whenInvalidUuid_shouldThrow() {
        assertThatThrownBy(() -> conversationService.getOrCreateConversation("not-a-uuid"))
                .isInstanceOf(ConversationNotFoundException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    void getOrCreateConversation_whenNonExistentSessionId_shouldThrow() {
        // Arrange
        when(conversationRepository.existsById(CONV_ID)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> conversationService.getOrCreateConversation(CONV_ID.toString()))
                .isInstanceOf(ConversationNotFoundException.class)
                .hasMessageContaining("Conversation not found");
    }

    @Test
    void getHistory_whenEmpty_shouldReturnEmptyList() {
        // Arrange
        when(messageRepository.findByConversationIdOrderByIdAsc(CONV_ID)).thenReturn(List.of());

        // Act
        List<PromptMessage> history = conversationService.getHistory(CONV_ID);

        // Assert
        assertThat(history).isEmpty();
    }

    @Test
    void getHistory_shouldReturnMessagesAsMappedPromptMessages() {
        // Arrange
        ConversationEntity conv = ConversationEntity.create();
        List<ConversationMessageEntity> messages = List.of(
                new ConversationMessageEntity(conv, "USER", "Question 1"),
                new ConversationMessageEntity(conv, "ASSISTANT", "Answer 1"),
                new ConversationMessageEntity(conv, "USER", "Question 2"),
                new ConversationMessageEntity(conv, "ASSISTANT", "Answer 2")
        );
        when(messageRepository.findByConversationIdOrderByIdAsc(CONV_ID)).thenReturn(messages);

        // Act
        List<PromptMessage> history = conversationService.getHistory(CONV_ID);

        // Assert
        assertThat(history).hasSize(4);
        assertThat(history.get(0).role()).isEqualTo(PromptMessage.Role.USER);
        assertThat(history.get(0).content()).isEqualTo("Question 1");
        assertThat(history.get(1).role()).isEqualTo(PromptMessage.Role.ASSISTANT);
        assertThat(history.get(1).content()).isEqualTo("Answer 1");
    }

    @Test
    void getHistory_shouldRespectMaxHistoryTurns() {
        // maxHistoryTurns = 3, so max 6 messages; we provide 10 messages (5 turns)
        ConversationEntity conv = ConversationEntity.create();
        List<ConversationMessageEntity> messages = List.of(
                new ConversationMessageEntity(conv, "USER", "Q1"),
                new ConversationMessageEntity(conv, "ASSISTANT", "A1"),
                new ConversationMessageEntity(conv, "USER", "Q2"),
                new ConversationMessageEntity(conv, "ASSISTANT", "A2"),
                new ConversationMessageEntity(conv, "USER", "Q3"),
                new ConversationMessageEntity(conv, "ASSISTANT", "A3"),
                new ConversationMessageEntity(conv, "USER", "Q4"),
                new ConversationMessageEntity(conv, "ASSISTANT", "A4"),
                new ConversationMessageEntity(conv, "USER", "Q5"),
                new ConversationMessageEntity(conv, "ASSISTANT", "A5")
        );
        when(messageRepository.findByConversationIdOrderByIdAsc(CONV_ID)).thenReturn(messages);

        // Act
        List<PromptMessage> history = conversationService.getHistory(CONV_ID);

        // Assert — should only contain the last 3 turns (6 messages)
        assertThat(history).hasSize(6);
        assertThat(history.get(0).content()).isEqualTo("Q3");
        assertThat(history.get(5).content()).isEqualTo("A5");
    }

    @Test
    void appendUserMessage_shouldPersistWithCorrectRole() {
        // Arrange
        ConversationEntity conv = ConversationEntity.create();
        when(conversationRepository.getReferenceById(CONV_ID)).thenReturn(conv);
        when(conversationRepository.save(conv)).thenReturn(conv);

        // Act
        conversationService.appendUserMessage(CONV_ID, "My question");

        // Assert
        ArgumentCaptor<ConversationMessageEntity> captor =
                ArgumentCaptor.forClass(ConversationMessageEntity.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
        assertThat(captor.getValue().getContent()).isEqualTo("My question");
    }

    @Test
    void appendAssistantMessage_shouldPersistWithCorrectRole() {
        // Arrange
        ConversationEntity conv = ConversationEntity.create();
        when(conversationRepository.getReferenceById(CONV_ID)).thenReturn(conv);
        when(conversationRepository.save(conv)).thenReturn(conv);

        // Act
        conversationService.appendAssistantMessage(CONV_ID, "My answer");

        // Assert
        ArgumentCaptor<ConversationMessageEntity> captor =
                ArgumentCaptor.forClass(ConversationMessageEntity.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("ASSISTANT");
        assertThat(captor.getValue().getContent()).isEqualTo("My answer");
    }
}
