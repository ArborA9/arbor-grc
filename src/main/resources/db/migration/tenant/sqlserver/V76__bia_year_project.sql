-- BIA year project + Head workflow columns (SQL Server).

IF OBJECT_ID(N'bia_project', N'U') IS NULL
BEGIN
    CREATE TABLE bia_project (
        id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        assessment_year         INT          NOT NULL,
        name                    NVARCHAR(255) NOT NULL,
        status                  NVARCHAR(32)  NOT NULL CONSTRAINT df_bia_project_status DEFAULT N'OPEN',
        created_by_user_id      BIGINT       NOT NULL,
        created_at              DATETIME2    NOT NULL CONSTRAINT df_bia_project_created DEFAULT SYSUTCDATETIME(),
        updated_at              DATETIME2    NOT NULL CONSTRAINT df_bia_project_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uq_bia_project_year UNIQUE (assessment_year)
    );
    CREATE INDEX idx_bia_project_status ON bia_project (status);
END
GO

INSERT INTO bia_project (assessment_year, name, status, created_by_user_id, created_at, updated_at)
SELECT
    y.assessment_year,
    N'BIA ' + CAST(y.assessment_year AS NVARCHAR(10)),
    N'OPEN',
    COALESCE(y.created_by, 1),
    SYSUTCDATETIME(),
    SYSUTCDATETIME()
FROM (
    SELECT
        COALESCE(YEAR(c.issue_date), YEAR(c.created_at), YEAR(SYSUTCDATETIME())) AS assessment_year,
        MIN(c.created_by_user_id) AS created_by
    FROM bia_cycle c
    GROUP BY COALESCE(YEAR(c.issue_date), YEAR(c.created_at), YEAR(SYSUTCDATETIME()))
) y
WHERE NOT EXISTS (SELECT 1 FROM bia_project p WHERE p.assessment_year = y.assessment_year);
GO

IF COL_LENGTH('bia_cycle', 'bia_project_id') IS NULL
    ALTER TABLE bia_cycle ADD bia_project_id BIGINT NULL;
IF COL_LENGTH('bia_cycle', 'bc_head_user_id') IS NULL
    ALTER TABLE bia_cycle ADD bc_head_user_id BIGINT NULL;
IF COL_LENGTH('bia_cycle', 'bc_head_notes') IS NULL
    ALTER TABLE bia_cycle ADD bc_head_notes NVARCHAR(4000) NULL;
IF COL_LENGTH('bia_cycle', 'bc_head_decided_at') IS NULL
    ALTER TABLE bia_cycle ADD bc_head_decided_at DATETIME2 NULL;
IF COL_LENGTH('bia_cycle', 'admin_forward_notes') IS NULL
    ALTER TABLE bia_cycle ADD admin_forward_notes NVARCHAR(4000) NULL;
IF COL_LENGTH('bia_cycle', 'closed_at') IS NULL
    ALTER TABLE bia_cycle ADD closed_at DATETIME2 NULL;
IF COL_LENGTH('bia_cycle', 'closed_by_user_id') IS NULL
    ALTER TABLE bia_cycle ADD closed_by_user_id BIGINT NULL;
IF COL_LENGTH('bia_cycle', 'bc_head_work_task_id') IS NULL
    ALTER TABLE bia_cycle ADD bc_head_work_task_id BIGINT NULL;
GO

UPDATE c
SET bia_project_id = p.id
FROM bia_cycle c
INNER JOIN bia_project p
    ON p.assessment_year = COALESCE(YEAR(c.issue_date), YEAR(c.created_at), YEAR(SYSUTCDATETIME()))
WHERE c.bia_project_id IS NULL;
GO

UPDATE bia_cycle SET status = N'CLOSED' WHERE status = N'APPROVED';
GO

ALTER TABLE bia_cycle ALTER COLUMN bia_project_id BIGINT NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_bia_cycle_project')
    ALTER TABLE bia_cycle ADD CONSTRAINT fk_bia_cycle_project FOREIGN KEY (bia_project_id) REFERENCES bia_project (id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_bia_cycle_project')
    CREATE INDEX idx_bia_cycle_project ON bia_cycle (bia_project_id);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_PENDING_BC_HEAD')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_PENDING_BC_HEAD', N'BIA ready for BC Head: {{processName}}',
            N'Department BIA "{{processName}}" is waiting for Business Continuity Head review.', 1, 1, 1, 1);
IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_RETURNED_TO_ADMIN')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_RETURNED_TO_ADMIN', N'BIA returned by BC Head: {{processName}}',
            N'BC Head returned BIA "{{processName}}": {{reason}}', 1, 1, 1, 1);
IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_READY_TO_CLOSE')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_READY_TO_CLOSE', N'BIA approved by BC Head: {{processName}}',
            N'BC Head approved BIA "{{processName}}". Close the department BIA when ready.', 1, 1, 1, 1);
IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_CLOSED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_CLOSED', N'BIA closed: {{processName}}',
            N'Department BIA "{{processName}}" was closed.', 1, 1, 1, 1);
GO
