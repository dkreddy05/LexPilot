package com.lexpilot.ingestion.kafka;

import com.lexpilot.ingestion.entity.DocumentStatus;
import com.lexpilot.ingestion.repository.DocumentChunkRepository;
import com.lexpilot.ingestion.repository.DocumentRepository;
import com.lexpilot.ingestion.service.EmbeddingServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kafka consumer that listens for {@code CHUNKED} events, calls the
 * embedding-service to vectorise each chunk, writes the vector into
 * Postgres, and marks the document as {@code READY} once all chunks
 * are embedded.
 */
@Component
public class ChunkEmbeddingConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChunkEmbeddingConsumer.class);

    private final EmbeddingServiceClient embeddingClient;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentRepository documentRepository;

    public ChunkEmbeddingConsumer(
            EmbeddingServiceClient embeddingClient,
            DocumentChunkRepository chunkRepository,
            DocumentRepository documentRepository) {
        this.embeddingClient = embeddingClient;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
    }

    @KafkaListener(
            topics = "${lexpilot.ingestion.kafka-topic}",
            groupId = "lexpilot-embedding-group",
            properties = {
                "spring.json.value.default.type=com.lexpilot.ingestion.kafka.IngestionEvent"
            })
    @Transactional
    public void onChunkedEvent(IngestionEvent event) {
        if (!"CHUNKED".equals(event.eventType())) {
            log.debug("Ignoring event type: {}", event.eventType());
            return;
        }

        IngestionEvent.ChunkPayload payload = event.payload();
        UUID chunkId = UUID.fromString(payload.chunkId());
        UUID documentId = UUID.fromString(event.documentId());

        log.info("Processing CHUNKED event: doc={} chunk={} index={}/{}",
                 documentId, chunkId, payload.chunkIndex(), payload.expectedChunkCount());

        try {
            // 1. Call embedding service
            List<List<Float>> embeddings = embeddingClient.embed(List.of(payload.content()));
            List<Float> vector = embeddings.get(0);

            // 2. Convert to pgvector string format: [0.1,0.2,0.3,...]
            String pgvectorLiteral = vector.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]"));

            // 3. Write embedding to document_chunks
            chunkRepository.updateEmbedding(chunkId, pgvectorLiteral);

            log.debug("Embedded chunk {} ({}-dim vector)", chunkId, vector.size());

            // 4. Check if all chunks for this document are now embedded
            long totalChunks = chunkRepository.countByDocumentId(documentId);
            long embeddedChunks = chunkRepository.countEmbeddedByDocumentId(documentId);

            if (embeddedChunks >= totalChunks) {
                // NOTE: This guard prevents a successful trailing chunk from overwriting FAILED status.
                // The reverse case (a lagging FAILED write landing after READY) is an accepted non-goal:
                // once all chunks embed successfully and status is READY, the error-path code below
                // only fires on embedding exceptions — which can't happen after successful completion.
                int updated = documentRepository.updateStatusIfNotFailed(documentId, DocumentStatus.READY);
                if (updated > 0) {
                    log.info("Document {} is now READY ({}/{} chunks embedded)",
                             documentId, embeddedChunks, totalChunks);
                } else {
                    log.warn("Document {} has all chunks embedded but status was not updated " +
                             "(already FAILED by another thread)", documentId);
                }
            }

        } catch (Exception e) {
            log.error("Failed to embed chunk {} for document {}", chunkId, documentId, e);
            // Mark document as failed
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(DocumentStatus.FAILED);
                doc.setErrorMessage("Embedding failed for chunk " + chunkId + ": " + e.getMessage());
                documentRepository.save(doc);
            });
        }
    }
}
