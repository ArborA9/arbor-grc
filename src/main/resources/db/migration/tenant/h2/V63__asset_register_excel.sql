-- Wave: Asset Register redesign — client Information_Asset_Register Excel (Asset Register sheet).
ALTER TABLE asset ADD COLUMN IF NOT EXISTS display_id VARCHAR(32);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS environment VARCHAR(64);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS custodian_user_id BIGINT;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS country VARCHAR(128);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS site_building VARCHAR(255);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS hosting_model VARCHAR(64);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS cloud_provider_region VARCHAR(255);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS data_residency VARCHAR(64);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS hostname VARCHAR(255);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS ip_address VARCHAR(64);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS asset_tag_serial VARCHAR(128);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS platform_os_version VARCHAR(255);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS confidentiality_justification CLOB;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS integrity_justification CLOB;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS availability_justification CLOB;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS aggregate_score INT;
ALTER TABLE asset ADD COLUMN IF NOT EXISTS weighted_score DECIMAL(6, 2);
ALTER TABLE asset ADD COLUMN IF NOT EXISTS end_of_life_date DATE;

UPDATE asset SET display_id = CONCAT('AST-', CAST(id AS VARCHAR)) WHERE display_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_asset_display_id ON asset(display_id);
