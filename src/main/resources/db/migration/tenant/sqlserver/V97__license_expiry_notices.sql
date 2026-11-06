-- Licence expiry: richer email bodies, HTML part, Admin banner dismiss, reminder templates.
-- Azure SQL: GO after ADD before later use of the column (error 207).

IF COL_LENGTH('notification_email_outbox', 'body_html') IS NULL
BEGIN
    ALTER TABLE notification_email_outbox ADD body_html NVARCHAR(MAX) NULL;
END
GO

ALTER TABLE notification_email_outbox ALTER COLUMN body NVARCHAR(MAX) NOT NULL;
GO

ALTER TABLE notification_template ALTER COLUMN body_template NVARCHAR(MAX) NOT NULL;
GO

IF OBJECT_ID(N'license_banner_dismiss', N'U') IS NULL
BEGIN
    CREATE TABLE license_banner_dismiss (
        id            BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id       BIGINT       NOT NULL,
        banner_key    VARCHAR(128) NOT NULL,
        dismissed_at  DATETIME2    NOT NULL CONSTRAINT DF_license_banner_dismiss_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uq_license_banner_dismiss UNIQUE (user_id, banner_key),
        CONSTRAINT fk_license_banner_dismiss_user FOREIGN KEY (user_id) REFERENCES app_user (id)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'LICENSE_EXPIRY_REMINDER')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (
        N'LICENSE_EXPIRY_REMINDER',
        N'[Action Required] - Arbor licence expires {{whenLabel}}',
        N'{{moduleList}} licence for workspace {{workspaceId}} expires on {{expiresOn}} ({{whenLabel}}). Sign in to review modules. Contact Arbor to extend.',
        0, 1, 1, 1
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'LICENSE_EXPIRED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (
        N'LICENSE_EXPIRED',
        N'[Action Required] - Arbor licence expired',
        N'{{moduleList}} licence for workspace {{workspaceId}} expired on {{expiresOn}}. The workspace is view-only until Arbor extends the licence.',
        0, 1, 1, 1
    );
END
GO
