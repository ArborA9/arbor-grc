-- Excel Next Review Date (AL) reminder — SQL Server twin.

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_REVIEW_DUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_REVIEW_DUE', N'[Action Required] - Risk review due - Risk',
            N'Risk {{displayId}} ({{title}}) Next Review Date {{reviewDate}} is within 30 days.', 1, 1, 1, 1);
END;
