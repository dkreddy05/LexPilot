package com.lexpilot.generation.llm;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.exception.LexPilotException;
import com.lexpilot.common.exception.UpstreamServiceException;
import com.lexpilot.generation.prompt.PromptMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat-completion client.
 * <p>
 * Calls {@code POST /chat/completions} with the configured model, max tokens,
 * and temperature. Handles timeout, rate-limit (429), and malformed responses
 * explicitly — raw client exceptions never leak past this boundary.
 */
@Component
public class OpenAiLlmClient implements LlmApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final RestClient restClient;
    private final AppConfig.LlmConfig llmConfig;

    public OpenAiLlmClient(AppConfig appConfig) {
        this.llmConfig = appConfig.llm();
        this.restClient = RestClient.builder()
                .baseUrl(llmConfig.baseUrl())
                .defaultHeader("Authorization", "Bearer " + llmConfig.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public LlmResponse complete(List<PromptMessage> messages) {
        log.debug("Calling LLM ({}) with {} message(s), maxTokens={}",
                llmConfig.model(), messages.size(), llmConfig.maxTokens());

        List<Map<String, String>> messagePayload = messages.stream()
                .map(m -> Map.of(
                        "role", m.role().name().toLowerCase(),
                        "content", m.content()))
                .toList();

        Map<String, Object> requestBody = Map.of(
                "model", llmConfig.model(),
                "messages", messagePayload,
                "max_tokens", llmConfig.maxTokens(),
                "temperature", llmConfig.temperature()
        );

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 429) {
                            throw new UpstreamServiceException("llm-service (rate limited)",
                                    new RuntimeException("HTTP 429 from LLM API"));
                        }
                        throw new LexPilotException(
                                "LLM API returned " + res.getStatusCode().value(),
                                "LP-5040");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new UpstreamServiceException("llm-service",
                                new RuntimeException("HTTP " + res.getStatusCode().value()));
                    })
                    .body(ChatCompletionResponse.class);

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new LexPilotException("Malformed response from LLM API: no choices",
                        "LP-5041");
            }

            String text = response.choices().get(0).message().content();
            if (text == null || text.isBlank()) {
                throw new LexPilotException("LLM returned empty content", "LP-5042");
            }

            log.debug("LLM response received ({} chars)", text.length());
            return new LlmResponse(text);

        } catch (UpstreamServiceException e) {
            throw e;
        } catch (LexPilotException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.error("Timeout or connection error calling LLM API", e);
            throw new UpstreamServiceException("llm-service (timeout)", e);
        } catch (Exception e) {
            log.error("Unexpected error calling LLM API", e);
            throw new UpstreamServiceException("llm-service", e);
        }
    }

    // ---- OpenAI response DTOs (minimal) ----

    record ChatCompletionResponse(List<Choice> choices) {}

    record Choice(Message message) {}

    record Message(String role, String content) {}
}
