package com.lexpilot.ingestion.chunking;

import org.springframework.stereotype.Component;

import java.util.List;

@Component("structureAwareChunker")
public class StructureAwareChunker implements ChunkingStrategy<String> {

    @Override
    public List<String> chunk(String input, ChunkingOptions options) {
        // TODO: Implement structure-aware chunking using Tika XHTML metadata
        throw new UnsupportedOperationException("StructureAwareChunker not yet implemented");
    }
}
