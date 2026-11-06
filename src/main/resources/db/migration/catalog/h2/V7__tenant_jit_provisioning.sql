-- Path C B1: JIT provisioning settings on the tenant's IdP config.
ALTER TABLE tenant ADD COLUMN jit_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant ADD COLUMN jit_default_role VARCHAR(32) NOT NULL DEFAULT 'BLOCK';
