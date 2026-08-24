package com.lexpilot.ingestion.service;

import com.lexpilot.common.config.AppConfig;
import com.lexpilot.common.dto.DocumentUploadResponse;
import com.lexpilot.common.dto.IngestionStatusResponse;
import com.lexpilot.common.exception.DocumentNotFoundException;
import com.lexpilot.common.exception.ExtractionException;
import com.lexpilot.common.exception.InvalidDocumentException;
import com.lexpilot.ingestion.chunking.ChunkingOptions;
import com.lexpilot.ingestion.chunking.ChunkingStrategy;
import com.lexpilot.ingestion.entity.DocumentChunkEntity;
import com.lexpilot.ingestion.entity.DocumentEntity;
import com.lexpilot.ingestion.entity.DocumentStatus;
import com.lexpilot.ingestion.kafka.IngestionEvent;
import com.lexpilot.ingestion.kafka.IngestionKafkaProducer;
import com.lexpilot.ingestion.repository.DocumentChunkRepository;
import com.lexpilot.ingestion.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the synchronous document ingestion pipeline:
 * <ol>
 *   <li>Validate and store the uploaded PDF</li>
 *   <li>Extract text via Tika</li>
 *   <li>Chunk the text</li>
 *   <li>Persist chunk rows (without embeddings)</li>
 *   <li>Publish one Kafka CHUNKED event per chunk</li>
 * </ol>
 * <p>
 * The embedding step happens asynchronously via {@link com.lexpilot.ingestion.kafka.ChunkEmbeddingConsumer}.
 */
