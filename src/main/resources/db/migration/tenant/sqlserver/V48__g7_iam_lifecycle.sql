-- G7 — refresh tokens + IAM lifecycle notification templates

IF OBJECT_ID('refresh_token', 'U') IS NULL
BEGIN
    CREATE TABLE refresh_token (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        token_hash VARCHAR(64) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL,
        revoked_at DATETIME2 NULL,
        CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user(id)
    );
    CREATE UNIQUE INDEX ux_refresh_token_hash ON refresh_token (token_hash);
    CREATE INDEX ix_refresh_token_user ON refresh_token (user_id);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'USER_DEACTIVATED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'USER_DEACTIVATED', N'Your Arbor GRC account was deactivated', N'Your account ({{email}}) has been deactivated. Contact your administrator if you believe this is an error.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ROLE_CHANGED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'ROLE_CHANGED', N'Your Arbor GRC roles were updated', N'Your roles are now: {{roles}}. Changes take effect on your next login.', 1, 1, 1, 1);
END;
