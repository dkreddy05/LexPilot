package com.lexpilot.generation.pii;

/**
 * Scrubs Personally Identifiable Information (PII) from text before
 * sending to external LLM providers.
 */
public interface PiiScrubber {

    /**
     * Analyze and redact PII from the given text.
     *
     * @param text raw text potentially containing PII
     * @return scrub result with redacted text and statistics
     */
    ScrubResult scrub(String text);
}
