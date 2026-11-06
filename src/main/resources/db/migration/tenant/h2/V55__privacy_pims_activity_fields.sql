-- Framework Import V2 Phase 7: PIMS Activity RoPA fields + DPIA pre-screening.
ALTER TABLE privacy_ropa ADD COLUMN entity_name VARCHAR(255);
ALTER TABLE privacy_ropa ADD COLUMN sector_name VARCHAR(255);
ALTER TABLE privacy_ropa ADD COLUMN division_name VARCHAR(255);
ALTER TABLE privacy_ropa ADD COLUMN section_name VARCHAR(255);
ALTER TABLE privacy_ropa ADD COLUMN processing_description CLOB;
ALTER TABLE privacy_ropa ADD COLUMN sensitive_data_categories VARCHAR(1000);
ALTER TABLE privacy_ropa ADD COLUMN data_subject_volume VARCHAR(64);
ALTER TABLE privacy_ropa ADD COLUMN data_subject_volume_note VARCHAR(500);
ALTER TABLE privacy_ropa ADD COLUMN processing_frequency VARCHAR(64);
ALTER TABLE privacy_ropa ADD COLUMN processing_frequency_note VARCHAR(500);
ALTER TABLE privacy_ropa ADD COLUMN source_of_personal_data VARCHAR(2000);
ALTER TABLE privacy_ropa ADD COLUMN storage_systems VARCHAR(2000);
ALTER TABLE privacy_ropa ADD COLUMN comments CLOB;

ALTER TABLE privacy_dpia ADD COLUMN pre_screening_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE privacy_dpia ADD COLUMN pre_screening_outcome VARCHAR(64);
ALTER TABLE privacy_dpia ADD COLUMN pre_screening_notes CLOB;
