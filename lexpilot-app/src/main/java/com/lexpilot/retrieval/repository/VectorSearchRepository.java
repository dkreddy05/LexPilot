package com.lexpilot.retrieval.repository;

import com.lexpilot.ingestion.entity.DocumentChunkEntity;
import com.lexpilot.retrieval.dto.ScoredChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * pgvector-backed nearest-neighbour search over document chunk embeddings.
 * <p>
 * Uses cosine distance ({@code <=>}) which is correct for the normalised
 * {@code all-MiniLM-L6-v2} embeddings produced by the embedding-service.
 * The score is {@code 1 - cosine_distance}, yielding similarity in [0, 1].
 * <p>
 * JOINs the {@code documents} table to retrieve the source filename for
 * citations, avoiding N+1 lookups downstream.
 */
@Repository
public interface VectorSearchRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    /**
     * Raw native query — returns {@code Object[]} rows because Spring Data JPA
     * cannot project computed columns like {@code score} into entity fields.
     * <p>
     * Callers should use {@link #findNearest(float[], int)} instead.
     */
    @Query(value = """
            SELECT dc.id, dc.document_id, dc.content,
                   1 - (dc.embedding <=> CAST(:queryVector AS vector)) AS score,
                   d.filename
            FROM document_chunks dc
            JOIN documents d ON d.id = dc.document_id
            WHERE dc.embedding IS NOT NULL
            ORDER BY dc.embedding <=> CAST(:queryVector AS vector)
            LIMIT :topK
            """, nativeQuery = true)
    List<Object[]> findNearestRaw(@Param("queryVector") String queryVector,
                                  @Param("topK") int topK);

    /**
     * Find the {@code topK} document chunks whose embeddings are nearest to
     * the given query embedding, ranked by cosine similarity (descending).
     *
     * @param queryEmbedding the query vector (384-dim float array)
     * @param topK           maximum number of results to return
     * @return scored chunks ordered by similarity, highest first
     */
    default List<ScoredChunk> findNearest(float[] queryEmbedding, int topK) {
        String pgvectorLiteral = floatArrayToPgvector(queryEmbedding);
        List<Object[]> rows = findNearestRaw(pgvectorLiteral, topK);
        return rows.stream()
                .map(VectorSearchRepository::mapRow)
                .collect(Collectors.toList());
    }

    // ---- Internal helpers ----

    /**
     * Convert a float[] to the pgvector literal format: {@code [0.1,0.2,0.3,...]}.
     */
    private static String floatArrayToPgvector(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Map a native-query {@code Object[]} row to a {@link ScoredChunk}.
     * Column order: id (UUID), document_id (UUID), content (String), score (double), filename (String).
     */
    private static ScoredChunk mapRow(Object[] row) {
        UUID chunkId = (UUID) row[0];
        UUID documentId = (UUID) row[1];
        String content = (String) row[2];
        double score = ((Number) row[3]).doubleValue();
        String sourceLabel = (String) row[4];
        return new ScoredChunk(chunkId, documentId, content, score, sourceLabel);
    }
}
