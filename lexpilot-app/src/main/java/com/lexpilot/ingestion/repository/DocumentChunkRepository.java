package com.lexpilot.ingestion.repository;

import com.lexpilot.ingestion.entity.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    /**
     * Count the number of chunks belonging to a document.
     */
    long countByDocumentId(UUID documentId);

    /**
     * Count chunks that already have an embedding vector stored.
     */
    @Query(value = "SELECT COUNT(*) FROM document_chunks WHERE document_id = :docId AND embedding IS NOT NULL",
           nativeQuery = true)
    long countEmbeddedByDocumentId(@Param("docId") UUID documentId);

    /**
     * Write a pgvector embedding into an existing chunk row.
     * The embedding string must be in pgvector literal format, e.g. {@code [0.1,0.2,...]}.
     */
    @Modifying
    @Query(value = "UPDATE document_chunks SET embedding = cast(:embedding AS vector) WHERE id = :chunkId",
           nativeQuery = true)
    void updateEmbedding(@Param("chunkId") UUID chunkId, @Param("embedding") String embedding);

    /**
     * Delete all chunks belonging to a document.
     */
    void deleteByDocumentId(UUID documentId);
}
