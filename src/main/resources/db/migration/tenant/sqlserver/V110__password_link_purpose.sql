-- Activation (first password, 24h) vs reset (1h) links. Both are link only: no code is emailed.

IF COL_LENGTH('password_reset', 'purpose') IS NULL
BEGIN
    ALTER TABLE password_reset ADD purpose VARCHAR(20) NOT NULL
        CONSTRAINT df_password_reset_purpose DEFAULT 'RESET';
END
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ACCOUNT_ACTIVATION')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (
        N'ACCOUNT_ACTIVATION',
        N'Activate your Arbor GRC account',
        N'Your workspace {{workspaceId}} is ready. Activate your account by setting a password: {{actionLink}} This link is valid for 24 hours.',
        1, 0, 1, 1
    );
END;
GO

UPDATE notification_template
SET subject_template = N'Reset your Arbor GRC password',
    body_template = N'Reset your password for workspace {{workspaceId}}: {{actionLink}} This link is valid for 1 hour.'
WHERE trigger_type = N'PASSWORD_RESET';
GO
