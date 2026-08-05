package com.lexpilot.common.dto;

public record IngestionStatusResponse(
        String documentId,
        String status,
        String errorDetail
) {}
