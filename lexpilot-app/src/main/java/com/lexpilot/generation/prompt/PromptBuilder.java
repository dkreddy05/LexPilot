package com.lexpilot.generation.prompt;

import com.lexpilot.retrieval.dto.ScoredChunk;

import java.util.List;

/**
 * Builds the chat-completion prompt (system + user messages) from a query
 * and its retrieved context chunks, optionally including conversation history.
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

    /**
     * Build an ordered list of prompt messages for the LLM, including
     * conversation history for multi-turn context.
     * <p>
     * The resulting message list is ordered: SYSTEM → history (USER/ASSISTANT pairs)
     * → current USER message with RAG context.
     *
     * @param query               the user's natural-language query
     * @param chunks              scored chunks ordered by relevance (descending)
     * @param conversationHistory prior turns as USER/ASSISTANT prompt messages (chronological)
     * @return messages to send to the LLM
     */
    List<PromptMessage> build(String query, List<ScoredChunk> chunks,
                              List<PromptMessage> conversationHistory);
}
