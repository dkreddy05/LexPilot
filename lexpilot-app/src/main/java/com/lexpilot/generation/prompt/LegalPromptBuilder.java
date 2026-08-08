package com.lexpilot.generation.prompt;

import com.lexpilot.retrieval.dto.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Prompt builder tailored for a legal-rights assistant (LexPilot).
 * <p>
 * The system message explicitly forbids answering outside the retrieved context
 * and instructs the LLM to cite sources using numbered [n] markers matching
 * chunk order. This is the highest-leverage instruction for grounded legal
 * answers — the model must refuse rather than fabricate.
 */
@Component
public class LegalPromptBuilder implements PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are LexPilot, an AI assistant specialising in Indian consumer rights \
            and grievance redressal. Your role is to help users understand their legal \
            rights and the steps they can take to resolve consumer complaints.

            STRICT RULES:
            1. Answer ONLY using the context passages provided below. Do NOT use any \
               prior knowledge, training data, or external information.
            2. Cite your sources using numbered markers like [1], [2], etc. Each marker \
               corresponds to the numbered context passage it was drawn from.
            3. If the provided context does not contain enough information to answer \
               the question, say exactly: "I don't have enough information in the \
               provided documents to answer this question." Do NOT guess or fabricate.
            4. Be precise and factual. When citing legal provisions, Acts, or sections, \
               only mention those explicitly stated in the context.
            5. Keep your answer concise and well-structured. Use bullet points or \
               numbered steps where appropriate.
            """;

    @Override
    public List<PromptMessage> build(String query, List<ScoredChunk> chunks) {
        StringBuilder userMessage = new StringBuilder();

        // Numbered context blocks — ordering matches [n] citation markers
        userMessage.append("CONTEXT PASSAGES:\n\n");
        for (int i = 0; i < chunks.size(); i++) {
            ScoredChunk chunk = chunks.get(i);
            userMessage.append('[').append(i + 1).append("] ");
            userMessage.append("(Source: ").append(chunk.sourceLabel()).append(")\n");
            userMessage.append(chunk.content()).append("\n\n");
        }

        userMessage.append("---\n\n");
        userMessage.append("QUESTION: ").append(query);

        return List.of(
                new PromptMessage(PromptMessage.Role.SYSTEM, SYSTEM_PROMPT),
                new PromptMessage(PromptMessage.Role.USER, userMessage.toString())
        );
    }
}
