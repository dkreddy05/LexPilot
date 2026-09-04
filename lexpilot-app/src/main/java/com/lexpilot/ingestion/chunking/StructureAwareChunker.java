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
        // TODO: Implement structure-aware chunking using Tika XHTML metadata
        throw new UnsupportedOperationException("StructureAwareChunker not yet implemented");
    }
}
