package com.lexpilot.retrieval.service;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.ingestion.service.EmbeddingServiceClient;
import com.lexpilot.retrieval.client.RerankerClient;
import com.lexpilot.retrieval.dto.ScoredChunk;
import com.lexpilot.retrieval.fusion.ReciprocalRankFusion;
import com.lexpilot.retrieval.repository.BM25SearchRepository;
import com.lexpilot.retrieval.repository.VectorSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private VectorSearchRepository vectorSearchRepository;

    @Mock
    private BM25SearchRepository bm25SearchRepository;

    @Mock
    private ReciprocalRankFusion rrf;

    @Mock
    private RerankerClient rerankerClient;

    @Mock
    private EmbeddingServiceClient embeddingClient;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfig.RetrievalConfig retrievalConfig;

    private HybridSearchService hybridSearchService;

    @BeforeEach
    void setUp() {
        lenient().when(appConfig.retrieval()).thenReturn(retrievalConfig);
        lenient().when(retrievalConfig.vectorTopK()).thenReturn(20);
        lenient().when(retrievalConfig.bm25TopK()).thenReturn(20);
        lenient().when(retrievalConfig.rrfTopK()).thenReturn(15);

        hybridSearchService = new HybridSearchService(
                vectorSearchRepository,
                bm25SearchRepository,
                rrf,
                rerankerClient,
                embeddingClient,
                jdbcTemplate,
                appConfig
        );
    }

    @Test
    void search_orchestratesVectorBm25RrfAndReranking() {
        String query = "consumer protection limitation";
        UUID docId = UUID.randomUUID();
        ScoredChunk chunk1 = new ScoredChunk(UUID.randomUUID(), docId, "text 1", 0.9, "doc.pdf");
        ScoredChunk chunk2 = new ScoredChunk(UUID.randomUUID(), docId, "text 2", 0.8, "doc.pdf");

        when(embeddingClient.embed(List.of(query))).thenReturn(List.of(List.of(0.1f, 0.2f, 0.3f)));
        when(vectorSearchRepository.findNearest(any(float[].class), eq(20))).thenReturn(List.of(chunk1));
        when(bm25SearchRepository.findTopKByBM25(eq(query), eq(20))).thenReturn(List.of(chunk2));
        when(rrf.fuseChunks(anyList(), anyList(), eq(15))).thenReturn(List.of(chunk1, chunk2));
        when(rerankerClient.rerank(eq(query), anyList(), eq(5))).thenReturn(List.of(chunk2, chunk1));

        List<ScoredChunk> results = hybridSearchService.search(query, 5);

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo(chunk2);
        verify(embeddingClient).embed(List.of(query));
        verify(bm25SearchRepository).findTopKByBM25(query, 20);
        verify(rrf).fuseChunks(anyList(), anyList(), eq(15));
        verify(rerankerClient).rerank(query, List.of(chunk1, chunk2), 5);
    }

    @Test
    void emptyQuery_returnsEmptyListImmediately() {
        List<ScoredChunk> results = hybridSearchService.search("   ", 10);
        assertThat(results).isEmpty();
        verifyNoInteractions(embeddingClient);
        verifyNoInteractions(bm25SearchRepository);
    }
}
