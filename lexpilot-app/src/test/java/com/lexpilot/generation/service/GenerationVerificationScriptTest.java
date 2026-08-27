package com.lexpilot.generation.service;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.generation.citation.RegexCitationFormatter;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.generation.guardrail.NoOpLowConfidenceGuardrail;
import com.lexpilot.generation.llm.OpenAiLlmClient;
import com.lexpilot.generation.prompt.LegalPromptBuilder;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Standalone verification script to test LLM generation quality.
 * This runs the REAL generation pipeline against the REAL OpenAI API.
 * 
 * INSTRUCTIONS:
 * 1. Set the LLM_API_KEY environment variable with your OpenAI API key.
 * 2. Remove the @Disabled annotation from the tests.
 * 3. Run this class via your IDE or Maven.
 */
class GenerationVerificationScriptTest {

    private static final Logger log = LoggerFactory.getLogger(GenerationVerificationScriptTest.class);

    private GenerationService buildGenerationService() {
        // Read API key from environment variable
        String apiKey = System.getenv("LLM_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            // Provide a dummy key to avoid initialization crash if disabled
            apiKey = "missing-key"; 
        }

        // Mock AppConfig with the API key and default settings
        String finalApiKey = apiKey;
        AppConfig appConfig = new AppConfig(
                null, null,
                new AppConfig.LlmConfig(finalApiKey, "https://api.openai.com/v1", "gpt-4o-mini", 1024, 0.2, 30),
                null, null, null, new AppConfig.ConversationConfig(10)
        );

        OpenAiLlmClient llmClient = new OpenAiLlmClient(appConfig);
        LegalPromptBuilder promptBuilder = new LegalPromptBuilder();
        RegexCitationFormatter formatter = new RegexCitationFormatter();
        NoOpLowConfidenceGuardrail guardrail = new NoOpLowConfidenceGuardrail();

        return new GenerationService(promptBuilder, llmClient, formatter, guardrail);
    }

    @Test
    @Disabled("Requires LLM_API_KEY to be set in environment")
    void testStrongRetrievalMatches() {
        GenerationService generationService = buildGenerationService();

        String query = "How do I get a refund for a defective product under the Consumer Protection Act?";
        
        List<ScoredChunk> chunks = List.of(
                new ScoredChunk(UUID.randomUUID(), UUID.randomUUID(),
                        "Under the Consumer Protection Act, 2019, if a product is found to be defective, " +
                        "the consumer has the right to demand a replacement or a full refund from the seller.", 
                        0.95, "Consumer_Protection_Act_Overview.pdf"),
                new ScoredChunk(UUID.randomUUID(), UUID.randomUUID(),
                        "To initiate a refund, the consumer must provide the original receipt and " +
                        "file a complaint within 30 days of purchase.", 
                        0.88, "Refund_Guidelines.pdf")
        );

        log.info("Running query with strong retrieval matches...");
        GeneratedAnswer answer = generationService.generate(query, chunks);

        log.info("== ANSWER ==\n{}", answer.answer());
        log.info("== CITATIONS ==");
        answer.citations().forEach(c -> log.info("  [{}] -> {}", c.marker(), c.sourceLabel()));
    }

    @Test
    @Disabled("Requires LLM_API_KEY to be set in environment")
    void testOutOfoDomainQuery() {
        GenerationService generationService = buildGenerationService();

        String query = "What is the capital of France and how do I bake a cake?";
        
        List<ScoredChunk> chunks = List.of(
                new ScoredChunk(UUID.randomUUID(), UUID.randomUUID(),
                        "The Consumer Protection Act protects consumers from unfair trade practices.", 
                        0.25, "General_Rights.pdf")
        );

        log.info("Running out-of-domain query...");
        GeneratedAnswer answer = generationService.generate(query, chunks);

        log.info("== ANSWER ==\n{}", answer.answer());
        log.info("== CITATIONS == (Should be empty)");
        answer.citations().forEach(c -> log.info("  [{}] -> {}", c.marker(), c.sourceLabel()));
    }
}
