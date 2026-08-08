package com.lexpilot.generation.citation;

import com.lexpilot.generation.dto.Citation;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts {@code [n]} citation markers from the LLM's raw answer via regex,
 * maps each to the corresponding chunk in the ordered context list.
 * <p>
 * Design choices:
 * <ul>
 *   <li>Hallucinated markers (index out of bounds) are dropped with a WARN log,
 *       signalling the system prompt may need tightening.</li>
 *   <li>Duplicate citations (same chunk cited multiple times) are deduplicated,
 *       keeping the first occurrence's marker number.</li>
 *   <li>{@code lowConfidence} is hardcoded {@code false} — the guardrail sets
 *       the real value downstream.</li>
 * </ul>
 */
@Component
public class RegexCitationFormatter implements CitationFormatter {

    private static final Logger log = LoggerFactory.getLogger(RegexCitationFormatter.class);

    /** Matches [1], [2], [10], etc. in the LLM output. */
    private static final Pattern MARKER_PATTERN = Pattern.compile("\\[(\\d+)]");

    @Override
    public GeneratedAnswer format(String rawAnswer, List<ScoredChunk> orderedChunks) {
        List<Citation> citations = new ArrayList<>();
        Set<java.util.UUID> seenChunkIds = new LinkedHashSet<>();

        Matcher matcher = MARKER_PATTERN.matcher(rawAnswer);
        while (matcher.find()) {
            int markerNumber = Integer.parseInt(matcher.group(1));
            int chunkIndex = markerNumber - 1; // markers are 1-based, list is 0-based

            if (chunkIndex < 0 || chunkIndex >= orderedChunks.size()) {
                log.warn("Hallucinated citation marker [{}] — no chunk at index {}. "
                         + "Total chunks: {}. Consider tightening the system prompt.",
                        markerNumber, chunkIndex, orderedChunks.size());
                continue;
            }

            ScoredChunk chunk = orderedChunks.get(chunkIndex);

            // Dedupe by chunkId — first occurrence wins
            if (seenChunkIds.add(chunk.chunkId())) {
                citations.add(new Citation(
                        markerNumber,
                        chunk.chunkId(),
                        chunk.documentId(),
                        chunk.sourceLabel()
                ));
            }
        }

        return new GeneratedAnswer(rawAnswer, citations, false);
    }
}
