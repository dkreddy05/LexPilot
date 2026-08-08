package com.lexpilot.generation.prompt;

import com.lexpilot.retrieval.dto.ScoredChunk;

import java.util.List;

/**
 * Builds the chat-completion prompt (system + user messages) from a query
 * and its retrieved context chunks.
 */
public interface PromptBuilder {

    /**
     * Build an ordered list of prompt messages for the LLM.
     *
     * @param query  the user's natural-language query
     * @param chunks scored chunks ordered by relevance (descending)
     * @return messages to send to the LLM (system first, then user)
     */
    List<PromptMessage> build(String query, List<ScoredChunk> chunks);
}
