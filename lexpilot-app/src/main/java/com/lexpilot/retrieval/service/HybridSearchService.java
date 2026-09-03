package com.lexpilot.retrieval.service;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.ingestion.service.EmbeddingServiceClient;
import com.lexpilot.retrieval.client.RerankerClient;
import com.lexpilot.retrieval.dto.ScoredChunk;
import com.lexpilot.retrieval.fusion.ReciprocalRankFusion;
import com.lexpilot.retrieval.repository.BM25SearchRepository;
import com.lexpilot.retrieval.repository.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Search service that orchestrates True Hybrid Retrieval:
 * 1. Query embedding & Dense pgvector search
 * 2. Full-text BM25 sparse search (PostgreSQL tsvector)
 * 3. Reciprocal Rank Fusion (RRF) combining dense & sparse ranks
 * 4. Deep cross-attention reranking (BGE cross-encoder)
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    private static final int IVFFLAT_PROBES = 10;

    private final VectorSearchRepository vectorSearchRepository;
    private final BM25SearchRepository bm25SearchRepository;
    private final ReciprocalRankFusion rrf;
    private final RerankerClient rerankerClient;
    private final EmbeddingServiceClient embeddingClient;
    private final JdbcTemplate jdbcTemplate;
    private final AppConfig appConfig;

    public HybridSearchService(VectorSearchRepository vectorSearchRepository,
                               BM25SearchRepository bm25SearchRepository,
                               ReciprocalRankFusion rrf,
                               RerankerClient rerankerClient,
                               EmbeddingServiceClient embeddingClient,
                               JdbcTemplate jdbcTemplate,
                               AppConfig appConfig) {
        this.vectorSearchRepository = vectorSearchRepository;
        this.bm25SearchRepository = bm25SearchRepository;
        this.rrf = rrf;
        this.rerankerClient = rerankerClient;
        this.embeddingClient = embeddingClient;
        this.jdbcTemplate = jdbcTemplate;
        this.appConfig = appConfig;
    }

    /**
     * Executes the hybrid retrieval pipeline:
     * Dense vector + Sparse BM25 -> RRF Fusion -> Cross-Encoder Rerank
     *
     * @param query natural-language query text
     * @param topK  maximum number of final chunks to return
     * @return scored chunks ordered by cross-encoder relevance, highest first
     */
    @Transactional(readOnly = true)
    public List<ScoredChunk> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        int vectorK = appConfig.retrieval() != null ? appConfig.retrieval().vectorTopK() : 20;
        int bm25K = appConfig.retrieval() != null ? appConfig.retrieval().bm25TopK() : 20;
        int rrfK = appConfig.retrieval() != null ? appConfig.retrieval().rrfTopK() : 15;

        log.debug("Starting hybrid search for '{}' (vectorK={}, bm25K={}, rrfK={}, topK={})",
                query, vectorK, bm25K, rrfK, topK);

        // 1. Dense vector search
        List<ScoredChunk> vectorChunks = Collections.emptyList();
        try {
            List<List<Float>> embeddings = embeddingClient.embed(List.of(query));
            if (!embeddings.isEmpty()) {
                float[] queryEmbedding = toFloatArray(embeddings.get(0));
                jdbcTemplate.execute("SET LOCAL ivfflat.probes = " + IVFFLAT_PROBES);
                vectorChunks = vectorSearchRepository.findNearest(queryEmbedding, vectorK);
            }
        } catch (Exception e) {
            log.warn("Vector search failed for query '{}': {}", query, e.getMessage());
        }

        // 2. Sparse BM25 full-text search
        List<ScoredChunk> bm25Chunks = Collections.emptyList();
        try {
            bm25Chunks = bm25SearchRepository.findTopKByBM25(query, bm25K);
        } catch (Exception e) {
            log.warn("BM25 search failed for query '{}': {}", query, e.getMessage());
        }

        log.debug("Retrieval candidates: {} vector, {} bm25", vectorChunks.size(), bm25Chunks.size());

        // If both searches returned empty, return early
        if (vectorChunks.isEmpty() && bm25Chunks.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Reciprocal Rank Fusion
        List<ScoredChunk> fusedChunks = rrf.fuseChunks(vectorChunks, bm25Chunks, rrfK);
        log.debug("RRF fused candidates: {}", fusedChunks.size());

        // 4. Cross-encoder reranking
        List<ScoredChunk> rerankedChunks = rerankerClient.rerank(query, fusedChunks, topK);
        log.debug("Reranking completed, returning {} chunks", rerankedChunks.size());

        return rerankedChunks;
    }

    private static float[] toFloatArray(List<Float> vector) {
        float[] arr = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            arr[i] = vector.get(i);
        }
        return arr;
    }
}
