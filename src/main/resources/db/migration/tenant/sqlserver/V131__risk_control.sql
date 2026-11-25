IF OBJECT_ID(N'risk_control', N'U') IS NULL
BEGIN
    CREATE TABLE risk_control (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        risk_id BIGINT NOT NULL,
        sort_order INT NOT NULL CONSTRAINT df_risk_control_sort DEFAULT 0,
        description NVARCHAR(2000) NOT NULL,
        design NVARCHAR(32) NOT NULL,
        performance NVARCHAR(32) NOT NULL,
        control_gap NVARCHAR(4000) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_risk_control_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_risk_control_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT fk_risk_control_risk FOREIGN KEY (risk_id) REFERENCES risk_record(id)
    );
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'idx_risk_control_risk'
      AND object_id = OBJECT_ID(N'risk_control')
)
BEGIN
    CREATE INDEX idx_risk_control_risk ON risk_control (risk_id);
END
GO
