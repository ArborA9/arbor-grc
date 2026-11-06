CREATE TABLE tenant_framework_entitlement (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    tenant_id NVARCHAR(64) NOT NULL,
    framework_id BIGINT NOT NULL,
    entitled_at DATETIMEOFFSET NOT NULL,
    entitled_by NVARCHAR(255) NULL,
    CONSTRAINT uq_tenant_framework_entitlement UNIQUE (tenant_id, framework_id)
);

CREATE INDEX idx_tfe_tenant ON tenant_framework_entitlement (tenant_id);
CREATE INDEX idx_tfe_framework ON tenant_framework_entitlement (framework_id);
