package com.lexpilot.ingestion.chunking;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits text into fixed-size chunks with overlap, splitting on whitespace
 * boundaries so we never cut mid-word.
 * <p>
 * Token estimation uses an approximate heuristic: {@code wordCount × 1.3}.
 * This avoids pulling in a real tokenizer at the scaffold stage while
 * staying close enough for embedding model inputs.
 */
@Component("fixedSizeChunker")
public class FixedSizeChunker implements ChunkingStrategy<String> {

    /**
     * Approximate multiplier to convert word count to token estimate.
     * Most English words map to ~1.3 BPE tokens on average.
     */
    private static final double TOKENS_PER_WORD = 1.3;

    @Override
    public List<String> chunk(String input, ChunkingOptions options) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        String[] words = input.split("\\s+");
        int chunkSizeTokens = options.chunkSize();
        int overlapTokens = options.chunkOverlap();

        // Convert token targets to word counts
        int chunkSizeWords = Math.max(1, (int) (chunkSizeTokens / TOKENS_PER_WORD));
        int overlapWords = Math.max(0, (int) (overlapTokens / TOKENS_PER_WORD));
        int stepWords = Math.max(1, chunkSizeWords - overlapWords);

        List<String> chunks = new ArrayList<>();
        int totalWords = words.length;

        for (int start = 0; start < totalWords; start += stepWords) {
            int end = Math.min(start + chunkSizeWords, totalWords);
            StringBuilder sb = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) {
                    sb.append(' ');
                }
                sb.append(words[i]);
            }
            chunks.add(sb.toString());

            // If we've reached the end, no need to continue sliding
            if (end == totalWords) {
                break;
            }
        }

        return chunks;
    }
}
