-- V83: Add missing CAPA fields per Excel Audit_Module_MVP_v2.xlsx CAPA Tracker sheet
-- Columns J, O-T from Excel

-- J: Date Assigned (when CAPA was assigned to contributor)
ALTER TABLE capa ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP;

-- O: Response Description (Contributor fills)
ALTER TABLE capa ADD COLUMN IF NOT EXISTS response_description CLOB;

-- P: Upload Evidence note (actual files via evidence library)
ALTER TABLE capa ADD COLUMN IF NOT EXISTS contributor_evidence_note CLOB;

-- Q: Corrective Action plan review remarks (Admin fills)
ALTER TABLE capa ADD COLUMN IF NOT EXISTS review_remarks CLOB;

-- R: Evidence of Completion (Admin fills)
ALTER TABLE capa ADD COLUMN IF NOT EXISTS evidence_of_completion CLOB;

-- S: Verification Date (Admin fills)
ALTER TABLE capa ADD COLUMN IF NOT EXISTS verification_date DATE;

-- T: Verification Result (Admin fills - PASS, FAIL, PARTIAL, etc.)
ALTER TABLE capa ADD COLUMN IF NOT EXISTS verification_result VARCHAR(32);
