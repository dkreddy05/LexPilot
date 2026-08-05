package com.lexpilot.retrieval.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BM25SearchRepository {

    public BM25SearchRepository() {
    }

    public List<String> findTopKByBM25(String query, int topK, String domain) {
        // TODO: Execute Postgres tsvector full-text search query
        throw new UnsupportedOperationException("BM25SearchRepository not yet implemented");
    }
}
