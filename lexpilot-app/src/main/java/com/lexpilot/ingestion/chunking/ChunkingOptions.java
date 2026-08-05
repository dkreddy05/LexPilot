package com.lexpilot.ingestion.chunking;

public record ChunkingOptions(int chunkSize, int chunkOverlap) {
    public static ChunkingOptions defaults() {
        return new ChunkingOptions(512, 64);
    }
}
