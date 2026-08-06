package com.lexpilot.ingestion.service;

/**
 * Result of Apache Tika text extraction from a document.
 *
 * @param text      the extracted plain text
 * @param pageCount number of pages detected (may be -1 if unknown)
 * @param title     document title from metadata, or null
 */
public record ExtractionResult(
        String text,
        int pageCount,
        String title
) {}
