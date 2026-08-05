package com.lexpilot.ingestion.chunking;

import java.util.List;

public interface ChunkingStrategy<T> {
    List<String> chunk(T input, ChunkingOptions options);
}
