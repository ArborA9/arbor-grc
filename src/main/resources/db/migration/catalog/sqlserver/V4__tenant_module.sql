-- Per-tenant commercial module licences. All modules start disabled at provision.
CREATE TABLE tenant_module (
    id           BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id    VARCHAR(64)   NOT NULL,
    module_code  VARCHAR(32)   NOT NULL,
    is_enabled   BIT           NOT NULL CONSTRAINT DF_tenant_module_enabled DEFAULT 0,
    enabled_at   DATETIME2     NULL,
    expires_at   DATETIME2     NULL,
    CONSTRAINT uq_tenant_module UNIQUE (tenant_id, module_code)
);

CREATE INDEX idx_tenant_module_tenant ON tenant_module (tenant_id);
