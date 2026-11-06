-- Risk Wave 4: tenant-customisable risk matrices (§13.3 B6/B7) — SQL Server twin.

CREATE TABLE risk_matrix_config (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    inherent_json        NVARCHAR(MAX) NOT NULL,
    residual_json        NVARCHAR(MAX) NOT NULL,
    version              INT           NOT NULL CONSTRAINT df_rmc_version DEFAULT 1,
    change_reason        NVARCHAR(1000) NULL,
    updated_by_user_id   BIGINT        NULL,
    updated_at           DATETIME2     NOT NULL CONSTRAINT df_rmc_updated DEFAULT SYSUTCDATETIME()
);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_MATRIX_CHANGED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_MATRIX_CHANGED', N'[FYI] - Risk matrix updated - Risk',
            N'The tenant risk matrices were updated (version {{version}}). Reason: {{reason}}. Existing risk scores are not recalculated.', 1, 1, 1, 1);
END;
