package com.lexpilot.ingestion.repository;

import com.lexpilot.ingestion.entity.DocumentChunkEntity;
import com.lexpilot.ingestion.entity.DocumentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the derived query that replaced the
 * findAll().stream().filter() heap scan (REC-1).
 * <p>
 * Verifies two properties:
 * <ol>
 *   <li>Query returns only chunks for the target document (isolation)</li>
 *   <li>Results are ordered by chunk_index ascending (ordering)</li>
 * </ol>
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentChunkRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("lexpilot_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private UUID docAId;
    private UUID docBId;

    @BeforeEach
    void setUp() {
        chunkRepository.deleteAll();
        documentRepository.deleteAll();

        // Create two distinct documents
        DocumentEntity docA = new DocumentEntity("doc-a.pdf", "application/pdf");
        docA = documentRepository.save(docA);
        docAId = docA.getId();

        DocumentEntity docB = new DocumentEntity("doc-b.pdf", "application/pdf");
        docB = documentRepository.save(docB);
        docBId = docB.getId();

        // Insert chunks for doc A in reverse order to verify DB-side ordering
        chunkRepository.save(new DocumentChunkEntity(docAId, 2, "Doc A chunk 2"));
        chunkRepository.save(new DocumentChunkEntity(docAId, 0, "Doc A chunk 0"));
        chunkRepository.save(new DocumentChunkEntity(docAId, 1, "Doc A chunk 1"));

        // Insert chunks for doc B
        chunkRepository.save(new DocumentChunkEntity(docBId, 0, "Doc B chunk 0"));
        chunkRepository.save(new DocumentChunkEntity(docBId, 1, "Doc B chunk 1"));
    }

    @Test
    void findByDocumentId_shouldReturnOnlyChunksForTargetDocument() {
        List<DocumentChunkEntity> result = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(docAId);

        assertThat(result)
                .hasSize(3)
                .allSatisfy(chunk -> assertThat(chunk.getDocumentId()).isEqualTo(docAId));
    }

    @Test
    void findByDocumentId_shouldNotReturnChunksFromOtherDocuments() {
        List<DocumentChunkEntity> result = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(docAId);

        assertThat(result)
                .extracting(DocumentChunkEntity::getContent)
                .noneMatch(content -> content.startsWith("Doc B"));
    }

    @Test
    void findByDocumentId_shouldReturnChunksOrderedByChunkIndex() {
        List<DocumentChunkEntity> result = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(docAId);

        assertThat(result)
                .extracting(DocumentChunkEntity::getChunkIndex)
                .containsExactly(0, 1, 2);
    }

    @Test
    void findByDocumentId_shouldReturnEmptyForNonExistentDocument() {
        UUID nonExistentId = UUID.randomUUID();

        List<DocumentChunkEntity> result = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(nonExistentId);

        assertThat(result).isEmpty();
    }
}
