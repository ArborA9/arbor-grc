-- Path C B4: local TOTP MFA (Owner/Admin) + MFA-verified flag on the audit trail.

ALTER TABLE audit_log ADD mfa_verified BIT NULL;

CREATE TABLE user_mfa_totp (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    secret_encrypted NVARCHAR(500) NOT NULL,
    enabled          BIT NOT NULL CONSTRAINT DF_user_mfa_totp_enabled DEFAULT 0,
    created_at       DATETIME2 NOT NULL CONSTRAINT DF_user_mfa_totp_created DEFAULT SYSUTCDATETIME(),
    confirmed_at     DATETIME2 NULL,
    last_used_at     DATETIME2 NULL,
    CONSTRAINT uq_user_mfa_totp_user UNIQUE (user_id)
);

CREATE INDEX idx_user_mfa_totp_user ON user_mfa_totp (user_id);
