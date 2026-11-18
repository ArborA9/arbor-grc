IF COL_LENGTH('dpia_department', 'ropa_process_id') IS NULL
BEGIN
    ALTER TABLE dpia_department ADD ropa_process_id BIGINT NULL;
END
GO

IF COL_LENGTH('dpia_dpo_assessment_row', 'create_privacy_risk') IS NULL
BEGIN
    ALTER TABLE dpia_dpo_assessment_row ADD create_privacy_risk BIT NULL;
END
GO

IF COL_LENGTH('privacy_ropa', 'personal_data_other') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD personal_data_other NVARCHAR(500) NULL;
END
GO

IF COL_LENGTH('privacy_ropa', 'sensitive_data_other') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD sensitive_data_other NVARCHAR(500) NULL;
END
GO

IF COL_LENGTH('privacy_ropa', 'data_subject_other') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD data_subject_other NVARCHAR(500) NULL;
END
GO

IF COL_LENGTH('privacy_ropa', 'data_subjects') IS NOT NULL
BEGIN
    ALTER TABLE privacy_ropa ALTER COLUMN data_subjects NVARCHAR(2000) NULL;
END
GO

IF COL_LENGTH('risk_record', 'processing_activity') IS NULL
BEGIN
    ALTER TABLE risk_record ADD processing_activity NVARCHAR(255) NULL;
END
GO

UPDATE notification_template
SET subject_template = N'Action Required | ROPA | Approve RoPA for {{department}}',
    body_template = N'The Record of Processing Activities (RoPA) for {{department}} has been completed and is ready for your review. Please review the submitted processing activities and either approve them or return them for updates where necessary.'
WHERE trigger_type = N'ROPA_HEAD_SIGN_OFF';
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DPIA_HEAD_RETURNED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DPIA_HEAD_RETURNED', N'Action Required | DPIA | Returned for assessment',
            N'The Department Head has returned the DPIA for {{department}} for further assessment. Please review the findings and update the compliance and risk assessment in Arbor GRC.',
            1, 1, 1, 1);
GO
