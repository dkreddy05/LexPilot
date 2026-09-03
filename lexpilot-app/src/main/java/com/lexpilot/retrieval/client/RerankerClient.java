package com.lexpilot.retrieval.client;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client for the embedding-service {@code POST /rerank} endpoint.
 * Uses a cross-encoder model to compute deep query-passage cross-attention scores.
 */
@Component
public class RerankerClient {

    private static final Logger log = LoggerFactory.getLogger(RerankerClient.class);

    private final RestClient restClient;
    private final AppConfig appConfig;

    public RerankerClient(AppConfig appConfig) {
        this.appConfig = appConfig;
        this.restClient = RestClient.builder()
                .baseUrl(appConfig.embeddingService().baseUrl())
                .build();
    }

    /**
     * Rerank candidates against the query using the Cross-Encoder.
     * Falls back to the pre-reranked list if the service is unreachable or fails.
     *
     * @param query      natural language user query
     * @param candidates candidate chunks to rerank
     * @param topN       number of top candidates to keep
     * @return reranked chunks with updated cross-encoder scores
     */
    public List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates, int topN) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        if (candidates.size() == 1) {
            return candidates;
        }

        List<String> candidateTexts = candidates.stream().map(ScoredChunk::content).toList();
        RerankRequest request = new RerankRequest(query, candidateTexts);

        try {
            RerankResponse response = restClient.post()
                    .uri("/rerank")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RerankResponse.class);

            if (response == null || response.ranked_indices() == null || response.ranked_indices().isEmpty()) {
                log.warn("Empty response from reranker service, returning original candidates");
                return candidates.stream().limit(topN).toList();
            }

            List<ScoredChunk> reranked = new ArrayList<>();
            List<Integer> indices = response.ranked_indices();
            List<Double> scores = response.scores();

            for (int i = 0; i < indices.size() && reranked.size() < topN; i++) {
                int originalIdx = indices.get(i);
                if (originalIdx >= 0 && originalIdx < candidates.size()) {
                    ScoredChunk orig = candidates.get(originalIdx);
                    double score = (scores != null && i < scores.size()) ? scores.get(i) : orig.score();
                    reranked.add(new ScoredChunk(
                            orig.chunkId(),
                            orig.documentId(),
                            orig.content(),
                            score,
                            orig.sourceLabel()
                    ));
                }
            }

            return reranked;
        } catch (Exception e) {
            log.warn("Reranking call failed ({}), returning original fused order as fallback", e.getMessage());
            return candidates.stream().limit(topN).toList();
        }
    }

    /**
     * String-based overload for backward compatibility.
     */
    public List<String> rerank(String query, List<String> chunkIds, List<String> chunkTexts) {
        if (chunkIds == null || chunkIds.isEmpty()) return Collections.emptyList();

        RerankRequest request = new RerankRequest(query, chunkTexts);
        try {
            RerankResponse response = restClient.post()
                    .uri("/rerank")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RerankResponse.class);

            if (response == null || response.ranked_indices() == null) {
                return chunkIds;
            }

            List<String> rankedIds = new ArrayList<>();
            for (int idx : response.ranked_indices()) {
                if (idx >= 0 && idx < chunkIds.size()) {
                    rankedIds.add(chunkIds.get(idx));
                }
            }
            return rankedIds;
        } catch (Exception e) {
            log.warn("Rerank string call failed, returning chunkIds fallback: {}", e.getMessage());
            return chunkIds;
        }
    }

    public record RerankRequest(String query, List<String> candidates) {}

    public record RerankResponse(List<Integer> ranked_indices, List<Double> scores, String model) {}
}
