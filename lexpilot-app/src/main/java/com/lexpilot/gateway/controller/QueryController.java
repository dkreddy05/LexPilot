package com.lexpilot.gateway.controller;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.dto.QueryRequest;
import com.lexpilot.common.dto.SearchResultsResponse;
import com.lexpilot.retrieval.dto.ScoredChunk;
import com.lexpilot.retrieval.service.HybridSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class QueryController {

    private final HybridSearchService hybridSearchService;
    private final AppConfig appConfig;

    public QueryController(HybridSearchService hybridSearchService, AppConfig appConfig) {
        this.hybridSearchService = hybridSearchService;
        this.appConfig = appConfig;
    }

    /**
     * Retrieve the top-K document chunks most relevant to the query.
     * <p>
     * This endpoint currently returns raw scored chunks (retrieval only).
     * It will be extended to include LLM-generated answers once the
     * generation slice is implemented.
     */
    @PostMapping("/query")
    public ResponseEntity<SearchResultsResponse> query(@Valid @RequestBody QueryRequest request) {
        int topK = appConfig.retrieval().vectorTopK();

        List<ScoredChunk> chunks = hybridSearchService.search(request.query(), topK);

        List<SearchResultsResponse.Result> results = chunks.stream()
                .map(sc -> new SearchResultsResponse.Result(
                        sc.chunkId().toString(),
                        sc.documentId().toString(),
                        sc.content(),
                        sc.score()))
                .toList();

        return ResponseEntity.ok(new SearchResultsResponse(results));
    }
}
