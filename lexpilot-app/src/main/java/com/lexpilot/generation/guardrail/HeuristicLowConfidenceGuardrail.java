package com.lexpilot.generation.guardrail;

import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Heuristic-based guardrail to flag potentially hallucinated, ungrounded,
 * or low-confidence assistant answers.
 */
@Component
@Primary
public class HeuristicLowConfidenceGuardrail implements LowConfidenceGuardrail {

    private static final Logger log = LoggerFactory.getLogger(HeuristicLowConfidenceGuardrail.class);

    /** Cosine similarity below this indicates poor semantic relevance */
    private static final double MIN_TOP_CHUNK_SCORE = 0.35;

    private static final List<String> REFUSAL_INDICATORS = List.of(
            "don't have enough information",
            "do not have enough information",
            "cannot find sufficient information",
            "not mentioned in the provided documents",
            "not enough information in the provided"
    );

    @Override
    public boolean isLowConfidence(String query, List<ScoredChunk> chunks, GeneratedAnswer answer) {
        // 1. No context was retrieved
        if (chunks == null || chunks.isEmpty()) {
            log.info("Flagged low confidence: No chunks retrieved for query '{}'", query);
            return true;
        }

        // 2. Weak retrieval: top chunk score is below relevance threshold
        double topScore = chunks.get(0).score();
        if (topScore < MIN_TOP_CHUNK_SCORE) {
            log.info("Flagged low confidence: Top chunk score {} is below threshold {}", topScore, MIN_TOP_CHUNK_SCORE);
            return true;
        }

        // 3. Explicit refusal phrase detected from prompt instructions
        String text = answer != null && answer.answer() != null ? answer.answer().toLowerCase() : "";
        for (String indicator : REFUSAL_INDICATORS) {
            if (text.contains(indicator)) {
                log.info("Flagged low confidence: Refusal phrase '{}' detected in answer", indicator);
                return true;
            }
        }

        // 4. Citation-less answer despite non-empty context
        if (answer != null && (answer.citations() == null || answer.citations().isEmpty())) {
            log.info("Flagged low confidence: Assistant generated response without citing any retrieved passages");
            return true;
        }

        return false;
    }
}
