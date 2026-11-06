-- Unfinished IS / ERM create forms. Not a risk record: no Risk ID, no status, not in register counts.
IF OBJECT_ID('risk_form_draft', 'U') IS NULL
BEGIN
    CREATE TABLE risk_form_draft (
        id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
        risk_type               NVARCHAR(16)  NOT NULL,
        risk_project_id         BIGINT        NOT NULL,
        title                   NVARCHAR(500) NULL,
        payload_json            NVARCHAR(MAX) NOT NULL,
        created_by_user_id      BIGINT        NOT NULL,
        created_at              DATETIME2     NOT NULL CONSTRAINT df_rfd_created DEFAULT SYSUTCDATETIME(),
        updated_at              DATETIME2     NOT NULL CONSTRAINT df_rfd_updated DEFAULT SYSUTCDATETIME()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_rfd_owner_project' AND object_id = OBJECT_ID('risk_form_draft'))
    CREATE INDEX idx_rfd_owner_project ON risk_form_draft (created_by_user_id, risk_type, risk_project_id);
GO
