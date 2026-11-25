-- Super Admin local TOTP MFA (opt-in). One row per Super Admin; enabled only after confirm.

IF OBJECT_ID('super_admin_mfa_totp', 'U') IS NULL
BEGIN
    CREATE TABLE super_admin_mfa_totp (
        id               BIGINT IDENTITY(1,1) PRIMARY KEY,
        super_admin_id   BIGINT NOT NULL,
        secret_encrypted NVARCHAR(500) NOT NULL,
        enabled          BIT NOT NULL CONSTRAINT DF_super_admin_mfa_totp_enabled DEFAULT 0,
        created_at       DATETIME2 NOT NULL CONSTRAINT DF_super_admin_mfa_totp_created DEFAULT SYSUTCDATETIME(),
        confirmed_at     DATETIME2 NULL,
        last_used_at     DATETIME2 NULL,
        CONSTRAINT uq_super_admin_mfa_totp_admin UNIQUE (super_admin_id)
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'idx_super_admin_mfa_totp_admin'
      AND object_id = OBJECT_ID(N'super_admin_mfa_totp')
)
BEGIN
    CREATE INDEX idx_super_admin_mfa_totp_admin ON super_admin_mfa_totp (super_admin_id);
END
GO
