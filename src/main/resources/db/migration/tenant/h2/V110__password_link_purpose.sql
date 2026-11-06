-- Activation (first password, 24h) vs reset (1h) links. Both are link only: no code is emailed.

ALTER TABLE password_reset ADD COLUMN IF NOT EXISTS purpose VARCHAR(20) DEFAULT 'RESET' NOT NULL;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'ACCOUNT_ACTIVATION',
       'Activate your Arbor GRC account',
       'Your workspace {{workspaceId}} is ready. Activate your account by setting a password: {{actionLink}} This link is valid for 24 hours.',
       TRUE, FALSE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'ACCOUNT_ACTIVATION');

UPDATE notification_template
SET subject_template = 'Reset your Arbor GRC password',
    body_template = 'Reset your password for workspace {{workspaceId}}: {{actionLink}} This link is valid for 1 hour.'
WHERE trigger_type = 'PASSWORD_RESET';
