package com.lexpilot.ingestion.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructureAwareChunkerTest {

    private final StructureAwareChunker chunker = new StructureAwareChunker();

    @Test
    void shouldChunkByParagraphs() {
        String input = "Paragraph 1 is here.\n\nParagraph 2 is here.";
        ChunkingOptions options = new ChunkingOptions(20, 0);

        List<String> chunks = chunker.chunk(input, options);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Paragraph 1 is here.\n\nParagraph 2 is here.");
    }

    @Test
    void shouldSplitLongParagraph() {
        // A single paragraph with 20 words
        String input = "Word1 word2 word3 word4 word5 word6 word7 word8 word9 word10 word11 word12 word13 word14 word15 word16 word17 word18 word19 word20";
        // chunkSize 13 -> approx 10 words per chunk
        ChunkingOptions options = new ChunkingOptions(13, 0);

        List<String> chunks = chunker.chunk(input, options);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isEqualTo("Word1 word2 word3 word4 word5 word6 word7 word8 word9 word10");
        assertThat(chunks.get(1)).isEqualTo("word11 word12 word13 word14 word15 word16 word17 word18 word19 word20");
    }

    @Test
    void shouldHandleEmptyOrNull() {
        ChunkingOptions options = new ChunkingOptions(10, 0);
        assertThat(chunker.chunk(null, options)).isEmpty();
        assertThat(chunker.chunk("   ", options)).isEmpty();
    }
}
