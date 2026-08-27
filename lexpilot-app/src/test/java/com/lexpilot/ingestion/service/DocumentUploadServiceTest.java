package com.lexpilot.ingestion.service;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.exception.InvalidDocumentException;
import com.lexpilot.ingestion.chunking.ChunkingStrategy;
import com.lexpilot.ingestion.kafka.IngestionKafkaProducer;
import com.lexpilot.ingestion.repository.DocumentChunkRepository;
import com.lexpilot.ingestion.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DocumentUploadServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentChunkRepository chunkRepository;
    @Mock private TikaExtractionService tikaExtractionService;
    @Mock private ChunkingStrategy<String> chunker;
    @Mock private IngestionKafkaProducer kafkaProducer;

    @TempDir
    Path tempUploadDir;

    private DocumentUploadService uploadService;

    @BeforeEach
    void setUp() {
        AppConfig appConfig = new AppConfig(
                "test-key",
                new AppConfig.EmbeddingServiceConfig("http://localhost:8000"),
                new AppConfig.LlmConfig("key", "http://llm", "test-model", 1024, 0.2, 30),
                new AppConfig.IngestionConfig("topic", 500, 75, tempUploadDir.toString(), 20),
                new AppConfig.RetrievalConfig(20, 20, 10),
                new AppConfig.RateLimitingConfig(60, 1000),
                new AppConfig.ConversationConfig(10)
        );

        uploadService = new DocumentUploadService(
                documentRepository,
                chunkRepository,
                tikaExtractionService,
                chunker,
                kafkaProducer,
                appConfig
        );
    }

    @Test
    void upload_whenFileHasFakePdfMimeTypeButInvalidMagicBytes_shouldThrowInvalidDocumentException() {
        // Content-Type is "application/pdf", but content is plain text / binary without %PDF-
        byte[] fakePdfBytes = "This is a fake PDF without magic bytes header".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file",
                "malicious.pdf",
                "application/pdf",
                fakePdfBytes
        );

        assertThatThrownBy(() -> uploadService.upload(fakeFile, "TEST"))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("file signature does not match a valid PDF header");
    }

    @Test
    void upload_whenFileIsEmpty_shouldThrowInvalidDocumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> uploadService.upload(emptyFile, "TEST"))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Uploaded file is empty");
    }

    @Test
    void upload_whenInvalidContentType_shouldThrowInvalidDocumentException() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file",
                "program.exe",
                "application/octet-stream",
                "MZ...".getBytes()
        );

        assertThatThrownBy(() -> uploadService.upload(exeFile, "TEST"))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Only application/pdf is supported");
    }
}
