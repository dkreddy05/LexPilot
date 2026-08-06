-- =============================================================================
-- LexPilot — Database Initialisation Script
-- Runs once on first container start via docker-entrypoint-initdb.d
-- =============================================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable full-text search (built-in, but explicit for clarity)
-- BM25 queries will use tsvector columns on the document_chunks table.

-- =============================================================================
-- TODO: Replace stub DDL with Flyway-managed migrations before production.
--       This file is for local dev bootstrapping only.
-- =============================================================================

-- Documents table — one row per uploaded source document
CREATE TABLE IF NOT EXISTS documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename      TEXT NOT NULL,
    content_type  TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'UPLOADED',
                                -- UPLOADED | EXTRACTING | CHUNKING | EMBEDDING | READY | FAILED
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    error_message TEXT
);

-- Document chunks table — one row per text chunk of a document
CREATE TABLE IF NOT EXISTS document_chunks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index     INT NOT NULL,
    content         TEXT NOT NULL,
    embedding       VECTOR(384),   -- matches all-MiniLM-L6-v2 (384 dims)
    tsv_content     TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for retrieval
CREATE INDEX IF NOT EXISTS idx_doc_chunks_embedding    ON document_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);  -- TODO: tune lists based on dataset size

CREATE INDEX IF NOT EXISTS idx_doc_chunks_tsv          ON document_chunks USING GIN (tsv_content);
CREATE INDEX IF NOT EXISTS idx_doc_chunks_document_id  ON document_chunks (document_id);
