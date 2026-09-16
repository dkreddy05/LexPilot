package com.lexpilot.gateway.controller;

import com.lexpilot.common.audit.AuditService;
import com.lexpilot.common.dto.DocumentUploadResponse;
import com.lexpilot.common.dto.IngestionStatusResponse;
import com.lexpilot.ingestion.service.DocumentUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentUploadService documentUploadService;
    private final AuditService auditService;

    public DocumentController(DocumentUploadService documentUploadService,
                              AuditService auditService) {
        this.documentUploadService = documentUploadService;
        this.auditService = auditService;
    }

    /**
     * Upload a PDF document for ingestion.
     * The synchronous pipeline runs extraction + chunking inline, then
     * publishes Kafka events for async embedding.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceType", required = false) String sourceType,
            HttpServletRequest httpRequest) {

        DocumentUploadResponse response = documentUploadService.upload(file, sourceType);

        auditService.logDocumentUpload(response.documentId(), file.getOriginalFilename(), httpRequest.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get the current ingestion status of a document.
     */
    @GetMapping("/{documentId}/status")
    public ResponseEntity<IngestionStatusResponse> getIngestionStatus(
            @PathVariable String documentId) {

        IngestionStatusResponse response = documentUploadService.getStatus(documentId);
        return ResponseEntity.ok(response);
    }

    /**
     * List all tracked documents.
     */
    @GetMapping
    public ResponseEntity<java.util.List<DocumentUploadResponse>> listDocuments() {
        return ResponseEntity.ok(documentUploadService.getAllDocuments());
    }

    /**
     * Delete a document and its indexed vectors.
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String documentId,
                                               HttpServletRequest httpRequest) {
        documentUploadService.deleteDocument(documentId);
        auditService.logDocumentDelete(documentId, httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}
