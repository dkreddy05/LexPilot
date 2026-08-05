package com.lexpilot.generation.llm;

import com.lexpilot.common.config.AppConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiLlmClient implements LlmApiClient {

    private final RestClient restClient;
    private final AppConfig appConfig;

    public OpenAiLlmClient(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.restClient = RestClient.builder()
                .baseUrl(appConfig.llm().baseUrl())
                .defaultHeader("Authorization", "Bearer " + appConfig.llm().apiKey())
                .build();
    }

    @Override
    public String complete(String prompt) {
        // TODO: Call OpenAI-compatible /chat/completions endpoint
        throw new UnsupportedOperationException("OpenAiLlmClient.complete() not yet implemented");
    }
}
