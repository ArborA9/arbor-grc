-- Snapshot of vendor answer state at Return-to-vendor hand-off.
-- Resubmit requires a change vs this fingerprint (answer, justification, evidence).
ALTER TABLE tprm_assessment_item ADD COLUMN return_fingerprint VARCHAR(512);
