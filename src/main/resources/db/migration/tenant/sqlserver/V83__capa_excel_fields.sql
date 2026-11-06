-- V83: Add missing CAPA fields per Excel Audit_Module_MVP_v2.xlsx CAPA Tracker sheet
-- Columns J, O-T from Excel

-- J: Date Assigned (when CAPA was assigned to contributor)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('capa') AND name = 'assigned_at')
    ALTER TABLE capa ADD assigned_at DATETIME2;

-- O: Response Description (Contributor fills)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('capa') AND name = 'response_description')
    ALTER TABLE capa ADD response_description NVARCHAR(MAX);

-- P: Upload Evidence note (actual files via evidence library)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('capa') AND name = 'contributor_evidence_note')
    ALTER TABLE capa ADD contributor_evidence_note NVARCHAR(MAX);

-- Q: Corrective Action plan review remarks (Admin fills)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('capa') AND name = 'review_remarks')
    ALTER TABLE capa ADD review_remarks NVARCHAR(MAX);

-- R: Evidence of Completion (Admin fills)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('capa') AND name = 'evidence_of_completion')
    ALTER TABLE capa ADD evidence_of_completion NVARCHAR(MAX);

-- S: Verification Date (Admin fills)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('capa') AND name = 'verification_date')
    ALTER TABLE capa ADD verification_date DATE;

-- T: Verification Result (Admin fills - PASS, FAIL, PARTIAL, etc.)
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('capa') AND name = 'verification_result')
    ALTER TABLE capa ADD verification_result NVARCHAR(32);