@Service
public class DocumentUploadService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024; // 20 MB

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TikaExtractionService tikaExtractionService;
    private final ChunkingStrategy<String> chunker;
    private final IngestionKafkaProducer kafkaProducer;
    private final ChunkingOptions chunkingOptions;
    private final Path uploadDir;

    public DocumentUploadService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            TikaExtractionService tikaExtractionService,
            @Qualifier("fixedSizeChunker") ChunkingStrategy<String> chunker,
            IngestionKafkaProducer kafkaProducer,
            AppConfig appConfig) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.tikaExtractionService = tikaExtractionService;
        this.chunker = chunker;
        this.kafkaProducer = kafkaProducer;
        this.chunkingOptions = new ChunkingOptions(
                appConfig.ingestion().chunkSize(),
                appConfig.ingestion().chunkOverlap());
        this.uploadDir = Paths.get(appConfig.ingestion().uploadDir());
        ensureUploadDirectory();
    }

    /**
     * Upload a PDF, run extraction + chunking synchronously, then publish
     * Kafka events for async embedding.
     *
     * @param file       the uploaded multipart file
     * @param sourceType optional label (e.g. CONSUMER_PROTECTION, RBI)
     * @return upload response with document ID and status
     */
    @Transactional
    public DocumentUploadResponse upload(MultipartFile file, String sourceType) {
        // --- 1. Validate ---
        validateFile(file);

        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "unnamed.pdf";

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidDocumentException("Failed to read uploaded file: " + e.getMessage());
        }

        // --- 2. Persist document row (UPLOADED) ---
        DocumentEntity doc = new DocumentEntity(filename, ALLOWED_CONTENT_TYPE);
        doc = documentRepository.save(doc);
        UUID documentId = doc.getId();

        log.info("Document {} created with status UPLOADED (filename={})", documentId, filename);

        try {
            // --- 3. Store raw file to disk ---
            storeFile(documentId, fileBytes);

            // --- 4. Extract text (EXTRACTING) ---
            doc.setStatus(DocumentStatus.EXTRACTING);
            documentRepository.save(doc);

            ExtractionResult extraction = tikaExtractionService.extract(fileBytes, ALLOWED_CONTENT_TYPE);
            log.info("Document {} extracted: {} chars, {} pages",
                     documentId, extraction.text().length(), extraction.pageCount());

            // --- 5. Chunk text (CHUNKING) ---
            doc.setStatus(DocumentStatus.CHUNKING);
            documentRepository.save(doc);

            List<String> chunkContents = chunker.chunk(extraction.text(), chunkingOptions);

            if (chunkContents.isEmpty()) {
                throw new ExtractionException("Chunking produced zero chunks from extracted text");
            }

            log.info("Document {} chunked into {} chunks", documentId, chunkContents.size());

            // --- 6. Persist chunk rows (without embeddings) ---
            for (int i = 0; i < chunkContents.size(); i++) {
                DocumentChunkEntity chunk = new DocumentChunkEntity(
                        documentId, i, chunkContents.get(i));
                chunkRepository.save(chunk);
            }

            // --- 7. Publish Kafka events (one per chunk) then set EMBEDDING ---
            doc.setStatus(DocumentStatus.EMBEDDING);
            documentRepository.save(doc);

            // Re-read saved chunks to get their generated IDs
            List<DocumentChunkEntity> savedChunks = chunkRepository.findAll().stream()
                    .filter(c -> c.getDocumentId().equals(documentId))
                    .toList();

            for (DocumentChunkEntity chunk : savedChunks) {
                IngestionEvent event = new IngestionEvent(
                        UUID.randomUUID().toString(),
                        documentId.toString(),
                        "CHUNKED",
                        new IngestionEvent.ChunkPayload(
                                chunk.getId().toString(),
                                chunk.getChunkIndex(),
                                chunk.getContent(),
                                chunkContents.size()),
                        IngestionEvent.CURRENT_SCHEMA_VERSION);
                kafkaProducer.publish(event);
            }

            log.info("Document {} published {} CHUNKED events to Kafka",
                     documentId, savedChunks.size());

            return new DocumentUploadResponse(
                    documentId.toString(),
                    filename,
                    DocumentStatus.EMBEDDING,
                    "Document accepted. Text extracted and chunked; embedding in progress.");

        } catch (ExtractionException e) {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(e.getMessage());
            documentRepository.save(doc);
            throw e;
        } catch (Exception e) {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(e.getMessage());
            documentRepository.save(doc);
            log.error("Document {} ingestion failed", documentId, e);
            throw new ExtractionException("Ingestion pipeline failed: " + e.getMessage(), e);
        }
    }

    /**
     * Look up the current status of a document.
     */
    public IngestionStatusResponse getStatus(String documentId) {
        UUID id;
        try {
            id = UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            throw new DocumentNotFoundException(documentId);
        }

        DocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        return new IngestionStatusResponse(
                doc.getId().toString(),
                doc.getStatus(),
                doc.getErrorMessage());
    }

    /**
     * Delete a document and all its associated chunks/embeddings, and clean up the file on disk.
     */
    @Transactional
    public void deleteDocument(String documentId) {
        UUID id;
        try {
            id = UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            throw new DocumentNotFoundException(documentId);
        }

        DocumentEntity doc = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        // 1. Delete chunks
        chunkRepository.deleteByDocumentId(id);

        // 2. Delete raw file on disk
        try {
            Path filePath = uploadDir.resolve(id.toString() + ".pdf");
            Files.deleteIfExists(filePath);
            log.debug("Deleted raw file at {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file for document {}: {}", documentId, e.getMessage());
        }

        // 3. Delete document row
        documentRepository.delete(doc);
        log.info("Document {} and its chunks deleted successfully", documentId);
    }

    /**
     * Retrieve all tracked documents.
     */
    public List<DocumentUploadResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(doc -> new DocumentUploadResponse(
                        doc.getId().toString(),
                        doc.getFilename(),
                        doc.getStatus(),
                        doc.getErrorMessage() != null ? doc.getErrorMessage() : "Document status: " + doc.getStatus()
                ))
                .toList();
    }

    // ---- Private helpers ----

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(ALLOWED_CONTENT_TYPE)) {
            throw new InvalidDocumentException(
                    "Invalid content type: " + contentType + ". Only application/pdf is supported.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidDocumentException(
                    "File size " + (file.getSize() / (1024 * 1024)) + " MB exceeds the 20 MB limit.");
        }
    }

    private void storeFile(UUID documentId, byte[] fileBytes) {
        try {
            Path filePath = uploadDir.resolve(documentId.toString() + ".pdf");
            Files.write(filePath, fileBytes);
            log.debug("Stored raw file at {}", filePath);
        } catch (IOException e) {
            throw new InvalidDocumentException("Failed to store uploaded file: " + e.getMessage());
        }
    }

    private void ensureUploadDirectory() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            log.warn("Could not create upload directory {}: {}", uploadDir, e.getMessage());
        }
    }
}
