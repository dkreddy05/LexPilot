package com.lexpilot.ingestion.service;

import com.lexpilot.common.exception.ExtractionException;
import com.lexpilot.common.tenant.TenantContext;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TikaExtractionService tikaExtractionService;
    private final ChunkingStrategy<String> chunker;
    private final IngestionKafkaProducer kafkaProducer;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            TikaExtractionService tikaExtractionService,
            ChunkingStrategy<String> chunker,
            IngestionKafkaProducer kafkaProducer) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.tikaExtractionService = tikaExtractionService;
        this.chunker = chunker;
        this.kafkaProducer = kafkaProducer;
    }

    @Async
    @Transactional
    public void processDocumentAsync(UUID documentId, byte[] fileBytes, String contentType, ChunkingOptions chunkingOptions, UUID tenantId) {
        try {
            // Set TenantContext for this async thread to ensure RLS works
            TenantContext.setTenantId(tenantId);

            DocumentEntity doc = documentRepository.findById(documentId).orElseThrow();

            // --- 4. Extract text (EXTRACTING) ---
            doc.setStatus(DocumentStatus.EXTRACTING);
            documentRepository.save(doc);

            ExtractionResult extraction = tikaExtractionService.extract(fileBytes, contentType);
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
                DocumentChunkEntity chunk = new DocumentChunkEntity(documentId, i, chunkContents.get(i));
                chunkRepository.save(chunk);
            }

            // --- 7. Publish Kafka events (one per chunk) then set EMBEDDING ---
            doc.setStatus(DocumentStatus.EMBEDDING);
            documentRepository.save(doc);

            List<DocumentChunkEntity> savedChunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);

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

            log.info("Document {} published {} CHUNKED events to Kafka", documentId, savedChunks.size());

        } catch (ExtractionException e) {
            updateStatusToFailed(documentId, e.getMessage());
        } catch (Exception e) {
            log.error("Document {} ingestion failed", documentId, e);
            updateStatusToFailed(documentId, "Ingestion pipeline failed: " + e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private void updateStatusToFailed(UUID documentId, String errorMessage) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setErrorMessage(errorMessage);
            documentRepository.save(doc);
        });
    }
}
