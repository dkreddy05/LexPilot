package com.lexpilot.generation.pii;

import java.util.Collections;
import java.util.List;

/**
 * Result of PII scrubbing — carries the redacted text and metadata
 * about what was redacted, for audit and debugging purposes.
 *
 * @param scrubbedText     text with PII replaced by redaction markers
 * @param redactedItems    list describing each redacted element
 * @param totalRedactions  count of PII instances found and redacted
 */
public record ScrubResult(
        String scrubbedText,
        List<RedactedItem> redactedItems,
        int totalRedactions
) {
    /**
     * Convenience factory for no-redaction pass-through.
     */
    public static ScrubResult unchanged(String text) {
        return new ScrubResult(text, Collections.emptyList(), 0);
    }

    /**
     * Describes a single redacted PII element.
     *
     * @param type   category of PII (e.g. AADHAAR, PAN, PHONE, EMAIL, BANK_ACCOUNT)
     * @param offset character offset in the original text where the PII was found
     * @param length length of the original PII string
     */
    public record RedactedItem(String type, int offset, int length) {}
}
