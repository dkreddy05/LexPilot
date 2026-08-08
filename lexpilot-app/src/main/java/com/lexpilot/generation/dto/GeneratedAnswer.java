package com.lexpilot.generation.dto;

import java.util.List;

/**
 * The final output of the generation pipeline: a grounded answer with citations.
 *
 * @param answer        the LLM-generated answer text (with [n] citation markers)
 * @param citations     parsed citations mapping markers to source chunks
 * @param lowConfidence true if the guardrail flagged this answer as low-confidence
 */
public record GeneratedAnswer(
        String answer,
        List<Citation> citations,
        boolean lowConfidence
) {}
