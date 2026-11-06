IF COL_LENGTH('risk_record', 'source_dpia_assessment_row_id') IS NULL
BEGIN
    ALTER TABLE risk_record ADD source_dpia_assessment_row_id BIGINT NULL;
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'idx_risk_source_dpia_assessment_row'
      AND object_id = OBJECT_ID(N'risk_record')
)
BEGIN
    CREATE INDEX idx_risk_source_dpia_assessment_row ON risk_record (source_dpia_assessment_row_id);
END
GO
