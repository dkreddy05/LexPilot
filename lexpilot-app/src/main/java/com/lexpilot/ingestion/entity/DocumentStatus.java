package com.lexpilot.ingestion.entity;

/**
 * Status constants for the document ingestion pipeline.
 * Matches the status column in the {@code documents} table.
 */
public final class DocumentStatus {

    private DocumentStatus() {
        // utility class — no instantiation
    }

    /** File uploaded but processing has not started. */
    public static final String UPLOADED = "UPLOADED";

    /** Tika text extraction in progress. */
    public static final String EXTRACTING = "EXTRACTING";

    /** Text chunking in progress. */
    public static final String CHUNKING = "CHUNKING";

    /** Chunks are being sent to the embedding service. */
    public static final String EMBEDDING = "EMBEDDING";

    /** All chunks embedded successfully; document is searchable. */
    public static final String READY = "READY";

    /** An error occurred at some stage; see {@code error_message}. */
    public static final String FAILED = "FAILED";
}
