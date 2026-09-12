package com.lexpilot.generation.guardrail;

import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;

import java.util.List;

/**
 * No-op guardrail — always returns {@code false} (not low-confidence).
 * <p>
 * Superseded by {@link ScoreBasedLowConfidenceGuardrail} which is now the
 * active {@code @Component}. This class is retained for testing purposes
 * where a pass-through guardrail is needed.
 */
public class NoOpLowConfidenceGuardrail implements LowConfidenceGuardrail {

    @Override
    public boolean isLowConfidence(String query, List<ScoredChunk> chunks, GeneratedAnswer answer) {
        return false;
    }
}

