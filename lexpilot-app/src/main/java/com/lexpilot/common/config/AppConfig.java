package com.lexpilot.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lexpilot")
public record AppConfig(
        TenantConfig tenant,
        EmbeddingServiceConfig embeddingService,
        LlmConfig llm,
        IngestionConfig ingestion,
        RetrievalConfig retrieval,
        RateLimitingConfig rateLimiting,
        ConversationConfig conversation
) {
    public record TenantConfig(
            boolean enabled,
            String defaultTenantId
    ) {}
    public record EmbeddingServiceConfig(String baseUrl) {}

    public record LlmConfig(
            String apiKey,
            String baseUrl,
            String defaultModel,
            String fastModel,
            int maxTokens,
            double temperature,
            int timeoutSeconds
    ) {}

    public record IngestionConfig(
            String kafkaTopic,
            int chunkSize,
            int chunkOverlap,
            String uploadDir,
            int maxFileSizeMb
    ) {}

    public record RetrievalConfig(
            int vectorTopK,
            int bm25TopK,
            int rrfTopK
    ) {}

    public record RateLimitingConfig(
            int requestsPerMinute,
            int requestsPerDay
    ) {}

    public record ConversationConfig(
            int maxHistoryTurns
    ) {
        public ConversationConfig {
            if (maxHistoryTurns <= 0) {
                maxHistoryTurns = 10;
            }
        }
    }
}
