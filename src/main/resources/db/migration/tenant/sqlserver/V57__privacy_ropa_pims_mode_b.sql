-- RoPA Phase A: PIMS SSD fields, UAE legal-basis remap, DPIA status enum, Mode B assign.
-- GO splits batches: SQL Server cannot UPDATE/INDEX a column added in the same batch.

ALTER TABLE privacy_ropa ADD method_of_receiving VARCHAR(2000) NULL;
ALTER TABLE privacy_ropa ADD share_internal BIT NOT NULL CONSTRAINT DF_privacy_ropa_share_int DEFAULT 0;
ALTER TABLE privacy_ropa ADD share_internal_details VARCHAR(2000) NULL;
ALTER TABLE privacy_ropa ADD share_operator BIT NOT NULL CONSTRAINT DF_privacy_ropa_share_op DEFAULT 0;
ALTER TABLE privacy_ropa ADD share_operator_details VARCHAR(2000) NULL;
ALTER TABLE privacy_ropa ADD share_external BIT NOT NULL CONSTRAINT DF_privacy_ropa_share_ext DEFAULT 0;
ALTER TABLE privacy_ropa ADD share_external_details VARCHAR(2000) NULL;
ALTER TABLE privacy_ropa ADD external_entity_location VARCHAR(500) NULL;
ALTER TABLE privacy_ropa ADD adequate_protection BIT NULL;
ALTER TABLE privacy_ropa ADD cross_border_transfer_basis VARCHAR(2000) NULL;
ALTER TABLE privacy_ropa ADD safeguards_implemented VARCHAR(2000) NULL;
ALTER TABLE privacy_ropa ADD representative_name VARCHAR(255) NULL;
ALTER TABLE privacy_ropa ADD representative_employee_id VARCHAR(64) NULL;
ALTER TABLE privacy_ropa ADD dpia_status VARCHAR(32) NOT NULL CONSTRAINT DF_privacy_ropa_dpia_st DEFAULT 'NOT_REQUIRED';
ALTER TABLE privacy_ropa ADD linked_dpia_id BIGINT NULL;
ALTER TABLE privacy_ropa ADD intake_mode VARCHAR(32) NOT NULL CONSTRAINT DF_privacy_ropa_intake DEFAULT 'FORM';
ALTER TABLE privacy_ropa ADD assigned_contributor_id BIGINT NULL;
ALTER TABLE privacy_ropa ADD work_task_id BIGINT NULL;
ALTER TABLE privacy_ropa ADD assigned_at DATETIME2 NULL;
GO

UPDATE privacy_ropa SET dpia_status = 'REQUIRED' WHERE dpia_required = 1;
UPDATE privacy_ropa SET dpia_status = 'NOT_REQUIRED' WHERE dpia_required = 0;

UPDATE privacy_ropa SET legal_basis = 'CONTRACT_PERFORMANCE' WHERE legal_basis = 'CONTRACT';
UPDATE privacy_ropa SET legal_basis = 'COMPLIANCE_WITH_OTHER_LAWS' WHERE legal_basis = 'LEGAL_OBLIGATION';
UPDATE privacy_ropa SET legal_basis = 'OTHER_CASES_AS_PER_REGULATIONS' WHERE legal_basis = 'LEGITIMATE_INTEREST';
UPDATE privacy_ropa SET legal_basis = 'PROTECTING_DATA_SUBJECT_INTERESTS' WHERE legal_basis = 'VITAL_INTEREST';
UPDATE privacy_ropa SET legal_basis = 'PROTECT_PUBLIC_INTEREST' WHERE legal_basis = 'PUBLIC_TASK';
GO

CREATE INDEX idx_privacy_ropa_intake ON privacy_ropa (intake_mode);
CREATE INDEX idx_privacy_ropa_assignee ON privacy_ropa (assigned_contributor_id);
CREATE INDEX idx_privacy_ropa_dpia_status ON privacy_ropa (dpia_status);
