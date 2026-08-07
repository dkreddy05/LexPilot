package com.lexpilot.retrieval;

import com.lexpilot.retrieval.dto.ScoredChunk;
import com.lexpilot.retrieval.repository.VectorSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the pgvector cosine-similarity search.
 * <p>
 * Uses precomputed embedding vectors (not the live embedding-service) to keep
 * the test deterministic and fast. Three chunks are seeded with carefully chosen
 * vectors so we can assert ranking order and score properties.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class VectorSearchIntegrationTest {

    // ---- Testcontainers ----

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("lexpilot")
            .withUsername("lexpilot")
            .withPassword("lexpilot_test")
            .withInitScript("db/init.sql");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("lexpilot.embedding-service.base-url", () -> "http://localhost:19999");
        registry.add("lexpilot.llm.api-key", () -> "test-key");
        registry.add("lexpilot.api-key", () -> "test-api-key");
        registry.add("lexpilot.ingestion.upload-dir", () -> "./test-uploads");
        registry.add("lexpilot.ingestion.max-file-size-mb", () -> "20");
    }

    // ---- Dependencies ----

    @Autowired
    private VectorSearchRepository vectorSearchRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---- Precomputed test vectors (384-dim, unit-normalised) ----
    // We use sparse-ish vectors: set a few dimensions to non-zero values.
    // This lets us control cosine similarity precisely.

    private static final UUID DOC_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHUNK_REFUND  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHUNK_WARRANTY = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CHUNK_TELECOM  = UUID.fromString("33333333-3333-3333-3333-333333333333");

    /**
     * Build a 384-dim vector that is all zeros except for specified dimensions.
     * The result is L2-normalised so cosine distance is meaningful.
     */
    private static float[] sparseVector(int... hotDims) {
        float[] v = new float[384];
        for (int d : hotDims) {
            v[d] = 1.0f;
        }
        // L2-normalise
        float norm = 0f;
        for (float x : v) norm += x * x;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
        return v;
    }

    /** Convert float[] to pgvector literal string [0.1,0.2,...] */
    private static String toPgvector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    // Vectors:
    //  refund  chunk  → hot dims {0, 1, 2}
    //  warranty chunk → hot dims {0, 1, 3}   (overlaps partially with refund)
    //  telecom  chunk → hot dims {10, 11, 12} (no overlap)
    //  query vector   → hot dims {0, 1, 2}   (identical to refund)
    private final float[] refundVector   = sparseVector(0, 1, 2);
    private final float[] warrantyVector = sparseVector(0, 1, 3);
    private final float[] telecomVector  = sparseVector(10, 11, 12);
    private final float[] queryVector    = sparseVector(0, 1, 2);

    @BeforeEach
    void seedData() {
        // Clean slate
        jdbcTemplate.update("DELETE FROM document_chunks");
        jdbcTemplate.update("DELETE FROM documents");

        // Insert parent document
        jdbcTemplate.update(
                "INSERT INTO documents (id, filename, content_type, status) VALUES (?, ?, ?, ?)",
                DOC_ID, "test-corpus.pdf", "application/pdf", "READY");

        // Insert chunks with precomputed embeddings
        insertChunk(CHUNK_REFUND, 0, "How to get a refund when a seller refuses", refundVector);
        insertChunk(CHUNK_WARRANTY, 1, "Filing a warranty claim under consumer protection", warrantyVector);
        insertChunk(CHUNK_TELECOM, 2, "Telecom service provider complaint resolution process", telecomVector);

        // With only 3 rows and ivfflat lists=100, default probes=1 misses most
        // neighbors. Set probes high enough to scan all lists in this test.
        jdbcTemplate.execute("SET ivfflat.probes = 100");
    }

    private void insertChunk(UUID chunkId, int index, String content, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO document_chunks (id, document_id, chunk_index, content, embedding) "
                        + "VALUES (?, ?, ?, ?, CAST(? AS vector))",
                chunkId, DOC_ID, index, content, toPgvector(embedding));
    }

    // ---- Tests ----

    @Test
    void findNearest_shouldReturnExactMatchAsTopResult() {
        List<ScoredChunk> results = vectorSearchRepository.findNearest(queryVector, 3);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).chunkId()).isEqualTo(CHUNK_REFUND);
        // Cosine similarity of identical normalised vectors should be ~1.0
        assertThat(results.get(0).score()).isGreaterThan(0.99);
    }

    @Test
    void findNearest_scoresAreDescending() {
        List<ScoredChunk> results = vectorSearchRepository.findNearest(queryVector, 3);

        assertThat(results).hasSize(3);
        for (int i = 0; i < results.size() - 1; i++) {
            assertThat(results.get(i).score())
                    .as("score[%d] >= score[%d]", i, i + 1)
                    .isGreaterThanOrEqualTo(results.get(i + 1).score());
        }
    }

    @Test
    void findNearest_partialOverlapRanksAboveNoOverlap() {
        List<ScoredChunk> results = vectorSearchRepository.findNearest(queryVector, 3);

        assertThat(results).hasSize(3);
        // warranty (partial overlap) should rank above telecom (no overlap)
        ScoredChunk warranty = results.stream()
                .filter(sc -> sc.chunkId().equals(CHUNK_WARRANTY))
                .findFirst().orElseThrow();
        ScoredChunk telecom = results.stream()
                .filter(sc -> sc.chunkId().equals(CHUNK_TELECOM))
                .findFirst().orElseThrow();

        assertThat(warranty.score()).isGreaterThan(telecom.score());
    }

    @Test
    void findNearest_noOverlapChunkHasLowScore() {
        List<ScoredChunk> results = vectorSearchRepository.findNearest(queryVector, 3);

        ScoredChunk telecom = results.stream()
                .filter(sc -> sc.chunkId().equals(CHUNK_TELECOM))
                .findFirst().orElseThrow();

        // Orthogonal vectors → cosine similarity ≈ 0
        assertThat(telecom.score()).isLessThan(0.01);
    }

    @Test
    void findNearest_topKLimitsResults() {
        List<ScoredChunk> results = vectorSearchRepository.findNearest(queryVector, 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunkId()).isEqualTo(CHUNK_REFUND);
    }

    @Test
    void findNearest_returnsCorrectDocumentId() {
        List<ScoredChunk> results = vectorSearchRepository.findNearest(queryVector, 3);

        results.forEach(sc ->
                assertThat(sc.documentId())
                        .as("All chunks belong to the seeded document")
                        .isEqualTo(DOC_ID));
    }

    @Test
    void findNearest_returnsContentText() {
        List<ScoredChunk> results = vectorSearchRepository.findNearest(queryVector, 1);

        assertThat(results.get(0).content()).contains("refund");
    }
}
