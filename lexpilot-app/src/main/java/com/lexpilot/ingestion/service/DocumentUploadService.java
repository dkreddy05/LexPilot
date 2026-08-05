package com.lexpilot.ingestion.service;

import com.lexpilot.common.dto.DocumentUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentUploadService {

    public DocumentUploadService() {
    }

    public DocumentUploadResponse upload(MultipartFile file, String sourceType) {
        // TODO: Implement file storage and Kafka event publication
        throw new UnsupportedOperationException("DocumentUploadService.upload() not yet implemented");
    }

    public String getStatus(String documentId) {
        // TODO: Implement document status lookup
        throw new UnsupportedOperationException("DocumentUploadService.getStatus() not yet implemented");
    }
}
