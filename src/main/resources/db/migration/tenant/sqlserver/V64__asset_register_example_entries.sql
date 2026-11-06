-- Asset Register — Example Entries fields (Register ⊂ Example; client workbook).
IF COL_LENGTH('asset', 'business_process_service') IS NULL
    ALTER TABLE asset ADD business_process_service NVARCHAR(512) NULL;
IF COL_LENGTH('asset', 'dependency_text') IS NULL
    ALTER TABLE asset ADD dependency_text NVARCHAR(512) NULL;
IF COL_LENGTH('asset', 'owner_job_title') IS NULL
    ALTER TABLE asset ADD owner_job_title NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'owner_department') IS NULL
    ALTER TABLE asset ADD owner_department NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'custodian_name') IS NULL
    ALTER TABLE asset ADD custodian_name NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'applied_classification') IS NULL
    ALTER TABLE asset ADD applied_classification NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'agreed_rto_hours') IS NULL
    ALTER TABLE asset ADD agreed_rto_hours DECIMAL(10, 2) NULL;
IF COL_LENGTH('asset', 'agreed_rpo_hours') IS NULL
    ALTER TABLE asset ADD agreed_rpo_hours DECIMAL(10, 2) NULL;
IF COL_LENGTH('asset', 'encrypted_at_rest') IS NULL
    ALTER TABLE asset ADD encrypted_at_rest NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'encrypted_in_transit') IS NULL
    ALTER TABLE asset ADD encrypted_in_transit NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'mfa_enforced') IS NULL
    ALTER TABLE asset ADD mfa_enforced NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'backed_up') IS NULL
    ALTER TABLE asset ADD backed_up NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'backup_frequency') IS NULL
    ALTER TABLE asset ADD backup_frequency NVARCHAR(128) NULL;
IF COL_LENGTH('asset', 'last_restore_test_date') IS NULL
    ALTER TABLE asset ADD last_restore_test_date DATE NULL;
IF COL_LENGTH('asset', 'siem_monitored') IS NULL
    ALTER TABLE asset ADD siem_monitored NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'vuln_scanned') IS NULL
    ALTER TABLE asset ADD vuln_scanned NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'last_patch_date') IS NULL
    ALTER TABLE asset ADD last_patch_date DATE NULL;
IF COL_LENGTH('asset', 'access_review_frequency') IS NULL
    ALTER TABLE asset ADD access_review_frequency NVARCHAR(128) NULL;
IF COL_LENGTH('asset', 'lifecycle_status') IS NULL
    ALTER TABLE asset ADD lifecycle_status NVARCHAR(64) NULL;
IF COL_LENGTH('asset', 'acquisition_go_live_date') IS NULL
    ALTER TABLE asset ADD acquisition_go_live_date DATE NULL;
IF COL_LENGTH('asset', 'warranty_end_date') IS NULL
    ALTER TABLE asset ADD warranty_end_date DATE NULL;
IF COL_LENGTH('asset', 'disposal_method') IS NULL
    ALTER TABLE asset ADD disposal_method NVARCHAR(512) NULL;
IF COL_LENGTH('asset', 'third_party_involved') IS NULL
    ALTER TABLE asset ADD third_party_involved NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'vendor_name') IS NULL
    ALTER TABLE asset ADD vendor_name NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'contract_sla_ref') IS NULL
    ALTER TABLE asset ADD contract_sla_ref NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'tprm_assessment_done') IS NULL
    ALTER TABLE asset ADD tprm_assessment_done NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'last_reviewed_date') IS NULL
    ALTER TABLE asset ADD last_reviewed_date DATE NULL;
IF COL_LENGTH('asset', 'reviewed_by') IS NULL
    ALTER TABLE asset ADD reviewed_by NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'change_ticket_ref') IS NULL
    ALTER TABLE asset ADD change_ticket_ref NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'notes') IS NULL
    ALTER TABLE asset ADD notes NVARCHAR(MAX) NULL;
