package com.lexpilot.ingestion.repository;

import com.lexpilot.ingestion.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    /**
     * Conditionally update document status, but only if the document is not already FAILED.
     * <p>
     * Prevents a race where a successfully-embedding trailing chunk overwrites
     * a FAILED status set by an earlier chunk's error handler (REC-3).
     *
     * @return row count — 1 if updated, 0 if skipped because status was already FAILED
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE documents SET status = :newStatus WHERE id = :docId AND status != 'FAILED'",
           nativeQuery = true)
    int updateStatusIfNotFailed(@Param("docId") UUID documentId, @Param("newStatus") String newStatus);
}

