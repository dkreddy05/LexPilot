package com.lexpilot.retrieval.fusion;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReciprocalRankFusion {

    private static final int K = 60;

    public List<String> fuse(List<String> vectorResults, List<String> bm25Results, int topN) {
        // TODO: Implement Reciprocal Rank Fusion
        throw new UnsupportedOperationException("ReciprocalRankFusion.fuse() not yet implemented");
    }
}
