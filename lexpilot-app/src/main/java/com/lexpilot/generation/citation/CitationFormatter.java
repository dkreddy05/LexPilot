package com.lexpilot.generation.citation;

import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;

import java.util.List;

/**
 * Parses citation markers from the LLM's raw output and maps them back
 * to the source chunks that were provided as context.
 */
public interface CitationFormatter {

    /**
     * Parse the raw LLM answer, extract citation markers, and produce
     * a {@link GeneratedAnswer} with a validated citation list.
     *
     * @param rawAnswer     the LLM's generated text (may contain [n] markers)
     * @param orderedChunks chunks in the same order they were presented to the LLM
     * @return generated answer with parsed citations
     */
    GeneratedAnswer format(String rawAnswer, List<ScoredChunk> orderedChunks);
}
