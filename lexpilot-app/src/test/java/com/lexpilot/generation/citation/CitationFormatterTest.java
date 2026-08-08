package com.lexpilot.generation.citation;

import com.lexpilot.generation.dto.Citation;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RegexCitationFormatter}.
 * <p>
 * All tests use precomputed data — no LLM calls, fully deterministic.
 */
class CitationFormatterTest {

    private final RegexCitationFormatter formatter = new RegexCitationFormatter();

    // ---- Test fixtures ----

    private static final UUID DOC_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CHUNK_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHUNK_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CHUNK_3_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final List<ScoredChunk> threeChunks = List.of(
            new ScoredChunk(CHUNK_1_ID, DOC_ID, "Refund policy content", 0.95, "refund-guide.pdf"),
            new ScoredChunk(CHUNK_2_ID, DOC_ID, "Warranty claim content", 0.80, "warranty-faq.pdf"),
            new ScoredChunk(CHUNK_3_ID, DOC_ID, "Telecom complaint content", 0.60, "telecom-handbook.pdf")
    );

    // ---- Tests ----

    @Test
    void validMarkers_shouldProduceCorrectCitations() {
        String rawAnswer = "According to [1], you can get a refund. See also [2] for warranty info.";

        GeneratedAnswer result = formatter.format(rawAnswer, threeChunks);

        assertThat(result.answer()).isEqualTo(rawAnswer);
        assertThat(result.citations()).hasSize(2);

        Citation c1 = result.citations().get(0);
        assertThat(c1.marker()).isEqualTo(1);
        assertThat(c1.chunkId()).isEqualTo(CHUNK_1_ID);
        assertThat(c1.sourceLabel()).isEqualTo("refund-guide.pdf");

        Citation c2 = result.citations().get(1);
        assertThat(c2.marker()).isEqualTo(2);
        assertThat(c2.chunkId()).isEqualTo(CHUNK_2_ID);
        assertThat(c2.sourceLabel()).isEqualTo("warranty-faq.pdf");
    }

    @Test
    void hallucinatedMarker_shouldBeDropped() {
        String rawAnswer = "See [1] and [99] for details.";

        GeneratedAnswer result = formatter.format(rawAnswer, threeChunks);

        // [99] is out of bounds → dropped
        assertThat(result.citations()).hasSize(1);
        assertThat(result.citations().get(0).marker()).isEqualTo(1);
    }

    @Test
    void zeroMarker_shouldBeDropped() {
        String rawAnswer = "See [0] for details.";

        GeneratedAnswer result = formatter.format(rawAnswer, threeChunks);

        // [0] maps to index -1 → out of bounds → dropped
        assertThat(result.citations()).isEmpty();
    }

    @Test
    void duplicateMarkers_shouldBeDeduplicated() {
        String rawAnswer = "First [1] says this. Later [1] confirms it. Also [2].";

        GeneratedAnswer result = formatter.format(rawAnswer, threeChunks);

        // [1] appears twice but should produce only one citation
        assertThat(result.citations()).hasSize(2);
        assertThat(result.citations().get(0).marker()).isEqualTo(1);
        assertThat(result.citations().get(1).marker()).isEqualTo(2);
    }

    @Test
    void noMarkers_shouldProduceEmptyCitations() {
        String rawAnswer = "I don't have enough information to answer this question.";

        GeneratedAnswer result = formatter.format(rawAnswer, threeChunks);

        assertThat(result.citations()).isEmpty();
        assertThat(result.answer()).isEqualTo(rawAnswer);
    }

    @Test
    void allThreeMarkers_shouldMapCorrectly() {
        String rawAnswer = "See [1], [2], and [3].";

        GeneratedAnswer result = formatter.format(rawAnswer, threeChunks);

        assertThat(result.citations()).hasSize(3);
        assertThat(result.citations().get(0).chunkId()).isEqualTo(CHUNK_1_ID);
        assertThat(result.citations().get(1).chunkId()).isEqualTo(CHUNK_2_ID);
        assertThat(result.citations().get(2).chunkId()).isEqualTo(CHUNK_3_ID);
    }

    @Test
    void lowConfidence_shouldBeFalseByDefault() {
        String rawAnswer = "See [1].";

        GeneratedAnswer result = formatter.format(rawAnswer, threeChunks);

        assertThat(result.lowConfidence()).isFalse();
    }

    @Test
    void emptyChunkList_allMarkersShouldBeDropped() {
        String rawAnswer = "See [1] and [2].";

        GeneratedAnswer result = formatter.format(rawAnswer, List.of());

        assertThat(result.citations()).isEmpty();
    }
}
