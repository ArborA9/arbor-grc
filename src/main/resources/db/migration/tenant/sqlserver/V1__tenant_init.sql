CREATE TABLE ping_record (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    note       NVARCHAR(255) NOT NULL,
    created_at DATETIME2     NOT NULL CONSTRAINT DF_ping_record_created_at DEFAULT SYSUTCDATETIME()
);
