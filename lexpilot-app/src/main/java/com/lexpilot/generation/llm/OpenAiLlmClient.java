package com.lexpilot.generation.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.exception.LexPilotException;
import com.lexpilot.common.exception.UpstreamServiceException;
import com.lexpilot.common.observability.MetricsService;
import com.lexpilot.generation.prompt.PromptMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat-completion client.
 * <p>
 * Calls {@code POST /chat/completions} with the configured model, max tokens,
 * and temperature. Handles timeout, rate-limit (429), and malformed responses
 * explicitly — raw client exceptions never leak past this boundary.
 * <p>
 * Resilience4j annotations provide:
 * - Retry with exponential backoff on transient failures (429, 5xx, timeouts)
 * - Circuit breaker to fail fast when the LLM provider is persistently down
 */
@Component
public class OpenAiLlmClient implements LlmApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final RestClient restClient;
    private final WebClient webClient;
    private final AppConfig.LlmConfig llmConfig;
    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    public OpenAiLlmClient(AppConfig appConfig, MetricsService metricsService, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.llmConfig = appConfig.llm();
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(llmConfig.baseUrl())
                .defaultHeader("Authorization", "Bearer " + llmConfig.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.webClient = webClientBuilder
                .baseUrl(llmConfig.baseUrl())
                .defaultHeader("Authorization", "Bearer " + llmConfig.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    @Retry(name = "llm-service")
    @CircuitBreaker(name = "llm-service", fallbackMethod = "completeFallback")
    public LlmResponse complete(List<PromptMessage> messages, boolean useFastModel) {
        String model = useFastModel ? llmConfig.fastModel() : llmConfig.defaultModel();
        log.debug("Calling LLM ({}) with {} message(s), maxTokens={}",
                model, messages.size(), llmConfig.maxTokens());

        List<Map<String, String>> messagePayload = messages.stream()
                .map(m -> Map.of(
                        "role", m.role().name().toLowerCase(),
                        "content", m.content()))
                .toList();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messagePayload,
                "max_tokens", llmConfig.maxTokens(),
                "temperature", llmConfig.temperature()
        );

        try {
            ChatCompletionResponse response = metricsService.timeLlmCall(model, () -> restClient.post()
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
                    .body(ChatCompletionResponse.class));

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new LexPilotException("Malformed response from LLM API: no choices",
                        "LP-5041");
            }

            String text = response.choices().get(0).message().content();
            if (text == null || text.isBlank()) {
                throw new LexPilotException("LLM returned empty content", "LP-5042");
            }

            log.debug("LLM response received ({} chars)", text.length());
            metricsService.recordLlmCallSuccess(model);
            return new LlmResponse(text);

        } catch (UpstreamServiceException e) {
            metricsService.recordLlmCallFailure(model, "upstream_error");
            throw e;
        } catch (LexPilotException e) {
            metricsService.recordLlmCallFailure(model, "lexpilot_error");
            throw e;
        } catch (ResourceAccessException e) {
            metricsService.recordLlmCallFailure(model, "timeout");
            log.error("Timeout or connection error calling LLM API", e);
            throw new UpstreamServiceException("llm-service (timeout)", e);
        } catch (Exception e) {
            metricsService.recordLlmCallFailure(model, "unexpected_error");
            log.error("Unexpected error calling LLM API", e);
            throw new UpstreamServiceException("llm-service", e);
        }
    }

    /**
     * Fallback invoked when the circuit breaker is OPEN or all retries are exhausted.
     * Returns a graceful user-facing message instead of an error.
     */
    @SuppressWarnings("unused")
    private LlmResponse completeFallback(List<PromptMessage> messages, boolean useFastModel, Throwable t) {
        log.warn("LLM circuit breaker fallback triggered: {}", t.getMessage());
        return new LlmResponse(
                "I'm sorry, the AI service is temporarily unavailable. " +
                "Please try again in a few moments. If the issue persists, " +
                "your query has been logged and will be addressed.");
    }

    @SuppressWarnings("unused")
    private Flux<String> streamFallback(List<PromptMessage> messages, boolean useFastModel, Throwable t) {
        log.warn("LLM circuit breaker stream fallback triggered: {}", t.getMessage());
        return Flux.just("I'm sorry, the AI service is temporarily unavailable. " +
                "Please try again in a few moments. If the issue persists, " +
                "your query has been logged and will be addressed.");
    }

    @Override
    @Retry(name = "llm-service")
    @CircuitBreaker(name = "llm-service", fallbackMethod = "streamFallback")
    public Flux<String> stream(List<PromptMessage> messages, boolean useFastModel) {
        String model = useFastModel ? llmConfig.fastModel() : llmConfig.defaultModel();
        log.debug("Streaming LLM ({}) with {} message(s), maxTokens={}",
                model, messages.size(), llmConfig.maxTokens());

        List<Map<String, String>> messagePayload = messages.stream()
                .map(m -> Map.of(
                        "role", m.role().name().toLowerCase(),
                        "content", m.content()))
                .toList();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messagePayload,
                "max_tokens", llmConfig.maxTokens(),
                "temperature", llmConfig.temperature(),
                "stream", true
        );

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class) // OpenAI streams JSON chunks prefixed with "data: "
                .mapNotNull(chunk -> {
                    String trimmed = chunk.trim();
                    if (trimmed.isEmpty() || trimmed.equals("[DONE]")) return null;
                    if (trimmed.startsWith("data: ")) {
                        trimmed = trimmed.substring(6);
                    }
                    if (trimmed.equals("[DONE]")) return null;
                    
                    try {
                        ChatCompletionResponse chunkResponse = objectMapper.readValue(trimmed, ChatCompletionResponse.class);
                        if (chunkResponse != null && chunkResponse.choices() != null && !chunkResponse.choices().isEmpty()) {
                            Choice choice = chunkResponse.choices().get(0);
                            
                            // Try message first (for non-streaming or if provider returns message)
                            if (choice.message() != null && choice.message().content() != null) {
                                return choice.message().content();
                            }
                            
                            // Try delta (for streaming)
                            if (choice.delta() != null && choice.delta().content() != null) {
                                return choice.delta().content();
                            }
                        }
                        return null;
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    // ---- OpenAI response DTOs (minimal) ----

    record ChatCompletionResponse(List<Choice> choices) {}

    // We add delta for streaming
    record Choice(Message message, Message delta) {}

    record Message(String role, String content) {}
}
