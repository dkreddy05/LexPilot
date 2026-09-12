package com.lexpilot.generation.guardrail;

import com.lexpilot.generation.dto.Citation;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreBasedLowConfidenceGuardrailTest {

    private ScoreBasedLowConfidenceGuardrail guardrail;

    @BeforeEach
    void setUp() {
        guardrail = new ScoreBasedLowConfidenceGuardrail();
    }

    // -- Helpers --

    private ScoredChunk chunk(double score) {
        return new ScoredChunk(UUID.randomUUID(), UUID.randomUUID(), "Some legal text", score, "doc.pdf");
    }

    private Citation citation(int marker) {
        return new Citation(marker, UUID.randomUUID(), UUID.randomUUID(), "doc.pdf");
    }

    private GeneratedAnswer answerWithCitations(String text, List<Citation> citations) {
        return new GeneratedAnswer(text, citations, false);
    }

    // -- Tests --

    @Test
    void emptyChunks_isLowConfidence() {
        GeneratedAnswer answer = answerWithCitations("Some answer", List.of());
        assertThat(guardrail.isLowConfidence("query", List.of(), answer)).isTrue();
    }

    @Test
    void nullChunks_isLowConfidence() {
        GeneratedAnswer answer = answerWithCitations("Some answer", List.of());
        assertThat(guardrail.isLowConfidence("query", null, answer)).isTrue();
    }

    @Test
    void allChunksBelowThreshold_isLowConfidence() {
        List<ScoredChunk> chunks = List.of(chunk(0.1), chunk(0.2), chunk(0.29));
        GeneratedAnswer answer = answerWithCitations("Answer [1]", List.of(citation(1)));
        assertThat(guardrail.isLowConfidence("query", chunks, answer)).isTrue();
    }

    @Test
    void someChunksAboveThreshold_notLowConfidence() {
        List<ScoredChunk> chunks = List.of(chunk(0.1), chunk(0.5));
        GeneratedAnswer answer = answerWithCitations("Answer [1]", List.of(citation(1)));
        assertThat(guardrail.isLowConfidence("query", chunks, answer)).isFalse();
    }

    @Test
    void noCitations_isLowConfidence() {
        List<ScoredChunk> chunks = List.of(chunk(0.8));
        GeneratedAnswer answer = answerWithCitations("Answer with no markers", List.of());
        assertThat(guardrail.isLowConfidence("query", chunks, answer)).isTrue();
    }

    @Test
    void refusalPhrase_isLowConfidence() {
        List<ScoredChunk> chunks = List.of(chunk(0.8));
        GeneratedAnswer answer = answerWithCitations(
                "I don't have enough information in the provided documents to answer this question.",
                List.of(citation(1)));
        assertThat(guardrail.isLowConfidence("query", chunks, answer)).isTrue();
    }

    @Test
    void refusalPhrase_caseInsensitive() {
        List<ScoredChunk> chunks = List.of(chunk(0.8));
        GeneratedAnswer answer = answerWithCitations(
                "I DON'T HAVE ENOUGH INFORMATION to answer.",
                List.of(citation(1)));
        assertThat(guardrail.isLowConfidence("query", chunks, answer)).isTrue();
    }

    @Test
    void goodRetrieval_withCitations_notLowConfidence() {
        List<ScoredChunk> chunks = List.of(chunk(0.85), chunk(0.72));
        GeneratedAnswer answer = answerWithCitations(
                "According to [1], the Consumer Protection Act applies. See also [2].",
                List.of(citation(1), citation(2)));
        assertThat(guardrail.isLowConfidence("query", chunks, answer)).isFalse();
    }

    @Test
    void singleHighChunk_withCitation_notLowConfidence() {
        List<ScoredChunk> chunks = List.of(chunk(0.95));
        GeneratedAnswer answer = answerWithCitations("As per [1].", List.of(citation(1)));
        assertThat(guardrail.isLowConfidence("query", chunks, answer)).isFalse();
    }
}
