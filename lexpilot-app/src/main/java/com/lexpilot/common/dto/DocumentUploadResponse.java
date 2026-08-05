package com.lexpilot.common.dto;

public record DocumentUploadResponse(
        String documentId,
        String filename,
        String status,
        String message
) {}
