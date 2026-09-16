package com.lexpilot.generation.pii;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regex-based PII scrubber targeting Indian personal data formats.
 * <p>
 * Detects and redacts:
 * <ul>
 *   <li>Aadhaar numbers (12-digit, with optional spaces/hyphens)</li>
 *   <li>PAN card numbers (ABCDE1234F format)</li>
 *   <li>Indian mobile numbers (+91/0 prefix, 10-digit)</li>
 *   <li>Email addresses</li>
 *   <li>Bank account numbers (10-18 consecutive digits)</li>
 *   <li>IFSC codes (11-character alphanumeric)</li>
 * </ul>
 * <p>
 * Each detected PII instance is replaced with a type-specific marker
 * (e.g. {@code [REDACTED_AADHAAR]}) to preserve text structure for the LLM
 * while removing sensitive data.
 * <p>
 * Activated only when {@code lexpilot.pii.enabled=true} (recommended for production).
 */
@Component
@ConditionalOnProperty(name = "lexpilot.pii.enabled", havingValue = "true")
public class RegexPiiScrubber implements PiiScrubber {

    private static final Logger log = LoggerFactory.getLogger(RegexPiiScrubber.class);

    /**
     * Ordered list of PII patterns. Order matters — more specific patterns
     * (e.g. Aadhaar) should be matched before generic ones (e.g. bank account).
     */
    private static final List<PiiPattern> PATTERNS = List.of(
            // Aadhaar: 12 digits, optionally separated by spaces or hyphens
            new PiiPattern("AADHAAR",
                    Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"),
                    "[REDACTED_AADHAAR]"),

            // PAN: 5 uppercase letters + 4 digits + 1 uppercase letter
            new PiiPattern("PAN",
                    Pattern.compile("\\b[A-Z]{5}\\d{4}[A-Z]\\b"),
                    "[REDACTED_PAN]"),

            // IFSC Code: 4 letters + 0 + 6 alphanumeric
            new PiiPattern("IFSC",
                    Pattern.compile("\\b[A-Z]{4}0[A-Z0-9]{6}\\b"),
                    "[REDACTED_IFSC]"),

            // Indian mobile: optional +91 or 0 prefix, 10 digits starting with 6-9
            new PiiPattern("PHONE",
                    Pattern.compile("(?:\\+91[\\s-]?|0)?[6-9]\\d{9}\\b"),
                    "[REDACTED_PHONE]"),

            // Email addresses
            new PiiPattern("EMAIL",
                    Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
                    "[REDACTED_EMAIL]"),

            // Bank account numbers: 10-18 digit sequences (matched last to avoid
            // false positives with Aadhaar which is also 12 digits)
            new PiiPattern("BANK_ACCOUNT",
                    Pattern.compile("\\b\\d{10,18}\\b"),
                    "[REDACTED_BANK_ACCOUNT]")
    );

    @Override
    public ScrubResult scrub(String text) {
        if (text == null || text.isBlank()) {
            return ScrubResult.unchanged(text != null ? text : "");
        }

        String scrubbed = text;
        List<ScrubResult.RedactedItem> redactedItems = new ArrayList<>();

        for (PiiPattern piiPattern : PATTERNS) {
            Matcher matcher = piiPattern.pattern().matcher(scrubbed);
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;

            while (matcher.find()) {
                sb.append(scrubbed, lastEnd, matcher.start());
                sb.append(piiPattern.replacement());
                redactedItems.add(new ScrubResult.RedactedItem(
                        piiPattern.type(), matcher.start(), matcher.end() - matcher.start()));
                lastEnd = matcher.end();
            }

            if (lastEnd > 0) {
                sb.append(scrubbed, lastEnd, scrubbed.length());
                scrubbed = sb.toString();
            }
        }

        if (!redactedItems.isEmpty()) {
            log.debug("PII scrubber redacted {} items: {}", redactedItems.size(),
                    redactedItems.stream()
                            .map(ScrubResult.RedactedItem::type)
                            .distinct()
                            .toList());
        }

        return new ScrubResult(scrubbed, redactedItems, redactedItems.size());
    }

    /**
     * Immutable pattern descriptor for a PII category.
     */
    private record PiiPattern(String type, Pattern pattern, String replacement) {}
}
