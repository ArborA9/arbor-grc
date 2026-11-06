ALTER TABLE risk_record ADD COLUMN final_risk_status VARCHAR(16);
ALTER TABLE risk_record ADD COLUMN target_residual_manual BOOLEAN NOT NULL DEFAULT FALSE;
