package com.lexpilot.retrieval.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VectorSearchRepository {

    public VectorSearchRepository() {
    }

    public List<String> findTopKByVector(float[] queryEmbedding, int topK, String domain) {
        // TODO: Execute native pgvector cosine similarity query
        throw new UnsupportedOperationException("VectorSearchRepository not yet implemented");
    }
}
