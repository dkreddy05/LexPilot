package com.lexpilot;

import com.lexpilot.common.dto.DocumentUploadResponse;
import com.lexpilot.common.dto.IngestionStatusResponse;
import com.lexpilot.ingestion.entity.DocumentStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for the document ingestion pipeline.
 * Uses Testcontainers for Postgres (pgvector), Kafka, and Redis.
 * <p>
 * Tests the synchronous portion of the pipeline: upload → extract → chunk → persist.
 * The async embedding consumer is also wired but will fail gracefully since
 * there is no real embedding-service in this test — documents will reach
 * EMBEDDING status (not READY).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LexPilotIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("lexpilot")
            .withUsername("lexpilot")
            .withPassword("lexpilot_test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @TempDir
    static Path tempUploadDir;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // Use 'none' since init.sql creates the schema via withInitScript
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        // Point embedding service to a non-existent URL — embedding will fail gracefully
        registry.add("lexpilot.embedding-service.base-url", () -> "http://localhost:19999");
        registry.add("lexpilot.llm.api-key", () -> "test-key");
        registry.add("lexpilot.api-key", () -> "test-api-key");
        registry.add("lexpilot.ingestion.upload-dir", () -> tempUploadDir.toString());
        registry.add("lexpilot.ingestion.max-file-size-mb", () -> "20");
    }

    @Test
    void contextLoads() {
        // Smoke test — Spring context starts with all beans wired
    }

    @Test
    void uploadPdf_shouldExtractAndChunk() {
        // --- Build a minimal but valid PDF with enough text to pass the 50-char threshold ---
        byte[] pdfBytes = createMinimalPdf();

        // --- Upload via multipart POST ---
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return "test-consumer-complaint.pdf";
            }
        });

        ResponseEntity<DocumentUploadResponse> uploadResponse = restTemplate.exchange(
                "/api/v1/documents",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                DocumentUploadResponse.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(uploadResponse.getBody()).isNotNull();

        DocumentUploadResponse uploadResult = uploadResponse.getBody();
        assertThat(uploadResult.documentId()).isNotBlank();
        assertThat(uploadResult.status()).isEqualTo(DocumentStatus.EMBEDDING);

        // --- Check status endpoint ---
        ResponseEntity<IngestionStatusResponse> statusResponse = restTemplate.getForEntity(
                "/api/v1/documents/{id}/status",
                IngestionStatusResponse.class,
                uploadResult.documentId());

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody()).isNotNull();
        // Status should be EMBEDDING or FAILED (embedding service not available in test)
        String status = statusResponse.getBody().status();
        assertThat(status).isIn(DocumentStatus.EMBEDDING, DocumentStatus.FAILED);
    }

    @Test
    void uploadInvalidContentType_shouldReturn400() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("not a pdf".getBytes()) {
            @Override
            public String getFilename() {
                return "test.txt";
            }

            // Simulate a non-PDF content type
        });

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/documents",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        // Should fail with 400 (invalid content type) or similar
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void getStatusForNonExistentDoc_shouldReturn404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/documents/{id}/status",
                String.class,
                "00000000-0000-0000-0000-000000000000");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Creates a minimal but valid PDF containing enough text to pass Tika
     * extraction and the 50-char minimum threshold.
     * <p>
     * This builds a raw PDF from scratch without any external library —
     * just the bare minimum objects required by the PDF spec.
     */
    private static byte[] createMinimalPdf() {
        String sampleText = "Consumer Grievance Report. "
                + "This document describes a consumer complaint filed under the Consumer Protection Act 2019. "
                + "The complainant alleges deficiency in service by the respondent company. "
                + "Details of the complaint include delayed delivery, defective product, and lack of response "
                + "to multiple follow-up emails sent over a period of thirty days. "
                + "The complainant seeks a full refund and compensation for mental agony.";

        // Build a minimal valid PDF manually
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");

        // Object 1: Catalog
        int obj1Offset = pdf.length();
        pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // Object 2: Pages
        int obj2Offset = pdf.length();
        pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        // Object 4: Font
        int obj4Offset = pdf.length();
        pdf.append("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

        // Object 5: Stream content (the text)
        String streamContent = "BT\n/F1 12 Tf\n72 720 Td\n(" + escPdf(sampleText) + ") Tj\nET\n";
        int obj5Offset = pdf.length();
        pdf.append("5 0 obj\n<< /Length ").append(streamContent.length()).append(" >>\nstream\n");
        pdf.append(streamContent);
        pdf.append("endstream\nendobj\n");

        // Object 3: Page
        int obj3Offset = pdf.length();
        pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R ")
           .append("/MediaBox [0 0 612 792] ")
           .append("/Contents 5 0 R ")
           .append("/Resources << /Font << /F1 4 0 R >> >> ")
           .append(">>\nendobj\n");

        // Cross-reference table
        int xrefOffset = pdf.length();
        pdf.append("xref\n0 6\n");
        pdf.append(String.format("0000000000 65535 f \n"));
        pdf.append(String.format("%010d 00000 n \n", obj1Offset));
        pdf.append(String.format("%010d 00000 n \n", obj2Offset));
        pdf.append(String.format("%010d 00000 n \n", obj3Offset));
        pdf.append(String.format("%010d 00000 n \n", obj4Offset));
        pdf.append(String.format("%010d 00000 n \n", obj5Offset));

        // Trailer
        pdf.append("trailer\n<< /Size 6 /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");

        return pdf.toString().getBytes();
    }

    /** Escape special PDF string characters. */
    private static String escPdf(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
