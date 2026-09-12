-- V4__seed_default_api_key.sql
-- Seed the default API key used for local development and Docker Compose.
-- Key: 'dev-api-key-change-me'
-- SHA256 Base64: 'V4iNNsS7hnncKT8nH1i0hehIWCijyhkRGH3cuAQwZoM='

INSERT INTO api_keys (id, key_hash, tenant_id, label)
VALUES (
    gen_random_uuid(),
    'V4iNNsS7hnncKT8nH1i0hehIWCijyhkRGH3cuAQwZoM=',
    '00000000-0000-0000-0000-000000000000',
    'Default Dev Key'
) ON CONFLICT (key_hash) DO NOTHING;
