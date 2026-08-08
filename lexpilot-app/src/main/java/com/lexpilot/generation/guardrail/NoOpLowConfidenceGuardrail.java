package com.lexpilot.generation.guardrail;

import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * No-op guardrail — always returns {@code false} (not low-confidence).
 * <p>
 * This is the default implementation wired into GenerationService.
 * Replace with a real implementation once low-confidence patterns are
 * observed during manual verification (e.g. low retrieval scores,
 * model refusal patterns, citation-less answers).
 */
@Component
public class NoOpLowConfidenceGuardrail implements LowConfidenceGuardrail {

    @Override
    public boolean isLowConfidence(String query, List<ScoredChunk> chunks, GeneratedAnswer answer) {
        return false; // TODO: implement once real low-confidence cases are observed
    }
}
