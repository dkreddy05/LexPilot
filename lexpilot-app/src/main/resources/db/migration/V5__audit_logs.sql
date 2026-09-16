-- V5__audit_logs.sql
-- Immutable audit log for compliance and legal accountability.
-- Records every query, document upload/delete, and login event.

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    action          TEXT NOT NULL,      -- QUERY, DOCUMENT_UPLOAD, DOCUMENT_DELETE, LOGIN
    user_id         TEXT,               -- nullable until JWT auth is implemented
    query_text      TEXT,               -- the user's query (for QUERY actions)
    retrieved_chunk_ids JSONB,          -- array of chunk UUIDs used in generation
    generated_answer    TEXT,           -- truncated answer text for audit trail
    low_confidence  BOOLEAN DEFAULT FALSE,
    ip_address      TEXT,               -- client IP for security auditing
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for tenant-scoped audit queries (e.g. "show me all queries by tenant X in the last 7 days")
CREATE INDEX idx_audit_tenant_time ON audit_logs(tenant_id, created_at DESC);

-- Index for action-type filtering
CREATE INDEX idx_audit_action ON audit_logs(action, created_at DESC);
