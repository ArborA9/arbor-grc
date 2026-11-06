ALTER TABLE risk_record ADD final_risk_status VARCHAR(16);
ALTER TABLE risk_record ADD target_residual_manual BIT NOT NULL CONSTRAINT df_risk_target_manual DEFAULT 0;
