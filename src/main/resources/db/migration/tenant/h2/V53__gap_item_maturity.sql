-- Framework Import V2 Phase 5: SAMA maturity (0–5) on Gap items + submissions.
ALTER TABLE gap_item ADD COLUMN maturity INT;
ALTER TABLE gap_item ADD COLUMN final_maturity INT;

ALTER TABLE gap_item_submission ADD COLUMN maturity INT;
ALTER TABLE gap_item_submission ALTER COLUMN implementation_status DROP NOT NULL;
