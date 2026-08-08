package com.lexpilot.generation.llm;

import com.lexpilot.generation.prompt.PromptMessage;

import java.util.List;

/**
 * Abstraction over the LLM completion API. Implementations handle HTTP/SDK
 * details — callers only see typed messages in, typed response out.
 */
public interface LlmApiClient {

    /**
     * Send a chat-completion request to the LLM.
     *
     * @param messages ordered list of prompt messages (system first, then user)
     * @return the LLM's generated response
     */
    LlmResponse complete(List<PromptMessage> messages);
}
