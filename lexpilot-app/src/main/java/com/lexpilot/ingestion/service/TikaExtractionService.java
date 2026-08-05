package com.lexpilot.ingestion.service;

import org.springframework.stereotype.Service;

@Service
public class TikaExtractionService {

    public TikaExtractionService() {
    }

    public String extract(byte[] fileBytes, String mimeType) {
        // TODO: Implement Apache Tika text extraction
        throw new UnsupportedOperationException("TikaExtractionService.extract() not yet implemented");
    }
}
