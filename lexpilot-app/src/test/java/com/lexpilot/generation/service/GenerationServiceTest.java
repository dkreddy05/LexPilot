package com.lexpilot.generation.service;

import com.lexpilot.generation.citation.CitationFormatter;
import com.lexpilot.generation.dto.Citation;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.generation.guardrail.LowConfidenceGuardrail;
import com.lexpilot.generation.llm.LlmApiClient;
import com.lexpilot.generation.llm.LlmResponse;
import com.lexpilot.generation.prompt.PromptBuilder;
import com.lexpilot.generation.prompt.PromptMessage;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link GenerationService} with all dependencies mocked.
 * Verifies the orchestration flow without hitting a real LLM.
 */
@ExtendWith(MockitoExtension.class)
class GenerationServiceTest {

    @Mock private PromptBuilder promptBuilder;
    @Mock private LlmApiClient llmApiClient;
    @Mock private CitationFormatter citationFormatter;
    @Mock private LowConfidenceGuardrail guardrail;

    private GenerationService generationService;

    private static final UUID DOC_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHUNK_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHUNK_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        generationService = new GenerationService(promptBuilder, llmApiClient,
                citationFormatter, guardrail);
    }

    @Test
    void generate_shouldOrchestratePipelineCorrectly() {
        // Arrange
        String query = "How do I get a refund?";
        List<ScoredChunk> chunks = List.of(
                new ScoredChunk(CHUNK_1_ID, DOC_ID, "Refund policy...", 0.95, "guide.pdf"),
                new ScoredChunk(CHUNK_2_ID, DOC_ID, "Warranty info...", 0.80, "warranty.pdf")
        );

        List<PromptMessage> messages = List.of(
                new PromptMessage(PromptMessage.Role.SYSTEM, "system prompt"),
                new PromptMessage(PromptMessage.Role.USER, "user message")
        );

        String rawLlmOutput = "You can get a refund per [1]. Warranty applies per [2].";
        List<Citation> expectedCitations = List.of(
                new Citation(1, CHUNK_1_ID, DOC_ID, "guide.pdf"),
                new Citation(2, CHUNK_2_ID, DOC_ID, "warranty.pdf")
        );
        GeneratedAnswer formattedAnswer = new GeneratedAnswer(rawLlmOutput, expectedCitations, false);

        when(promptBuilder.build(query, chunks)).thenReturn(messages);
        when(llmApiClient.complete(messages)).thenReturn(new LlmResponse(rawLlmOutput));
        when(citationFormatter.format(rawLlmOutput, chunks)).thenReturn(formattedAnswer);
        when(guardrail.isLowConfidence(query, chunks, formattedAnswer)).thenReturn(false);

        // Act
        GeneratedAnswer result = generationService.generate(query, chunks);

        // Assert
        assertThat(result.answer()).isEqualTo(rawLlmOutput);
        assertThat(result.citations()).hasSize(2);
        assertThat(result.citations().get(0).marker()).isEqualTo(1);
        assertThat(result.citations().get(0).chunkId()).isEqualTo(CHUNK_1_ID);
        assertThat(result.citations().get(0).sourceLabel()).isEqualTo("guide.pdf");
        assertThat(result.citations().get(1).marker()).isEqualTo(2);
        assertThat(result.lowConfidence()).isFalse();

        // Verify pipeline ordering
        verify(promptBuilder).build(query, chunks);
        verify(llmApiClient).complete(messages);
        verify(citationFormatter).format(rawLlmOutput, chunks);
        verify(guardrail).isLowConfidence(query, chunks, formattedAnswer);
    }

    @Test
    void generate_whenGuardrailFlagsLowConfidence_shouldPropagate() {
        // Arrange
        String query = "What is quantum physics?";
        List<ScoredChunk> chunks = List.of(
                new ScoredChunk(CHUNK_1_ID, DOC_ID, "Unrelated content", 0.20, "random.pdf")
        );

        List<PromptMessage> messages = List.of(
                new PromptMessage(PromptMessage.Role.SYSTEM, "system prompt"),
                new PromptMessage(PromptMessage.Role.USER, "user message")
        );

        String rawAnswer = "I don't have enough information.";
        GeneratedAnswer formatted = new GeneratedAnswer(rawAnswer, List.of(), false);

        when(promptBuilder.build(anyString(), anyList())).thenReturn(messages);
        when(llmApiClient.complete(anyList())).thenReturn(new LlmResponse(rawAnswer));
        when(citationFormatter.format(anyString(), anyList())).thenReturn(formatted);
        when(guardrail.isLowConfidence(anyString(), anyList(), any())).thenReturn(true);

        // Act
        GeneratedAnswer result = generationService.generate(query, chunks);

        // Assert
        assertThat(result.lowConfidence()).isTrue();
        assertThat(result.citations()).isEmpty();
    }
}
