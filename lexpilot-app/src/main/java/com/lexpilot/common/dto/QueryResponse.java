package com.lexpilot.common.dto;

import java.util.List;

public record QueryResponse(
        String answer,
        List<Citation> citations,
        boolean lowConfidence,
        String refusalReason
) {

    public record Citation(
            String chunkId,
            String documentTitle,
            String excerpt,
            Integer pageNumber
    ) {}
}
