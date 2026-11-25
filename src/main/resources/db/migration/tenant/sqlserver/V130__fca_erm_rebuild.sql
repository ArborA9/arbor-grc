-- FCA ERM rebuild (ERM only; Unified Risk unchanged)

IF COL_LENGTH('risk_record', 'taxonomy_l1') IS NULL
BEGIN
    ALTER TABLE risk_record ADD taxonomy_l1 NVARCHAR(128) NULL;
END
GO
IF COL_LENGTH('risk_record', 'taxonomy_l2') IS NULL
BEGIN
    ALTER TABLE risk_record ADD taxonomy_l2 NVARCHAR(128) NULL;
END
GO
IF COL_LENGTH('risk_record', 'taxonomy_l3') IS NULL
BEGIN
    ALTER TABLE risk_record ADD taxonomy_l3 NVARCHAR(256) NULL;
END
GO
IF COL_LENGTH('risk_record', 'taxonomy_other') IS NULL
BEGIN
    ALTER TABLE risk_record ADD taxonomy_other NVARCHAR(256) NULL;
END
GO
IF COL_LENGTH('risk_record', 'internal_causes') IS NULL
BEGIN
    ALTER TABLE risk_record ADD internal_causes NVARCHAR(MAX) NULL;
END
GO
IF COL_LENGTH('risk_record', 'potential_consequences') IS NULL
BEGIN
    ALTER TABLE risk_record ADD potential_consequences NVARCHAR(MAX) NULL;
END
GO
IF COL_LENGTH('risk_record', 'section_org_unit_id') IS NULL
BEGIN
    ALTER TABLE risk_record ADD section_org_unit_id BIGINT NULL;
END
GO
IF COL_LENGTH('risk_record', 'process_org_unit_id') IS NULL
BEGIN
    ALTER TABLE risk_record ADD process_org_unit_id BIGINT NULL;
END
GO
IF COL_LENGTH('risk_record', 'process_other') IS NULL
BEGIN
    ALTER TABLE risk_record ADD process_other NVARCHAR(256) NULL;
END
GO
IF COL_LENGTH('risk_record', 'driving_impact_dimension') IS NULL
BEGIN
    ALTER TABLE risk_record ADD driving_impact_dimension NVARCHAR(128) NULL;
END
GO
IF COL_LENGTH('risk_record', 'driving_impact_dimension_other') IS NULL
BEGIN
    ALTER TABLE risk_record ADD driving_impact_dimension_other NVARCHAR(256) NULL;
END
GO
IF COL_LENGTH('risk_record', 'consolidated_control_score') IS NULL
BEGIN
    ALTER TABLE risk_record ADD consolidated_control_score INT NULL;
END
GO
IF COL_LENGTH('risk_record', 'consolidated_control_label') IS NULL
BEGIN
    ALTER TABLE risk_record ADD consolidated_control_label NVARCHAR(64) NULL;
END
GO

IF COL_LENGTH('risk_project', 'version_label') IS NULL
BEGIN
    ALTER TABLE risk_project ADD version_label NVARCHAR(64) NULL;
END
GO
IF COL_LENGTH('risk_project', 'reviewed_by') IS NULL
BEGIN
    ALTER TABLE risk_project ADD reviewed_by NVARCHAR(256) NULL;
END
GO
IF COL_LENGTH('risk_project', 'approved_by') IS NULL
BEGIN
    ALTER TABLE risk_project ADD approved_by NVARCHAR(256) NULL;
END
GO
IF COL_LENGTH('risk_project', 'distribution_list') IS NULL
BEGIN
    ALTER TABLE risk_project ADD distribution_list NVARCHAR(MAX) NULL;
END
GO
IF COL_LENGTH('risk_project', 'next_review_date') IS NULL
BEGIN
    ALTER TABLE risk_project ADD next_review_date DATE NULL;
END
GO
IF COL_LENGTH('risk_project', 'assessment_notes') IS NULL
BEGIN
    ALTER TABLE risk_project ADD assessment_notes NVARCHAR(MAX) NULL;
