package com.lexpilot.generation.pii;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op PII scrubber for development and testing environments
 * where PII redaction is not needed. Activated when
 * {@code lexpilot.pii.enabled=false} (the default for local dev).
 */
@Component
@ConditionalOnProperty(name = "lexpilot.pii.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPiiScrubber implements PiiScrubber {

    @Override
    public ScrubResult scrub(String text) {
        return ScrubResult.unchanged(text != null ? text : "");
    }
}
