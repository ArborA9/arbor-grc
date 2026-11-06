IF COL_LENGTH('super_admin', 'must_change_password') IS NULL
BEGIN
    ALTER TABLE super_admin ADD must_change_password BIT NOT NULL
        CONSTRAINT DF_super_admin_must_change_password DEFAULT 0;
END
GO

-- One row per emergency reset code applied at startup (SHA-256 of the code), so a code works once.
IF OBJECT_ID('super_admin_reset', 'U') IS NULL
BEGIN
    CREATE TABLE super_admin_reset (
        id          BIGINT IDENTITY(1,1) PRIMARY KEY,
        code_hash   VARCHAR(64)   NOT NULL,
        email       NVARCHAR(255) NOT NULL,
        applied_at  DATETIME2     NOT NULL,
        CONSTRAINT uq_super_admin_reset_code UNIQUE (code_hash)
    );
END
GO
