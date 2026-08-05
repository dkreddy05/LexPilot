-- =============================================================================
-- LexPilot — Database Initialisation Script
-- Runs once on first container start via docker-entrypoint-initdb.d
-- =============================================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable full-text search (built-in, but explicit for clarity)
-- BM25 queries will use tsvector columns on the chunks table.

-- =============================================================================
-- TODO: Replace stub DDL with Flyway-managed migrations before production.
--       This file is for local dev bootstrapping only.
-- =============================================================================

-- Documents table — one row per uploaded source document
CREATE TABLE IF NOT EXISTS documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename    TEXT NOT NULL,
    title       TEXT,
    source_type TEXT,          -- e.g. 'CONSUMER_PROTECTION', 'RBI', 'TENANT'
    mime_type   TEXT,
    status      TEXT NOT NULL DEFAULT 'PENDING',
                               -- PENDING | PROCESSING | INDEXED | FAILED
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    indexed_at  TIMESTAMPTZ
);

-- Chunks table — one row per text chunk of a document
CREATE TABLE IF NOT EXISTS chunks (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id    UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index    INTEGER NOT NULL,
    content        TEXT NOT NULL,
    -- TODO: dimension must match embedding model output (bge-small-en = 384)
    embedding      VECTOR(384),
    tsv_content    TSVECTOR GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for retrieval
CREATE INDEX IF NOT EXISTS idx_chunks_embedding   ON chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);  -- TODO: tune lists based on dataset size

CREATE INDEX IF NOT EXISTS idx_chunks_tsv         ON chunks USING GIN (tsv_content);
CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON chunks (document_id);
