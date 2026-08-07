package com.lexpilot.retrieval.service;

import com.lexpilot.ingestion.service.EmbeddingServiceClient;
import com.lexpilot.retrieval.dto.ScoredChunk;
import com.lexpilot.retrieval.repository.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Search service that orchestrates query embedding and vector retrieval.
 * <p>
 * Currently implements pure vector search. The class name is kept stable so
 * BM25 and Reciprocal Rank Fusion can be wired in later without renaming.
 */
@Service
public class HybridSearchService {

    private static final Logger log = LoggerFactory.getLogger(HybridSearchService.class);

    private final VectorSearchRepository vectorSearchRepository;
    private final EmbeddingServiceClient embeddingClient;

    public HybridSearchService(VectorSearchRepository vectorSearchRepository,
                               EmbeddingServiceClient embeddingClient) {
        this.vectorSearchRepository = vectorSearchRepository;
        this.embeddingClient = embeddingClient;
    }

    /**
     * Embed the user's query and retrieve the top-K nearest document chunks.
     *
     * @param query natural-language query text
     * @param topK  maximum number of chunks to return
     * @return scored chunks ordered by cosine similarity, highest first
     */
    public List<ScoredChunk> search(String query, int topK) {
        log.debug("Embedding query text ({} chars) for vector search", query.length());

        // 1. Embed the query through the same service/model used by ingestion
        List<List<Float>> embeddings = embeddingClient.embed(List.of(query));
        float[] queryEmbedding = toFloatArray(embeddings.get(0));

        // 2. Vector nearest-neighbour search
        List<ScoredChunk> results = vectorSearchRepository.findNearest(queryEmbedding, topK);

        log.debug("Vector search returned {} chunks (topK={})", results.size(), topK);
        return results;
    }

    /**
     * Convert a {@code List<Float>} (from the embedding service response) to
     * a primitive {@code float[]} (expected by the repository).
     */
    private static float[] toFloatArray(List<Float> vector) {
        float[] arr = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            arr[i] = vector.get(i);
        }
        return arr;
    }
}
