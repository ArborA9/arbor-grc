-- DPIA Phase B: PIMS screening Q1–7, methodology JSON, Mode B assign.
-- GO after ADD so indexes on new columns are a separate batch (SQL Server).

ALTER TABLE privacy_dpia ADD intake_mode VARCHAR(32) NOT NULL CONSTRAINT DF_privacy_dpia_intake DEFAULT 'FORM';
ALTER TABLE privacy_dpia ADD assigned_contributor_id BIGINT NULL;
ALTER TABLE privacy_dpia ADD work_task_id BIGINT NULL;
ALTER TABLE privacy_dpia ADD assigned_at DATETIME2 NULL;

ALTER TABLE privacy_dpia ADD screening_q1 VARCHAR(2000) NULL;
ALTER TABLE privacy_dpia ADD screening_q2 VARCHAR(2000) NULL;
ALTER TABLE privacy_dpia ADD screening_q3 BIT NULL;
ALTER TABLE privacy_dpia ADD screening_q4 BIT NULL;
ALTER TABLE privacy_dpia ADD screening_q5 BIT NULL;
ALTER TABLE privacy_dpia ADD screening_q6 BIT NULL;
ALTER TABLE privacy_dpia ADD screening_q7 BIT NULL;
ALTER TABLE privacy_dpia ADD full_dpia_mandatory BIT NOT NULL CONSTRAINT DF_privacy_dpia_full DEFAULT 0;

ALTER TABLE privacy_dpia ADD questions_json NVARCHAR(MAX) NULL;
ALTER TABLE privacy_dpia ADD assessment_json NVARCHAR(MAX) NULL;
ALTER TABLE privacy_dpia ADD overview_risks_json NVARCHAR(MAX) NULL;
GO

CREATE INDEX idx_privacy_dpia_intake ON privacy_dpia (intake_mode);
CREATE INDEX idx_privacy_dpia_assignee ON privacy_dpia (assigned_contributor_id);
