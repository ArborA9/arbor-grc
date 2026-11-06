-- RoPA Phase A: PIMS SSD fields, UAE legal-basis remap, DPIA status enum, Mode B assign.

ALTER TABLE privacy_ropa ADD COLUMN method_of_receiving VARCHAR(2000);
ALTER TABLE privacy_ropa ADD COLUMN share_internal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE privacy_ropa ADD COLUMN share_internal_details VARCHAR(2000);
ALTER TABLE privacy_ropa ADD COLUMN share_operator BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE privacy_ropa ADD COLUMN share_operator_details VARCHAR(2000);
ALTER TABLE privacy_ropa ADD COLUMN share_external BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE privacy_ropa ADD COLUMN share_external_details VARCHAR(2000);
ALTER TABLE privacy_ropa ADD COLUMN external_entity_location VARCHAR(500);
ALTER TABLE privacy_ropa ADD COLUMN adequate_protection BOOLEAN;
ALTER TABLE privacy_ropa ADD COLUMN cross_border_transfer_basis VARCHAR(2000);
ALTER TABLE privacy_ropa ADD COLUMN safeguards_implemented VARCHAR(2000);
ALTER TABLE privacy_ropa ADD COLUMN representative_name VARCHAR(255);
ALTER TABLE privacy_ropa ADD COLUMN representative_employee_id VARCHAR(64);
ALTER TABLE privacy_ropa ADD COLUMN dpia_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED';
ALTER TABLE privacy_ropa ADD COLUMN linked_dpia_id BIGINT;
ALTER TABLE privacy_ropa ADD COLUMN intake_mode VARCHAR(32) NOT NULL DEFAULT 'FORM';
ALTER TABLE privacy_ropa ADD COLUMN assigned_contributor_id BIGINT;
ALTER TABLE privacy_ropa ADD COLUMN work_task_id BIGINT;
ALTER TABLE privacy_ropa ADD COLUMN assigned_at TIMESTAMP;

UPDATE privacy_ropa SET dpia_status = 'REQUIRED' WHERE dpia_required = TRUE;
UPDATE privacy_ropa SET dpia_status = 'NOT_REQUIRED' WHERE dpia_required = FALSE;

UPDATE privacy_ropa SET legal_basis = 'CONTRACT_PERFORMANCE' WHERE legal_basis = 'CONTRACT';
UPDATE privacy_ropa SET legal_basis = 'COMPLIANCE_WITH_OTHER_LAWS' WHERE legal_basis = 'LEGAL_OBLIGATION';
UPDATE privacy_ropa SET legal_basis = 'OTHER_CASES_AS_PER_REGULATIONS' WHERE legal_basis = 'LEGITIMATE_INTEREST';
UPDATE privacy_ropa SET legal_basis = 'PROTECTING_DATA_SUBJECT_INTERESTS' WHERE legal_basis = 'VITAL_INTEREST';
UPDATE privacy_ropa SET legal_basis = 'PROTECT_PUBLIC_INTEREST' WHERE legal_basis = 'PUBLIC_TASK';

CREATE INDEX idx_privacy_ropa_intake ON privacy_ropa (intake_mode);
CREATE INDEX idx_privacy_ropa_assignee ON privacy_ropa (assigned_contributor_id);
CREATE INDEX idx_privacy_ropa_dpia_status ON privacy_ropa (dpia_status);
