-- Wave: Asset Register redesign — client Information_Asset_Register Excel (Asset Register sheet).
-- GO: SQL Server cannot UPDATE/INDEX a column added in the same batch.

IF COL_LENGTH('asset', 'display_id') IS NULL
    ALTER TABLE asset ADD display_id NVARCHAR(32) NULL;
IF COL_LENGTH('asset', 'environment') IS NULL
    ALTER TABLE asset ADD environment NVARCHAR(64) NULL;
IF COL_LENGTH('asset', 'custodian_user_id') IS NULL
    ALTER TABLE asset ADD custodian_user_id BIGINT NULL;
IF COL_LENGTH('asset', 'country') IS NULL
    ALTER TABLE asset ADD country NVARCHAR(128) NULL;
IF COL_LENGTH('asset', 'site_building') IS NULL
    ALTER TABLE asset ADD site_building NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'hosting_model') IS NULL
    ALTER TABLE asset ADD hosting_model NVARCHAR(64) NULL;
IF COL_LENGTH('asset', 'cloud_provider_region') IS NULL
    ALTER TABLE asset ADD cloud_provider_region NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'data_residency') IS NULL
    ALTER TABLE asset ADD data_residency NVARCHAR(64) NULL;
IF COL_LENGTH('asset', 'hostname') IS NULL
    ALTER TABLE asset ADD hostname NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'ip_address') IS NULL
    ALTER TABLE asset ADD ip_address NVARCHAR(64) NULL;
IF COL_LENGTH('asset', 'asset_tag_serial') IS NULL
    ALTER TABLE asset ADD asset_tag_serial NVARCHAR(128) NULL;
IF COL_LENGTH('asset', 'platform_os_version') IS NULL
    ALTER TABLE asset ADD platform_os_version NVARCHAR(255) NULL;
IF COL_LENGTH('asset', 'confidentiality_justification') IS NULL
    ALTER TABLE asset ADD confidentiality_justification NVARCHAR(MAX) NULL;
IF COL_LENGTH('asset', 'integrity_justification') IS NULL
    ALTER TABLE asset ADD integrity_justification NVARCHAR(MAX) NULL;
IF COL_LENGTH('asset', 'availability_justification') IS NULL
    ALTER TABLE asset ADD availability_justification NVARCHAR(MAX) NULL;
IF COL_LENGTH('asset', 'aggregate_score') IS NULL
    ALTER TABLE asset ADD aggregate_score INT NULL;
IF COL_LENGTH('asset', 'weighted_score') IS NULL
    ALTER TABLE asset ADD weighted_score DECIMAL(6, 2) NULL;
IF COL_LENGTH('asset', 'end_of_life_date') IS NULL
    ALTER TABLE asset ADD end_of_life_date DATE NULL;
GO

UPDATE asset SET display_id = CONCAT(N'AST-', CAST(id AS NVARCHAR(20))) WHERE display_id IS NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ux_asset_display_id' AND object_id = OBJECT_ID('asset'))
    CREATE UNIQUE INDEX ux_asset_display_id ON asset(display_id) WHERE display_id IS NOT NULL;
