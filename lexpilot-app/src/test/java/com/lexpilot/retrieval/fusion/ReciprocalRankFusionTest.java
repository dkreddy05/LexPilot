package com.lexpilot.retrieval.fusion;

import com.lexpilot.retrieval.dto.ScoredChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReciprocalRankFusionTest {

    private ReciprocalRankFusion rrf;

    @BeforeEach
    void setUp() {
        rrf = new ReciprocalRankFusion();
    }

    @Test
    void chunkPresentInBothLists_ranksHigherThanChunkInSingleList() {
        UUID docId = UUID.randomUUID();
        UUID sharedChunkId = UUID.randomUUID();
        UUID vectorOnlyChunkId = UUID.randomUUID();
        UUID bm25OnlyChunkId = UUID.randomUUID();

        ScoredChunk sharedChunk = new ScoredChunk(sharedChunkId, docId, "Shared content", 0.9, "doc.pdf");
        ScoredChunk vectorChunk = new ScoredChunk(vectorOnlyChunkId, docId, "Vector content", 0.85, "doc.pdf");
        ScoredChunk bm25Chunk = new ScoredChunk(bm25OnlyChunkId, docId, "BM25 content", 0.8, "doc.pdf");

        List<ScoredChunk> vectorResults = List.of(sharedChunk, vectorChunk);
        List<ScoredChunk> bm25Results = List.of(sharedChunk, bm25Chunk);

        List<ScoredChunk> fused = rrf.fuseChunks(vectorResults, bm25Results, 10);

        assertThat(fused).isNotEmpty();
        assertThat(fused.get(0).chunkId()).isEqualTo(sharedChunkId);
        // Shared item has rank 1 in both: 1/(60+1) + 1/(60+1) = 2/61 ≈ 0.03278
        assertThat(fused.get(0).score()).isGreaterThan(fused.get(1).score());
    }

    @Test
    void emptyInputs_returnEmptyList() {
        List<ScoredChunk> fused = rrf.fuseChunks(List.of(), List.of(), 5);
        assertThat(fused).isEmpty();
    }

    @Test
    void topNLimitsResults() {
        UUID docId = UUID.randomUUID();
        List<ScoredChunk> vectorResults = List.of(
                new ScoredChunk(UUID.randomUUID(), docId, "1", 0.9, "doc.pdf"),
                new ScoredChunk(UUID.randomUUID(), docId, "2", 0.8, "doc.pdf"),
                new ScoredChunk(UUID.randomUUID(), docId, "3", 0.7, "doc.pdf")
        );

        List<ScoredChunk> fused = rrf.fuseChunks(vectorResults, List.of(), 2);
        assertThat(fused).hasSize(2);
    }
}
