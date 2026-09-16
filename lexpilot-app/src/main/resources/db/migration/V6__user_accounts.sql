-- V6__user_accounts.sql
-- Backend user accounts for JWT authentication and NextAuth integration

CREATE TABLE user_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT NOT NULL UNIQUE,
    name TEXT,
    password_hash TEXT,
    role TEXT NOT NULL DEFAULT 'USER',
    tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_users_email ON user_accounts(email);
