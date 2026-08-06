package com.lexpilot.ingestion.service;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * HTTP client for the embedding-service FastAPI microservice.
 * Calls {@code POST /embed} to obtain vector embeddings for text chunks.
 */
@Service
public class EmbeddingServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceClient.class);

    private final RestClient restClient;

    public EmbeddingServiceClient(AppConfig appConfig) {
        this.restClient = RestClient.builder()
                .baseUrl(appConfig.embeddingService().baseUrl())
                .build();
    }

    /**
     * Embed one or more text strings into 384-dim vectors.
     *
     * @param texts the texts to embed
     * @return list of embedding vectors (each a list of floats), same order as input
     */
    public List<List<Float>> embed(List<String> texts) {
        EmbedRequest request = new EmbedRequest(texts);

        try {
            EmbedResponse response = restClient.post()
                    .uri("/embed")
                    .header("Content-Type", "application/json")
                    .body(request)
                    .retrieve()
                    .body(EmbedResponse.class);

            if (response == null || response.embeddings() == null) {
                throw new UpstreamServiceException("embedding-service",
                        new RuntimeException("Null response from /embed"));
            }

            log.debug("Received {} embeddings from embedding-service", response.embeddings().size());
            return response.embeddings();

        } catch (UpstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call embedding-service /embed", e);
            throw new UpstreamServiceException("embedding-service", e);
        }
    }

    // ---- Request / Response DTOs ----

    record EmbedRequest(List<String> texts) {}

    record EmbedResponse(List<List<Float>> embeddings, String model) {}
}
