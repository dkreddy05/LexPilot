package com.lexpilot.generation.llm;

/**
 * Response from the LLM API containing the generated text.
 *
 * @param text the raw generated text (may contain citation markers like [1], [2])
 */
public record LlmResponse(String text) {}
