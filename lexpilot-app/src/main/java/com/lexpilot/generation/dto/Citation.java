package com.lexpilot.generation.dto;

import java.util.UUID;

/**
 * A citation linking a marker in the generated answer back to a source chunk.
 *
 * @param marker      the [n] number used in the answer text (1-based)
 * @param chunkId     primary key of the cited document_chunks row
 * @param documentId  foreign key to the parent document
 * @param sourceLabel human-readable source name (e.g. filename)
 */
public record Citation(int marker, UUID chunkId, UUID documentId, String sourceLabel) {}
