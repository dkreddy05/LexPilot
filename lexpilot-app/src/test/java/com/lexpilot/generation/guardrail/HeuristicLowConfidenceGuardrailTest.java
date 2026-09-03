package com.lexpilot.generation.guardrail;

import com.lexpilot.generation.dto.Citation;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicLowConfidenceGuardrailTest {

    private HeuristicLowConfidenceGuardrail guardrail;

    @BeforeEach
    void setUp() {
        guardrail = new HeuristicLowConfidenceGuardrail();
    }

    @Test
    void emptyChunks_flagsLowConfidence() {
        GeneratedAnswer answer = new GeneratedAnswer("Some answer", Collections.emptyList(), false);
        boolean result = guardrail.isLowConfidence("what is the limit?", Collections.emptyList(), answer);
        assertThat(result).isTrue();
    }

    @Test
    void lowTopScore_flagsLowConfidence() {
        ScoredChunk chunk = new ScoredChunk(UUID.randomUUID(), UUID.randomUUID(), "text", 0.20, "doc.pdf");
        Citation citation = new Citation(1, chunk.chunkId(), chunk.documentId(), "doc.pdf");
        GeneratedAnswer answer = new GeneratedAnswer("Answer with citation [1]", List.of(citation), false);

        boolean result = guardrail.isLowConfidence("query", List.of(chunk), answer);
        assertThat(result).isTrue();
    }

    @Test
    void refusalPhrase_flagsLowConfidence() {
        ScoredChunk chunk = new ScoredChunk(UUID.randomUUID(), UUID.randomUUID(), "text", 0.85, "doc.pdf");
        Citation citation = new Citation(1, chunk.chunkId(), chunk.documentId(), "doc.pdf");
        GeneratedAnswer answer = new GeneratedAnswer(
                "I don't have enough information in the provided documents to answer this question.",
                List.of(citation),
                false
        );

        boolean result = guardrail.isLowConfidence("query", List.of(chunk), answer);
        assertThat(result).isTrue();
    }

    @Test
    void noCitationsWithContext_flagsLowConfidence() {
        ScoredChunk chunk = new ScoredChunk(UUID.randomUUID(), UUID.randomUUID(), "text", 0.85, "doc.pdf");
        GeneratedAnswer answer = new GeneratedAnswer("Answer without any citations.", Collections.emptyList(), false);

        boolean result = guardrail.isLowConfidence("query", List.of(chunk), answer);
        assertThat(result).isTrue();
    }

    @Test
    void highConfidenceGroundedAnswer_doesNotFlag() {
        ScoredChunk chunk = new ScoredChunk(UUID.randomUUID(), UUID.randomUUID(), "text", 0.88, "doc.pdf");
        Citation citation = new Citation(1, chunk.chunkId(), chunk.documentId(), "doc.pdf");
        GeneratedAnswer answer = new GeneratedAnswer("Under Section 35, the limitation is 2 years [1].", List.of(citation), false);

        boolean result = guardrail.isLowConfidence("query", List.of(chunk), answer);
        assertThat(result).isFalse();
    }
}
