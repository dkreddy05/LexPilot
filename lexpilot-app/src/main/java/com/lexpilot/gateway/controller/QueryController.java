package com.lexpilot.gateway.controller;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.dto.QueryRequest;
import com.lexpilot.common.dto.QueryResponse;
import com.lexpilot.common.dto.SearchResultsResponse;
import com.lexpilot.conversation.service.ConversationService;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.generation.prompt.PromptMessage;
import com.lexpilot.generation.service.GenerationService;
import com.lexpilot.common.audit.AuditService;
import com.lexpilot.retrieval.dto.ScoredChunk;
import com.lexpilot.retrieval.service.HybridSearchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class QueryController {

    private final HybridSearchService hybridSearchService;
    private final GenerationService generationService;
    private final ConversationService conversationService;
    private final AppConfig appConfig;
    private final AuditService auditService;

    public QueryController(HybridSearchService hybridSearchService,
                           GenerationService generationService,
                           ConversationService conversationService,
                           AppConfig appConfig,
                           AuditService auditService) {
        this.hybridSearchService = hybridSearchService;
        this.generationService = generationService;
        this.conversationService = conversationService;
        this.appConfig = appConfig;
        this.auditService = auditService;
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
     * Flow: resolve/create conversation → load history → embed query → vector search
     * → build prompt (with history) → LLM call → parse citations → persist messages
     * → return structured answer with sessionId.
     */
    @PostMapping("/query/answer")
    public ResponseEntity<QueryResponse> queryWithAnswer(@Valid @RequestBody QueryRequest request,
                                                         HttpServletRequest httpRequest) {
        int topK = appConfig.retrieval().vectorTopK();

        // 1. Resolve or create the conversation session
        UUID conversationId = conversationService.getOrCreateConversation(request.sessionId());

        // 2. Load conversation history (capped at maxHistoryTurns)
        List<PromptMessage> history = conversationService.getHistory(conversationId);

        // 3. Persist the user's message
        conversationService.appendUserMessage(conversationId, request.query());

        // 4. Retrieve relevant chunks
        List<ScoredChunk> chunks = hybridSearchService.search(request.query(), topK);

        // 5. Generate answer with citations and conversation context
        GeneratedAnswer generated = generationService.generate(request.query(), chunks, history);

        // 6. Persist the assistant's response
        conversationService.appendAssistantMessage(conversationId, generated.answer());

        // 6.5 Log the audit event asynchronously
        auditService.logQuery(request.query(), chunks, generated, httpRequest.getRemoteAddr());

        // 7. Map to API response
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
                generated.lowConfidence(),
                conversationId.toString()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Stream a grounded answer for the given query using Server-Sent Events (SSE).
     * <p>
     * Returns text chunks as they are generated by the LLM.
     */
    @GetMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamQueryAnswer(@RequestParam String query,
                                          @RequestParam(required = false) String sessionId,
                                          HttpServletRequest httpRequest) {
        int topK = appConfig.retrieval().vectorTopK();

        // 1. Resolve or create the conversation session
        UUID conversationId = conversationService.getOrCreateConversation(sessionId);

        // 2. Load conversation history
        List<PromptMessage> history = conversationService.getHistory(conversationId);

        // 3. Persist the user's message
        conversationService.appendUserMessage(conversationId, query);

        // 4. Retrieve relevant chunks
        List<ScoredChunk> chunks = hybridSearchService.search(query, topK);

        // 5. Stream answer chunks
        Flux<String> answerStream = generationService.stream(query, chunks, history);

        // Note: For full audit logging and conversation persistence on a stream,
        // we'd typically buffer the stream or use a doOnComplete hook.
        // For simplicity in this demo, we'll log the query intent here.
        auditService.logQuery(query, chunks, new GeneratedAnswer("[Streaming Response]", List.of(), false), httpRequest.getRemoteAddr());

        return answerStream
                .doOnComplete(() -> {
                    // In a production scenario, we'd accumulate the chunks here and persist
                    // the final assistant message to conversationService.
                    conversationService.appendAssistantMessage(conversationId, "[Streamed Response]");
                });
    }
}
