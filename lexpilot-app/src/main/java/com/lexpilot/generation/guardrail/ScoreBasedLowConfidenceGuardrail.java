package com.lexpilot.generation.guardrail;

import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Heuristic-based guardrail that flags answers as low-confidence when
 * retrieval quality or LLM grounding signals are weak.
 * <p>
 * Triggers (any one is sufficient):
 * <ol>
 *   <li><b>Empty retrieval</b> — no chunks were provided as context</li>
 *   <li><b>Low retrieval scores</b> — all chunks scored below a threshold</li>
 *   <li><b>Zero citations</b> — the LLM produced no [n] markers despite having context</li>
 *   <li><b>Model refusal</b> — the answer contains the explicit refusal phrase
 *       instructed in the system prompt</li>
 * </ol>
 */
@Component
public class ScoreBasedLowConfidenceGuardrail implements LowConfidenceGuardrail {

    private static final Logger log = LoggerFactory.getLogger(ScoreBasedLowConfidenceGuardrail.class);

    /**
     * Minimum retrieval score threshold. If every chunk scores below this,
     * the context is deemed too weak to produce a reliable answer.
     */
    private static final double MIN_SCORE_THRESHOLD = 0.3;

    /**
     * The refusal phrase the system prompt instructs the LLM to use when
     * context is insufficient. Checked case-insensitively.
     */
    private static final String REFUSAL_PHRASE = "I don't have enough information";

    @Override
    public boolean isLowConfidence(String query, List<ScoredChunk> chunks, GeneratedAnswer answer) {
        // 1. No retrieval context at all
        if (chunks == null || chunks.isEmpty()) {
            log.debug("Low confidence: no retrieval chunks for query '{}'", truncate(query));
            return true;
        }

        // 2. All retrieval scores below threshold
        boolean allLowScores = chunks.stream().allMatch(c -> c.score() < MIN_SCORE_THRESHOLD);
        if (allLowScores) {
            log.debug("Low confidence: all {} chunk scores below {} for query '{}'",
                    chunks.size(), MIN_SCORE_THRESHOLD, truncate(query));
            return true;
        }

        // 3. LLM produced an answer with context but cited nothing
        if (answer.citations().isEmpty()) {
            log.debug("Low confidence: zero citations despite {} context chunks for query '{}'",
                    chunks.size(), truncate(query));
            return true;
        }

        // 4. LLM explicitly refused to answer
        if (answer.answer() != null
                && answer.answer().toLowerCase().contains(REFUSAL_PHRASE.toLowerCase())) {
            log.debug("Low confidence: LLM refusal detected for query '{}'", truncate(query));
            return true;
        }

        return false;
    }

    private static String truncate(String text) {
        if (text == null) return "";
        return text.length() > 60 ? text.substring(0, 60) + "…" : text;
    }
}
