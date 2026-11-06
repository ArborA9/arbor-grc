-- TPRM client rebuild: named questionnaires, project fields, Other cert, evidence-mandatory flag.

CREATE TABLE tprm_questionnaire (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    name                NVARCHAR(200) NOT NULL,
    created_by_user_id  BIGINT         NOT NULL,
    created_at          DATETIME2      NOT NULL CONSTRAINT DF_tprm_questionnaire_created DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2      NOT NULL CONSTRAINT DF_tprm_questionnaire_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_tprm_questionnaire_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_tprm_questionnaire_name')
    CREATE INDEX idx_tprm_questionnaire_name ON tprm_questionnaire (name);
GO

CREATE TABLE tprm_questionnaire_item (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    questionnaire_id     BIGINT      NOT NULL,
    control_id           BIGINT      NOT NULL,
    evidence_mandatory   BIT         NOT NULL CONSTRAINT DF_tprm_qitem_ev_mand DEFAULT 0,
    sort_order           INT         NOT NULL CONSTRAINT DF_tprm_qitem_sort DEFAULT 0,
    CONSTRAINT fk_tprm_qitem_questionnaire FOREIGN KEY (questionnaire_id) REFERENCES tprm_questionnaire (id) ON DELETE CASCADE,
    CONSTRAINT fk_tprm_qitem_control FOREIGN KEY (control_id) REFERENCES tprm_control (id),
    CONSTRAINT uq_tprm_qitem_questionnaire_control UNIQUE (questionnaire_id, control_id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_tprm_qitem_questionnaire')
    CREATE INDEX idx_tprm_qitem_questionnaire ON tprm_questionnaire_item (questionnaire_id);
GO

IF COL_LENGTH('tprm_assessment', 'questionnaire_id') IS NULL
    ALTER TABLE tprm_assessment ADD questionnaire_id BIGINT NULL;
GO

IF COL_LENGTH('tprm_assessment', 'started_at') IS NULL
    ALTER TABLE tprm_assessment ADD started_at DATETIME2 NULL;
GO

IF COL_LENGTH('tprm_assessment_item', 'evidence_mandatory') IS NULL
    ALTER TABLE tprm_assessment_item ADD evidence_mandatory BIT NULL;
GO

IF COL_LENGTH('tprm_vendor_cert', 'other_label') IS NULL
    ALTER TABLE tprm_vendor_cert ADD other_label NVARCHAR(200) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_PROJECT_CLOSED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_PROJECT_CLOSED', N'[FYI] - TPRM project closed: {{assessmentTitle}}',
            N'The questionnaire "{{assessmentTitle}}" for {{vendorName}} is closed. Posture {{posturePercent}}%. Compliance: {{complianceStatus}}.',
            1, 1, 1, 1);
GO

UPDATE notification_template SET
    subject_template = N'[Action Required] - {{assessmentTitle}} has been assigned',
    body_template = N'The questionnaire "{{assessmentTitle}}" has been assigned to {{vendorName}}. Open the secure link: {{accessLink}} (expires {{expiresAt}}). A one-time code follows in a separate email.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_SENT' ORDER BY version DESC
    ) x
);
GO
