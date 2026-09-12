-- V3__multi_tenancy.sql
-- Add tenant_id to core tables and enable Row-Level Security

-- 1. Add tenant_id columns with a default "system" tenant for existing data
ALTER TABLE documents 
  ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

ALTER TABLE document_chunks 
  ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

ALTER TABLE conversations 
  ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

-- (We don't need tenant_id on conversation_messages since it inherits via conversation_id,
-- but we could add it for simpler RLS. We'll join through conversations in the policy).

-- 2. Create Indexes for tenant-scoped queries
CREATE INDEX idx_documents_tenant ON documents(tenant_id);
CREATE INDEX idx_chunks_tenant ON document_chunks(tenant_id);
CREATE INDEX idx_conversations_tenant ON conversations(tenant_id);

-- 3. Enable Row-Level Security
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversation_messages ENABLE ROW LEVEL SECURITY;

-- 4. Create Policies tied to the custom 'app.current_tenant' session variable
-- The backend sets this variable via a Hibernate interceptor per transaction.

CREATE POLICY tenant_documents ON documents
  FOR ALL
  USING (tenant_id = current_setting('app.current_tenant')::uuid)
  WITH CHECK (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_chunks ON document_chunks
  FOR ALL
  USING (tenant_id = current_setting('app.current_tenant')::uuid)
  WITH CHECK (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_conversations ON conversations
  FOR ALL
  USING (tenant_id = current_setting('app.current_tenant')::uuid)
  WITH CHECK (tenant_id = current_setting('app.current_tenant')::uuid);

CREATE POLICY tenant_messages ON conversation_messages
  FOR ALL
  USING (
    conversation_id IN (
      SELECT id FROM conversations WHERE tenant_id = current_setting('app.current_tenant')::uuid
    )
  )
  WITH CHECK (
    conversation_id IN (
      SELECT id FROM conversations WHERE tenant_id = current_setting('app.current_tenant')::uuid
    )
  );

-- 5. Create API Keys table for tenant resolution
CREATE TABLE api_keys (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  key_hash TEXT NOT NULL UNIQUE,
  tenant_id UUID NOT NULL,
  label TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);
