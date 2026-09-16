package com.lexpilot.common.audit;

import com.lexpilot.common.tenant.TenantContext;
import com.lexpilot.generation.dto.GeneratedAnswer;
import com.lexpilot.retrieval.dto.ScoredChunk;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for recording audit events asynchronously.
 * <p>
 * All {@code log*} methods are {@code @Async} so they don't block the request thread.
 * Uses the {@link TenantContext} to resolve the current tenant if not explicitly provided.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Log a query event — records the query text, retrieved chunks, generated answer,
     * and confidence flag.
     */
    @Async
    public void logQuery(String query, List<ScoredChunk> chunks,
                         GeneratedAnswer answer, String ipAddress) {
        try {
            UUID tenantId = resolveTenantId();

            List<String> chunkIds = chunks.stream()
                    .map(c -> c.chunkId().toString())
                    .toList();

            String chunkIdsJson;
            try {
                chunkIdsJson = objectMapper.writeValueAsString(chunkIds);
            } catch (JsonProcessingException e) {
                chunkIdsJson = "[]";
            }

            AuditLogEntity entity = new AuditLogEntity(tenantId, "QUERY")
                    .queryText(query)
                    .retrievedChunkIds(chunkIdsJson)
                    .generatedAnswer(answer.answer())
                    .lowConfidence(answer.lowConfidence())
                    .ipAddress(ipAddress);

            auditLogRepository.save(entity);
            log.debug("Audit logged: QUERY for tenant {}", tenantId);
        } catch (Exception e) {
            // Audit logging must never fail the request — log and swallow
            log.error("Failed to write audit log for QUERY", e);
        }
    }

    /**
     * Log a document upload event.
     */
    @Async
    public void logDocumentUpload(String documentId, String filename, String ipAddress) {
        try {
            UUID tenantId = resolveTenantId();

            AuditLogEntity entity = new AuditLogEntity(tenantId, "DOCUMENT_UPLOAD")
                    .queryText("Upload: " + filename + " (docId=" + documentId + ")")
                    .ipAddress(ipAddress);

            auditLogRepository.save(entity);
            log.debug("Audit logged: DOCUMENT_UPLOAD for tenant {} docId={}", tenantId, documentId);
        } catch (Exception e) {
            log.error("Failed to write audit log for DOCUMENT_UPLOAD", e);
        }
    }

    /**
     * Log a document deletion event.
     */
    @Async
    public void logDocumentDelete(String documentId, String ipAddress) {
        try {
            UUID tenantId = resolveTenantId();

            AuditLogEntity entity = new AuditLogEntity(tenantId, "DOCUMENT_DELETE")
                    .queryText("Delete docId=" + documentId)
                    .ipAddress(ipAddress);

            auditLogRepository.save(entity);
            log.debug("Audit logged: DOCUMENT_DELETE for tenant {} docId={}", tenantId, documentId);
        } catch (Exception e) {
            log.error("Failed to write audit log for DOCUMENT_DELETE", e);
        }
    }

    private UUID resolveTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
}
