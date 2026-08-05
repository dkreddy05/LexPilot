package com.lexpilot.ingestion.chunking;

import org.springframework.stereotype.Component;

import java.util.List;

@Component("fixedSizeChunker")
public class FixedSizeChunker implements ChunkingStrategy<String> {

    @Override
    public List<String> chunk(String input, ChunkingOptions options) {
        // TODO: Implement fixed-size sliding window chunking logic
        throw new UnsupportedOperationException("FixedSizeChunker not yet implemented");
    }
}
