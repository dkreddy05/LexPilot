package com.lexpilot.retrieval.fusion;

import com.lexpilot.retrieval.dto.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Implements Reciprocal Rank Fusion (RRF) to combine ranked search results
 * from heterogeneous retrieval methods (e.g. dense vector search and sparse BM25).
 * <p>
 * Standard formula: RRF_score(d) = \sum_{m} 1 / (k + rank_m(d))
 */
@Component
public class ReciprocalRankFusion {

    /**
     * Standard smoothing constant (Cormack et al., SIGIR 2009).
     * k=60 balances out top-rank biases from individual searchers.
     */
    public static final int DEFAULT_K = 60;

    /**
     * Fuse dense vector and sparse keyword results into a single ranked list.
     *
     * @param vectorResults scored chunks from vector search (ordered by similarity descending)
     * @param bm25Results   scored chunks from BM25 text search (ordered by text rank descending)
     * @param topN          maximum candidates to return
     * @return unified list of scored chunks ordered by composite RRF score descending
     */
    public List<ScoredChunk> fuseChunks(List<ScoredChunk> vectorResults, List<ScoredChunk> bm25Results, int topN) {
        return fuseChunks(vectorResults, bm25Results, topN, DEFAULT_K);
    }

    public List<ScoredChunk> fuseChunks(List<ScoredChunk> vectorResults, List<ScoredChunk> bm25Results, int topN, int k) {
        if (vectorResults == null) vectorResults = Collections.emptyList();
        if (bm25Results == null) bm25Results = Collections.emptyList();

        Map<UUID, Double> rrfScores = new HashMap<>();
        Map<UUID, ScoredChunk> chunkMap = new HashMap<>();

        // 1. Process vector results (1-based rank)
        for (int i = 0; i < vectorResults.size(); i++) {
            ScoredChunk chunk = vectorResults.get(i);
            int rank = i + 1;
            rrfScores.merge(chunk.chunkId(), 1.0 / (k + rank), Double::sum);
            chunkMap.putIfAbsent(chunk.chunkId(), chunk);
        }

        // 2. Process BM25 results (1-based rank)
        for (int i = 0; i < bm25Results.size(); i++) {
            ScoredChunk chunk = bm25Results.get(i);
            int rank = i + 1;
            rrfScores.merge(chunk.chunkId(), 1.0 / (k + rank), Double::sum);
            chunkMap.putIfAbsent(chunk.chunkId(), chunk);
        }

        // 3. Sort descending by combined RRF score
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> {
                    ScoredChunk orig = chunkMap.get(entry.getKey());
                    return new ScoredChunk(
                            orig.chunkId(),
                            orig.documentId(),
                            orig.content(),
                            entry.getValue(),
                            orig.sourceLabel()
                    );
                })
                .toList();
    }

    /**
     * Overload for String identifiers (for backward compatibility).
     */
    public List<String> fuse(List<String> vectorResults, List<String> bm25Results, int topN) {
        if (vectorResults == null) vectorResults = Collections.emptyList();
        if (bm25Results == null) bm25Results = Collections.emptyList();

        Map<String, Double> scores = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            String item = vectorResults.get(i);
            scores.merge(item, 1.0 / (DEFAULT_K + i + 1), Double::sum);
        }

        for (int i = 0; i < bm25Results.size(); i++) {
            String item = bm25Results.get(i);
            scores.merge(item, 1.0 / (DEFAULT_K + i + 1), Double::sum);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }
}
