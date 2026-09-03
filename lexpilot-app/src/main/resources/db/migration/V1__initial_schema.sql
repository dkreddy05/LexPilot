-- =============================================================================
-- LexPilot — V1 Initial Database Schema Migration
-- Managed by Flyway
-- =============================================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Documents table — one row per uploaded source document
CREATE TABLE IF NOT EXISTS documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename      TEXT NOT NULL,
    content_type  TEXT NOT NULL,
    status        TEXT NOT NULL DEFAULT 'UPLOADED',
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

-- Indexes for vector retrieval and full-text BM25 search
CREATE INDEX IF NOT EXISTS idx_doc_chunks_embedding    ON document_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_doc_chunks_tsv          ON document_chunks USING GIN (tsv_content);
CREATE INDEX IF NOT EXISTS idx_doc_chunks_document_id  ON document_chunks (document_id);

-- Conversations table — one row per chat session
CREATE TABLE IF NOT EXISTS conversations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Conversation messages — ordered turns within a conversation
CREATE TABLE IF NOT EXISTS conversation_messages (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id   UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role              TEXT NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content           TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_conv_messages_conv_id ON conversation_messages (conversation_id, id);
