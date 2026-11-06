-- Reminder ledger for commercial module licence expiry (catalog-wide, per tenant + module).
IF OBJECT_ID(N'tenant_module_expiry_notice', N'U') IS NULL
BEGIN
    CREATE TABLE tenant_module_expiry_notice (
        id           BIGINT IDENTITY(1,1) PRIMARY KEY,
        tenant_id    VARCHAR(64)  NOT NULL,
        module_code  VARCHAR(32)  NOT NULL,
        milestone    VARCHAR(16)  NOT NULL,
        expires_on   DATE         NOT NULL,
        sent_at      DATETIME2    NOT NULL,
        CONSTRAINT uq_tenant_module_expiry_notice UNIQUE (tenant_id, module_code, milestone, expires_on)
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = N'idx_tmen_tenant' AND object_id = OBJECT_ID(N'tenant_module_expiry_notice')
)
    CREATE INDEX idx_tmen_tenant ON tenant_module_expiry_notice (tenant_id);
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = N'idx_tmen_expires' AND object_id = OBJECT_ID(N'tenant_module_expiry_notice')
)
    CREATE INDEX idx_tmen_expires ON tenant_module_expiry_notice (expires_on);
GO
