-- Tenant checklist library (Excel SoT sheets) + CAPA link to Audit Project.

CREATE TABLE grc_audit_checklist_library (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    name                NVARCHAR(200) NOT NULL,
    template_key        NVARCHAR(32)  NOT NULL,
    system_seeded       BIT           NOT NULL CONSTRAINT DF_audit_clib_seeded DEFAULT 0,
    created_by_user_id  BIGINT        NOT NULL,
    created_at          DATETIME2     NOT NULL CONSTRAINT DF_audit_clib_created DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2     NOT NULL CONSTRAINT DF_audit_clib_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_audit_clib_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_audit_clib_name')
    CREATE INDEX idx_audit_clib_name ON grc_audit_checklist_library (name);
GO

CREATE TABLE grc_audit_checklist_library_item (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    library_id        BIGINT         NOT NULL,
    sort_order        INT            NOT NULL CONSTRAINT DF_audit_clib_item_sort DEFAULT 0,
    clause_ref        NVARCHAR(80)   NOT NULL,
    section           NVARCHAR(120)  NULL,
    title             NVARCHAR(512)  NOT NULL,
    test_focus        NVARCHAR(MAX)  NULL,
    soa_applicability NVARCHAR(120)  NULL,
    evidence_lives    NVARCHAR(300)  NULL,
    created_at        DATETIME2      NOT NULL CONSTRAINT DF_audit_clib_item_created DEFAULT SYSUTCDATETIME(),
    updated_at        DATETIME2      NOT NULL CONSTRAINT DF_audit_clib_item_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_audit_clib_item_library FOREIGN KEY (library_id)
        REFERENCES grc_audit_checklist_library (id) ON DELETE CASCADE
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_audit_clib_item_library')
    CREATE INDEX idx_audit_clib_item_library ON grc_audit_checklist_library_item (library_id);
GO

IF COL_LENGTH('capa', 'audit_id') IS NULL
    ALTER TABLE capa ADD audit_id BIGINT NULL;
GO

IF COL_LENGTH('capa', 'conducted_by') IS NULL
    ALTER TABLE capa ADD conducted_by NVARCHAR(200) NULL;
GO
