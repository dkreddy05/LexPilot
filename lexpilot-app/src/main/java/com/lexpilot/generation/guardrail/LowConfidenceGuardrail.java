package com.lexpilot.generation.guardrail;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class LowConfidenceGuardrail {

    public Optional<String> evaluate(String query, List<String> retrievedChunks, String llmAnswer) {
        // TODO: Evaluate confidence thresholds and return refusal reason if needed
        return Optional.empty();
    }
}
