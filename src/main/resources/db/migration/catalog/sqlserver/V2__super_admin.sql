CREATE TABLE super_admin (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    email         NVARCHAR(255) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    full_name     NVARCHAR(255) NOT NULL,
    status        VARCHAR(32)   NOT NULL,
    created_at    DATETIME2     NOT NULL CONSTRAINT DF_super_admin_created_at DEFAULT SYSUTCDATETIME()
);