END
GO

IF COL_LENGTH('org_profile', 'erm_auto_close_max_rating') IS NULL
BEGIN
    ALTER TABLE org_profile ADD erm_auto_close_max_rating NVARCHAR(16) NULL CONSTRAINT df_org_erm_auto_close DEFAULT 'VERY_LOW';
END
GO
IF COL_LENGTH('org_profile', 'erm_manual_residual_default') IS NULL
BEGIN
    ALTER TABLE org_profile ADD erm_manual_residual_default BIT NULL CONSTRAINT df_org_erm_manual_res DEFAULT 1;
END
GO

IF OBJECT_ID('risk_erm_control', 'U') IS NULL
BEGIN
    CREATE TABLE risk_erm_control (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        risk_record_id BIGINT NOT NULL,
        sort_order INT NOT NULL CONSTRAINT df_erm_ctrl_sort DEFAULT 0,
        description NVARCHAR(MAX) NOT NULL,
        design_rating NVARCHAR(64) NOT NULL,
        performance_rating NVARCHAR(64) NOT NULL,
        control_gap NVARCHAR(MAX) NULL,
        control_score INT NOT NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_risk_erm_control_risk' AND object_id = OBJECT_ID(N'risk_erm_control'))
    CREATE INDEX idx_risk_erm_control_risk ON risk_erm_control (risk_record_id);
GO

IF OBJECT_ID('risk_erm_treatment_action', 'U') IS NULL
BEGIN
    CREATE TABLE risk_erm_treatment_action (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        risk_record_id BIGINT NOT NULL,
        sort_order INT NOT NULL CONSTRAINT df_erm_act_sort DEFAULT 0,
        action_text NVARCHAR(MAX) NOT NULL,
        owner_user_id BIGINT NULL,
        dependency_org_unit_id BIGINT NULL,
        dependency_other NVARCHAR(256) NULL,
        target_date DATE NULL,
        status NVARCHAR(32) NOT NULL CONSTRAINT df_erm_act_status DEFAULT 'Pending',
        priority NVARCHAR(32) NULL,
        evidence_required NVARCHAR(MAX) NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_risk_erm_action_risk' AND object_id = OBJECT_ID(N'risk_erm_treatment_action'))
    CREATE INDEX idx_risk_erm_action_risk ON risk_erm_treatment_action (risk_record_id);
GO

IF OBJECT_ID('risk_erm_kri', 'U') IS NULL
BEGIN
    CREATE TABLE risk_erm_kri (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        risk_record_id BIGINT NOT NULL,
        sort_order INT NOT NULL CONSTRAINT df_erm_kri_sort DEFAULT 0,
        name NVARCHAR(256) NOT NULL,
        description NVARCHAR(MAX) NULL,
        calculation_method NVARCHAR(MAX) NULL,
        reporting_frequency NVARCHAR(64) NULL,
        threshold_green NVARCHAR(128) NULL,
        threshold_amber NVARCHAR(128) NULL,
        threshold_red NVARCHAR(128) NULL,
        month_01 NVARCHAR(64) NULL,
        month_02 NVARCHAR(64) NULL,
        month_03 NVARCHAR(64) NULL,
        month_04 NVARCHAR(64) NULL,
        month_05 NVARCHAR(64) NULL,
        month_06 NVARCHAR(64) NULL,
        month_07 NVARCHAR(64) NULL,
        month_08 NVARCHAR(64) NULL,
        month_09 NVARCHAR(64) NULL,
        month_10 NVARCHAR(64) NULL,
        month_11 NVARCHAR(64) NULL,
        month_12 NVARCHAR(64) NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL
    );
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_risk_erm_kri_risk' AND object_id = OBJECT_ID(N'risk_erm_kri'))
    CREATE INDEX idx_risk_erm_kri_risk ON risk_erm_kri (risk_record_id);
GO
