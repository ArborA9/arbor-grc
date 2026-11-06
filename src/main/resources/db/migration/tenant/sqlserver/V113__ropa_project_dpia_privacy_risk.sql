-- ROPA project journey (client Ropa Journey.docx + RoPA.xlsx), DPIA In Progress, Data Privacy Risk.
IF OBJECT_ID('ropa_project', 'U') IS NULL
BEGIN
    CREATE TABLE ropa_project (
        id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
        name                NVARCHAR(255) NOT NULL,
        status              NVARCHAR(32)  NOT NULL CONSTRAINT df_ropa_project_status DEFAULT N'DRAFT',
        created_by_user_id  BIGINT        NOT NULL,
        created_at          DATETIME2     NOT NULL CONSTRAINT df_ropa_project_created DEFAULT SYSUTCDATETIME(),
        updated_at          DATETIME2     NOT NULL CONSTRAINT df_ropa_project_updated DEFAULT SYSUTCDATETIME()
    );
END
GO

IF OBJECT_ID('ropa_department', 'U') IS NULL
BEGIN
    CREATE TABLE ropa_department (
        id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
        ropa_project_id         BIGINT         NOT NULL,
        org_unit_id             BIGINT         NOT NULL,
        department_head_user_id BIGINT         NULL,
        champion_user_id        BIGINT         NULL,
        due_date                DATE           NULL,
        status                  NVARCHAR(32)   NOT NULL CONSTRAINT df_ropa_dept_status DEFAULT N'NOT_STARTED',
        return_from             NVARCHAR(16)   NULL,
        return_reason           NVARCHAR(4000) NULL,
        work_task_id            BIGINT         NULL,
        head_work_task_id       BIGINT         NULL,
        head_notes              NVARCHAR(4000) NULL,
        head_decided_at         DATETIME2      NULL,
        closed_at               DATETIME2      NULL,
        closed_by_user_id       BIGINT         NULL,
        created_by_user_id      BIGINT         NOT NULL,
        created_at              DATETIME2      NOT NULL CONSTRAINT df_ropa_dept_created DEFAULT SYSUTCDATETIME(),
        updated_at              DATETIME2      NOT NULL CONSTRAINT df_ropa_dept_updated DEFAULT SYSUTCDATETIME()
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_ropa_dept_project' AND object_id = OBJECT_ID('ropa_department'))
    CREATE INDEX idx_ropa_dept_project ON ropa_department (ropa_project_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_ropa_dept_champion' AND object_id = OBJECT_ID('ropa_department'))
    CREATE INDEX idx_ropa_dept_champion ON ropa_department (champion_user_id);
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_ropa_dept_head' AND object_id = OBJECT_ID('ropa_department'))
    CREATE INDEX idx_ropa_dept_head ON ropa_department (department_head_user_id);
GO

IF COL_LENGTH('privacy_ropa', 'ropa_department_id') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD ropa_department_id BIGINT NULL;
END
GO
IF COL_LENGTH('privacy_ropa', 'review_outcome') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD review_outcome NVARCHAR(32) NULL;
END
GO
IF COL_LENGTH('privacy_ropa', 'legal_bases_json') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD legal_bases_json NVARCHAR(MAX) NULL;
END
GO
IF COL_LENGTH('privacy_ropa', 'adequacy_note') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD adequacy_note NVARCHAR(2000) NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_privacy_ropa_dept' AND object_id = OBJECT_ID('privacy_ropa'))
    CREATE INDEX idx_privacy_ropa_dept ON privacy_ropa (ropa_department_id);
GO

IF COL_LENGTH('risk_record', 'source_dpia_id') IS NULL
BEGIN
    ALTER TABLE risk_record ADD source_dpia_id BIGINT NULL;
END
GO
IF COL_LENGTH('risk_record', 'source_ropa_id') IS NULL
BEGIN
    ALTER TABLE risk_record ADD source_ropa_id BIGINT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_risk_source_dpia' AND object_id = OBJECT_ID('risk_record'))
    CREATE INDEX idx_risk_source_dpia ON risk_record (source_dpia_id);
GO
