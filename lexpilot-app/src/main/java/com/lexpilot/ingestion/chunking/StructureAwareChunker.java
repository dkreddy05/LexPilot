package com.lexpilot.ingestion.chunking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Placeholder for structure-aware chunking that uses Tika XHTML metadata
 * to split text at paragraph and heading boundaries.
 * <p>
 * This bean is <b>not</b> loaded by default — it only activates when
 * {@code lexpilot.chunking.strategy=structure-aware} is set explicitly.
 * When inactive, {@link FixedSizeChunker} is used instead.
 */
@Component("structureAwareChunker")
@ConditionalOnProperty(name = "lexpilot.chunking.strategy", havingValue = "structure-aware")
public class StructureAwareChunker implements ChunkingStrategy<String> {

    @Override
    public List<String> chunk(String input, ChunkingOptions options) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        List<String> chunks = new java.util.ArrayList<>();
        String[] paragraphs = input.split("\\n\\s*\\n");
        int chunkSize = options.chunkSize();
        int overlapSize = options.chunkOverlap();
        // Approximate words from tokens
        int maxWords = Math.max(1, (int) (chunkSize / 1.3));
        int overlapWords = Math.max(0, (int) (overlapSize / 1.3));

        StringBuilder currentChunk = new StringBuilder();
        int currentWordCount = 0;

        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) continue;
            
            String[] words = paragraph.split("\\s+");
            
            // If the paragraph is extremely long, break it up
            if (words.length > maxWords) {
                if (currentWordCount > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                    currentWordCount = 0;
                }
                
                int stepWords = Math.max(1, maxWords - overlapWords);
                for (int start = 0; start < words.length; start += stepWords) {
                    int end = Math.min(start + maxWords, words.length);
                    StringBuilder sb = new StringBuilder();
                    for (int i = start; i < end; i++) {
                        if (i > start) sb.append(" ");
                        sb.append(words[i]);
                    }
                    chunks.add(sb.toString().trim());
                    if (end == words.length) break;
                }
            } else {
                if (currentWordCount + words.length > maxWords && currentWordCount > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk.setLength(0);
                    currentWordCount = 0;
                }
                
                if (currentChunk.length() > 0) currentChunk.append("\n\n");
                currentChunk.append(paragraph.trim());
                currentWordCount += words.length;
            }
        }
        
        if (currentWordCount > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
