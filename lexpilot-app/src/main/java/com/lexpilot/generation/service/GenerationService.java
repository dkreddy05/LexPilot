package com.lexpilot.generation.service;

import com.lexpilot.generation.citation.CitationFormatter;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.generation.guardrail.LowConfidenceGuardrail;
import com.lexpilot.generation.llm.LlmApiClient;
import com.lexpilot.generation.llm.LlmResponse;
import com.lexpilot.generation.prompt.PromptBuilder;
import com.lexpilot.generation.prompt.PromptMessage;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the generation pipeline: prompt construction → LLM call →
 * citation parsing → low-confidence check.
 * <p>
 * Each step is delegated to a focused component behind an interface, so
 * individual pieces can be swapped or tested in isolation.
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    private final PromptBuilder promptBuilder;
    private final LlmApiClient llmApiClient;
    private final CitationFormatter citationFormatter;
    private final LowConfidenceGuardrail guardrail;

    public GenerationService(PromptBuilder promptBuilder,
                             LlmApiClient llmApiClient,
                             CitationFormatter citationFormatter,
                             LowConfidenceGuardrail guardrail) {
        this.promptBuilder = promptBuilder;
        this.llmApiClient = llmApiClient;
        this.citationFormatter = citationFormatter;
        this.guardrail = guardrail;
    }

    /**
     * Generate a grounded, cited answer for the given query using retrieved chunks.
     * Stateless — no conversation history is included.
     *
     * @param query  the user's natural-language query
     * @param chunks scored chunks from retrieval, ordered by relevance (descending)
     * @return a generated answer with citations and confidence flag
     */
    public GeneratedAnswer generate(String query, List<ScoredChunk> chunks) {
        return generate(query, chunks, List.of());
    }

    /**
     * Generate a grounded, cited answer for the given query using retrieved chunks,
     * with conversation history for multi-turn context.
     *
     * @param query               the user's natural-language query
     * @param chunks              scored chunks from retrieval, ordered by relevance (descending)
     * @param conversationHistory prior USER/ASSISTANT prompt messages (chronological)
     * @return a generated answer with citations and confidence flag
     */
    public GeneratedAnswer generate(String query, List<ScoredChunk> chunks,
                                    List<PromptMessage> conversationHistory) {
        log.debug("Starting generation for query ({} chars) with {} chunks, {} history messages",
                query.length(), chunks.size(), conversationHistory.size());

        // 1. Build prompt messages (with history if available)
        List<PromptMessage> messages = promptBuilder.build(query, chunks, conversationHistory);

        // 2. Call LLM
        LlmResponse llmResponse = llmApiClient.complete(messages);

        // 3. Parse citations from raw answer
        GeneratedAnswer answer = citationFormatter.format(llmResponse.text(), chunks);

        // 4. Evaluate confidence (no-op for now)
        boolean lowConfidence = guardrail.isLowConfidence(query, chunks, answer);

        log.debug("Generation complete: {} citations, lowConfidence={}",
                answer.citations().size(), lowConfidence);

        return new GeneratedAnswer(answer.answer(), answer.citations(), lowConfidence);
    }
}
