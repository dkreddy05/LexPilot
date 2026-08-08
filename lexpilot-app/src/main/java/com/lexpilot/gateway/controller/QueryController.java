package com.lexpilot.gateway.controller;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.dto.QueryRequest;
import com.lexpilot.common.dto.QueryResponse;
import com.lexpilot.common.dto.SearchResultsResponse;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.generation.service.GenerationService;
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
    private final GenerationService generationService;
    private final AppConfig appConfig;

    public QueryController(HybridSearchService hybridSearchService,
                           GenerationService generationService,
                           AppConfig appConfig) {
        this.hybridSearchService = hybridSearchService;
        this.generationService = generationService;
        this.appConfig = appConfig;
    }

    /**
     * Retrieve the top-K document chunks most relevant to the query.
     * <p>
     * This endpoint returns raw scored chunks (retrieval only) — useful for
     * debugging retrieval quality independently of generation.
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

    /**
     * Generate a grounded answer from retrieved context, with citations.
     * <p>
     * Flow: embed query → vector search → build prompt → LLM call →
     * parse citations → return structured answer.
     */
    @PostMapping("/query/answer")
    public ResponseEntity<QueryResponse> queryWithAnswer(@Valid @RequestBody QueryRequest request) {
        int topK = appConfig.retrieval().vectorTopK();

        // 1. Retrieve relevant chunks
        List<ScoredChunk> chunks = hybridSearchService.search(request.query(), topK);

        // 2. Generate answer with citations
        GeneratedAnswer generated = generationService.generate(request.query(), chunks);

        // 3. Map to API response
        List<QueryResponse.CitationDto> citationDtos = generated.citations().stream()
                .map(c -> new QueryResponse.CitationDto(
                        c.marker(),
                        c.chunkId().toString(),
                        c.documentId().toString(),
                        c.sourceLabel()))
                .toList();

        QueryResponse response = new QueryResponse(
                generated.answer(),
                citationDtos,
                generated.lowConfidence()
        );

        return ResponseEntity.ok(response);
    }
}
