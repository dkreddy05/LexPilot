package com.lexpilot.gateway.controller;

import com.lexpilot.common.dto.DocumentUploadResponse;
import com.lexpilot.common.dto.IngestionStatusResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    public DocumentController() {
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sourceType", required = false) String sourceType) {
        // TODO: Call DocumentUploadService
        return ResponseEntity.accepted().body(new DocumentUploadResponse(
                "stub-doc-id-00000000",
                file.getOriginalFilename(),
                "PENDING",
                "Document accepted for processing."
        ));
    }

    @GetMapping("/{documentId}/status")
    public ResponseEntity<IngestionStatusResponse> getIngestionStatus(
            @PathVariable String documentId) {
        // TODO: Call DocumentUploadService
        return ResponseEntity.ok(new IngestionStatusResponse(documentId, "PENDING", null));
    }
}
