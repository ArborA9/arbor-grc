-- Framework Import V2 Phase 5: SAMA maturity (0–5) on Gap items + submissions.
ALTER TABLE gap_item ADD maturity INT NULL;
ALTER TABLE gap_item ADD final_maturity INT NULL;
GO

ALTER TABLE gap_item_submission ADD maturity INT NULL;
ALTER TABLE gap_item_submission ALTER COLUMN implementation_status VARCHAR(32) NULL;
GO
