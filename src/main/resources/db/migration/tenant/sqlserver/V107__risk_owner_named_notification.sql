IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_OWNER_NAMED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_OWNER_NAMED', N'You are the Risk Owner: {{displayId}}', N'You are the Risk Owner for {{displayId}} ({{title}}).', 1, 1, 1, 1);
END;
GO
