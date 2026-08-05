package com.lexpilot.retrieval.service;

import com.lexpilot.common.config.AppConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HybridSearchService {

    private final AppConfig appConfig;

    public HybridSearchService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public List<String> search(String query, String domain) {
        // TODO: Implement hybrid search pipeline (Vector + BM25 + RRF + Rerank)
        throw new UnsupportedOperationException("HybridSearchService.search() not yet implemented");
    }
}
