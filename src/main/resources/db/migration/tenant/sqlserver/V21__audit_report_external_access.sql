-- Path C1: locked audit PDF storage key; Path C2: external auditor OTP invites.

ALTER TABLE grc_audit ADD report_storage_key VARCHAR(512) NULL;
ALTER TABLE grc_audit ADD report_generated_at DATETIME2 NULL;

CREATE TABLE grc_audit_external_access (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    audit_id           BIGINT        NOT NULL,
    invitee_name       NVARCHAR(255) NOT NULL,
    invitee_email      NVARCHAR(255) NOT NULL,
    token_hash         VARCHAR(128)  NOT NULL,
    otp_hash           VARCHAR(128)  NOT NULL,
    status             VARCHAR(32)   NOT NULL CONSTRAINT DF_grc_ext_access_status DEFAULT 'PENDING',
    expires_at         DATETIME2     NOT NULL,
    redeemed_at        DATETIME2     NULL,
    user_id            BIGINT        NULL,
    created_by_user_id BIGINT        NOT NULL,
    created_at         DATETIME2     NOT NULL CONSTRAINT DF_grc_ext_access_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_grc_ext_access_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_grc_ext_access_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_grc_ext_access_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_grc_ext_access_audit ON grc_audit_external_access (audit_id);
CREATE INDEX idx_grc_ext_access_token ON grc_audit_external_access (token_hash);
CREATE INDEX idx_grc_ext_access_email ON grc_audit_external_access (invitee_email);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
(N'AUDIT_EXTERNAL_INVITE_LINK',
 N'Audit access link: {{auditName}}',
 N'Hello {{inviteeName}}, you have been invited to access audit "{{auditName}}" in Arbor GRC. Open this secure link: {{accessLink}}. Your one-time code will arrive in a separate email.',
 1, 0, 1, 1),
(N'AUDIT_EXTERNAL_INVITE_OTP',
 N'Your audit access code: {{auditName}}',
 N'Hello {{inviteeName}}, your one-time code for audit "{{auditName}}" is: {{otpCode}}. This code expires on {{expiresAt}}.',
 1, 0, 1, 1);
