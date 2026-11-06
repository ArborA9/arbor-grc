-- Track who added a BIA process so Champions cannot delete Admin-created rows.
-- GO: SQL Server cannot INDEX a column added in the same batch.

IF COL_LENGTH('bia_process', 'created_by_user_id') IS NULL
    ALTER TABLE bia_process ADD created_by_user_id BIGINT NULL;
IF COL_LENGTH('bia_process', 'created_by_admin') IS NULL
    ALTER TABLE bia_process ADD created_by_admin BIT NOT NULL CONSTRAINT df_bia_process_created_by_admin DEFAULT 1;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_bia_process_created_by' AND object_id = OBJECT_ID(N'bia_process'))
    CREATE INDEX idx_bia_process_created_by ON bia_process (created_by_user_id);
