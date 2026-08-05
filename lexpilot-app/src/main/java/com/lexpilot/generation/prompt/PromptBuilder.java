package com.lexpilot.generation.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String build(String userQuery, List<String> contextChunks, String domain) {
        // TODO: Construct LLM prompt with system prompt and retrieved context
        throw new UnsupportedOperationException("PromptBuilder.build() not yet implemented");
    }
}
