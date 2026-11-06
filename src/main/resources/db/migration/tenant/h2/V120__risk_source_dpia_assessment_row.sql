ALTER TABLE risk_record ADD COLUMN IF NOT EXISTS source_dpia_assessment_row_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_risk_source_dpia_assessment_row ON risk_record (source_dpia_assessment_row_id);
