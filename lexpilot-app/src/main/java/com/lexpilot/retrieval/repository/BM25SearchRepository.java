package com.lexpilot.retrieval.repository;

import com.lexpilot.retrieval.dto.ScoredChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Executes BM25-style full text search using PostgreSQL's native tsvector
 * and websearch_to_tsquery rank scoring (ts_rank_cd).
 */
@Repository
public class BM25SearchRepository {

    private static final Logger log = LoggerFactory.getLogger(BM25SearchRepository.class);

    private static final String BM25_QUERY = """
            SELECT dc.id, dc.document_id, dc.content,
                   ts_rank_cd(dc.tsv_content, websearch_to_tsquery('english', ?)) AS score,
                   d.filename
            FROM document_chunks dc
            JOIN documents d ON d.id = dc.document_id
            WHERE dc.tsv_content @@ websearch_to_tsquery('english', ?)
            ORDER BY score DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public BM25SearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Find top-K document chunks matching the query using PostgreSQL full-text search.
     *
     * @param query natural language search query
     * @param topK  maximum chunks to return
     * @return scored chunks ordered by text rank (descending)
     */
    public List<ScoredChunk> findTopKByBM25(String query, int topK) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return jdbcTemplate.query(BM25_QUERY, ROW_MAPPER, query.trim(), query.trim(), topK);
        } catch (DataAccessException e) {
            log.warn("BM25 query failed for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Backward-compatible overload accepting domain filter.
     */
    public List<ScoredChunk> findTopKByBM25(String query, int topK, String domain) {
        return findTopKByBM25(query, topK);
    }

    private static final RowMapper<ScoredChunk> ROW_MAPPER = (rs, rowNum) -> {
        UUID chunkId = parseUuid(rs.getObject("id"));
        UUID documentId = parseUuid(rs.getObject("document_id"));
        String content = rs.getString("content");
        double score = rs.getDouble("score");
        String filename = rs.getString("filename");

        return new ScoredChunk(chunkId, documentId, content, score, filename);
    };

    private static UUID parseUuid(Object obj) {
        if (obj instanceof UUID u) {
            return u;
        }
        return UUID.fromString(obj.toString());
    }
}
