package com.lexpilot.retrieval.client;

import com.lexpilot.common.config.AppConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class RerankerClient {

    private final RestClient restClient;
    private final AppConfig appConfig;

    public RerankerClient(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.restClient = RestClient.builder()
                .baseUrl(appConfig.embeddingService().baseUrl())
                .build();
    }

    public List<String> rerank(String query, List<String> chunkIds, List<String> chunkTexts) {
        // TODO: Call embedding-service /rerank endpoint
        throw new UnsupportedOperationException("RerankerClient.rerank() not yet implemented");
    }
}
