-- Self-service local password reset (email OTP + one-hour link).

IF OBJECT_ID('password_reset', 'U') IS NULL
BEGIN
    CREATE TABLE password_reset (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id BIGINT NOT NULL,
        otp_hash VARCHAR(64) NOT NULL,
        link_token_hash VARCHAR(64) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        used_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL,
        CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES app_user(id)
    );
    CREATE UNIQUE INDEX ux_password_reset_link ON password_reset (link_token_hash);
    CREATE INDEX ix_password_reset_user ON password_reset (user_id);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'PASSWORD_RESET')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (
        N'PASSWORD_RESET',
        N'[Action Required] - Reset your Arbor GRC password',
        N'Your one-time code is {{otpCode}}. It expires in 1 hour. You can also open this link: {{resetLink}}',
        1, 0, 1, 1
    );
END;
