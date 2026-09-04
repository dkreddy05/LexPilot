-- =============================================================================
-- LexPilot — V2 Graph Schema Migration
-- Adds tables for Graphify code intelligence knowledge graphs
-- =============================================================================

-- Enable trigram extension for fuzzy label search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ─────────────────────────────────────────────────────────────────────────────
-- Repository metadata — one row per analyzed code repository
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS graph_repositories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    url             VARCHAR(2048),
    branch          VARCHAR(255) DEFAULT 'main',
    commit_hash     VARCHAR(64),
    analysis_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    node_count      INTEGER DEFAULT 0,
    edge_count      INTEGER DEFAULT 0,
    community_count INTEGER DEFAULT 0,
    error_detail    TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Graph nodes — files, classes, functions, modules
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS graph_nodes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID NOT NULL REFERENCES graph_repositories(id) ON DELETE CASCADE,
    external_id     VARCHAR(512) NOT NULL,
    label           VARCHAR(512) NOT NULL,
    file_type       VARCHAR(64),
    source_file     VARCHAR(1024),
    source_location VARCHAR(64),
    community       INTEGER DEFAULT -1,
    norm_label      VARCHAR(512),
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (repository_id, external_id)
);

CREATE INDEX IF NOT EXISTS idx_graph_nodes_repo       ON graph_nodes(repository_id);
CREATE INDEX IF NOT EXISTS idx_graph_nodes_community  ON graph_nodes(repository_id, community);
CREATE INDEX IF NOT EXISTS idx_graph_nodes_label_trgm ON graph_nodes USING gin(norm_label gin_trgm_ops);

-- ─────────────────────────────────────────────────────────────────────────────
-- Graph edges — contains, calls, inherits, imports, etc.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS graph_edges (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id    UUID NOT NULL REFERENCES graph_repositories(id) ON DELETE CASCADE,
    source_node_id   UUID NOT NULL REFERENCES graph_nodes(id) ON DELETE CASCADE,
    target_node_id   UUID NOT NULL REFERENCES graph_nodes(id) ON DELETE CASCADE,
    relation         VARCHAR(64) NOT NULL,
    confidence       VARCHAR(32),
    confidence_score FLOAT DEFAULT 1.0,
    weight           FLOAT DEFAULT 1.0,
    source_file      VARCHAR(1024),
    source_location  VARCHAR(64),
    metadata         JSONB DEFAULT '{}',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_graph_edges_repo     ON graph_edges(repository_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_source   ON graph_edges(source_node_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_target   ON graph_edges(target_node_id);
CREATE INDEX IF NOT EXISTS idx_graph_edges_relation ON graph_edges(repository_id, relation);

-- ─────────────────────────────────────────────────────────────────────────────
-- Hyperedges — multi-node architectural groupings
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS graph_hyperedges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repository_id   UUID NOT NULL REFERENCES graph_repositories(id) ON DELETE CASCADE,
    label           VARCHAR(512) NOT NULL,
    description     TEXT,
    edge_type       VARCHAR(64),
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS graph_hyperedge_members (
    hyperedge_id    UUID NOT NULL REFERENCES graph_hyperedges(id) ON DELETE CASCADE,
    node_id         UUID NOT NULL REFERENCES graph_nodes(id) ON DELETE CASCADE,
    role            VARCHAR(64),
    PRIMARY KEY (hyperedge_id, node_id)
);

CREATE INDEX IF NOT EXISTS idx_hyperedge_repo         ON graph_hyperedges(repository_id);
CREATE INDEX IF NOT EXISTS idx_hyperedge_members_node ON graph_hyperedge_members(node_id);
