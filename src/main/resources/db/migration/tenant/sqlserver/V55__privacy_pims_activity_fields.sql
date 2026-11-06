-- Framework Import V2 Phase 7: PIMS Activity RoPA fields + DPIA pre-screening.
ALTER TABLE privacy_ropa ADD entity_name VARCHAR(255) NULL;
ALTER TABLE privacy_ropa ADD sector_name VARCHAR(255) NULL;
ALTER TABLE privacy_ropa ADD division_name VARCHAR(255) NULL;
ALTER TABLE privacy_ropa ADD section_name VARCHAR(255) NULL;
ALTER TABLE privacy_ropa ADD processing_description NVARCHAR(MAX) NULL;
ALTER TABLE privacy_ropa ADD sensitive_data_categories VARCHAR(1000) NULL;
ALTER TABLE privacy_ropa ADD data_subject_volume VARCHAR(64) NULL;
ALTER TABLE privacy_ropa ADD data_subject_volume_note VARCHAR(500) NULL;
ALTER TABLE privacy_ropa ADD processing_frequency VARCHAR(64) NULL;
ALTER TABLE privacy_ropa ADD processing_frequency_note VARCHAR(500) NULL;
ALTER TABLE privacy_ropa ADD source_of_personal_data VARCHAR(2000) NULL;
ALTER TABLE privacy_ropa ADD storage_systems VARCHAR(2000) NULL;
ALTER TABLE privacy_ropa ADD comments NVARCHAR(MAX) NULL;
GO

ALTER TABLE privacy_dpia ADD pre_screening_required BIT NOT NULL CONSTRAINT df_dpia_prescreen DEFAULT 0;
ALTER TABLE privacy_dpia ADD pre_screening_outcome VARCHAR(64) NULL;
ALTER TABLE privacy_dpia ADD pre_screening_notes NVARCHAR(MAX) NULL;
GO
