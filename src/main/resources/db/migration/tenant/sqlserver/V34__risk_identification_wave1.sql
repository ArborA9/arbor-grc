-- Risk Wave 1: identification fidelity (§13.2) — SQL Server twin.

ALTER TABLE risk_record ADD uae_ia_priority NVARCHAR(8) NULL;
ALTER TABLE risk_record ADD display_id_overridden BIT NOT NULL CONSTRAINT df_risk_display_id_overridden DEFAULT 0;
ALTER TABLE risk_record ADD auto_close_override BIT NOT NULL CONSTRAINT df_risk_auto_close_override DEFAULT 0;
ALTER TABLE risk_record ADD auto_close_override_justification NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD source_gap_assessment_id BIGINT NULL;
ALTER TABLE risk_record ADD source_gap_item_id BIGINT NULL;
GO

CREATE INDEX idx_risk_source_gap ON risk_record (source_gap_assessment_id, source_gap_item_id);

CREATE TABLE risk_framework_mapping (
    id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    risk_id            BIGINT       NOT NULL,
    framework_id       BIGINT       NOT NULL,
    framework_code     NVARCHAR(64) NULL,
    framework_node_id  BIGINT       NULL,
    control_ref        NVARCHAR(100) NULL,
    custom_ref         NVARCHAR(512) NULL,
    created_at         DATETIME2    NOT NULL CONSTRAINT df_rfm_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_rfm_risk FOREIGN KEY (risk_id) REFERENCES risk_record (id)
);

CREATE INDEX idx_rfm_risk ON risk_framework_mapping (risk_id);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_AUTO_CLOSED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_AUTO_CLOSED',
            N'[FYI] - Risk auto-closed - Risk',
            N'Risk {{displayId}} ({{title}}) was auto-closed because residual risk is {{residualRisk}}. No treatment workflow is required.',
            1, 1, 1, 1);
END;
