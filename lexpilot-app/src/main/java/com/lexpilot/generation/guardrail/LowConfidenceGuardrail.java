package com.lexpilot.generation.guardrail;

import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;

import java.util.List;

/**
 * Evaluates whether a generated answer should be flagged as low-confidence.
 * <p>
 * Wired into {@code GenerationService} as an integration point. The actual
 * logic will be implemented once real low-confidence cases are observed
 * during manual verification.
 */
public interface LowConfidenceGuardrail {

    /**
     * Check whether the generated answer should be flagged as low-confidence.
     *
     * @param query   the user's original query
     * @param chunks  the retrieved context chunks
     * @param answer  the generated answer with citations
     * @return {@code true} if the answer is low-confidence
     */
    boolean isLowConfidence(String query, List<ScoredChunk> chunks, GeneratedAnswer answer);
}
