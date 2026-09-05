package com.lexpilot.ingestion.repository;

import com.lexpilot.ingestion.entity.DocumentEntity;
import com.lexpilot.ingestion.entity.DocumentStatus;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the conditional document status transition SQL (REC-3).
 * <p>
 * Verifies that {@code updateStatusIfNotFailed} does NOT overwrite a FAILED
 * status, preventing the race condition where a trailing successful chunk
 * could flip a document back to READY after an earlier chunk marked it FAILED.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DocumentStatusTransitionTest {

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
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
    }

    @Test
    void updateStatusIfNotFailed_shouldNotOverwriteFailedStatus() {
        // Arrange: document is already FAILED
        DocumentEntity doc = new DocumentEntity("failed-doc.pdf", "application/pdf");
        doc.setStatus(DocumentStatus.FAILED);
        doc.setErrorMessage("Embedding failed for chunk xyz");
        doc = documentRepository.save(doc);
        UUID docId = doc.getId();

        // Act: attempt to transition to READY (simulating trailing successful chunk)
        int updated = documentRepository.updateStatusIfNotFailed(docId, DocumentStatus.READY);

        // Assert: update was skipped — document stays FAILED
        assertThat(updated).isZero();

        DocumentEntity reloaded = documentRepository.findById(docId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.FAILED);
        assertThat(reloaded.getErrorMessage()).isEqualTo("Embedding failed for chunk xyz");
    }

    @Test
    void updateStatusIfNotFailed_shouldTransitionFromEmbeddingToReady() {
        // Arrange: document is in EMBEDDING state (happy path)
        DocumentEntity doc = new DocumentEntity("good-doc.pdf", "application/pdf");
        doc.setStatus(DocumentStatus.EMBEDDING);
        doc = documentRepository.save(doc);
        UUID docId = doc.getId();

        // Act: transition to READY
        int updated = documentRepository.updateStatusIfNotFailed(docId, DocumentStatus.READY);

        // Assert: update succeeded
        assertThat(updated).isEqualTo(1);

        DocumentEntity reloaded = documentRepository.findById(docId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.READY);
    }

    @Test
    void updateStatusIfNotFailed_shouldReturnZeroForNonExistentDocument() {
        UUID nonExistent = UUID.randomUUID();

        int updated = documentRepository.updateStatusIfNotFailed(nonExistent, DocumentStatus.READY);

        assertThat(updated).isZero();
    }
}
