package com.lexpilot.generation.llm;

import com.lexpilot.generation.prompt.PromptMessage;

import java.util.List;

import reactor.core.publisher.Flux;

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
    default LlmResponse complete(List<PromptMessage> messages) {
        return complete(messages, false);
    }
    
    LlmResponse complete(List<PromptMessage> messages, boolean useFastModel);

    /**
     * Send a chat-completion request and stream the response back.
     *
     * @param messages ordered list of prompt messages
     * @return a Flux of text chunks emitted as they arrive
     */
    default Flux<String> stream(List<PromptMessage> messages) {
        return stream(messages, false);
    }

    Flux<String> stream(List<PromptMessage> messages, boolean useFastModel);
}
