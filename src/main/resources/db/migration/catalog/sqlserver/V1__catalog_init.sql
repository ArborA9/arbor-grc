CREATE TABLE tenant (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id   VARCHAR(64)   NOT NULL UNIQUE,
    name        NVARCHAR(255) NOT NULL,
    status      VARCHAR(32)   NOT NULL,
    jdbc_url    NVARCHAR(1000) NOT NULL,
    db_username NVARCHAR(255) NOT NULL,
    db_password NVARCHAR(500) NOT NULL,
    created_at  DATETIME2     NOT NULL CONSTRAINT DF_tenant_created_at DEFAULT SYSUTCDATETIME(),
    updated_at  DATETIME2     NULL
);
