-- Wave 4 Doc 02: Asset CIA 1–5 (Excel Valuation), Application type, Other subtype text.

ALTER TABLE asset DROP CONSTRAINT IF EXISTS ck_asset_cia_c;
ALTER TABLE asset DROP CONSTRAINT IF EXISTS ck_asset_cia_i;
ALTER TABLE asset DROP CONSTRAINT IF EXISTS ck_asset_cia_a;

ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_c CHECK (confidentiality BETWEEN 1 AND 5);
ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_i CHECK (integrity_rating BETWEEN 1 AND 5);
ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_a CHECK (availability BETWEEN 1 AND 5);

ALTER TABLE asset ADD COLUMN IF NOT EXISTS asset_sub_type VARCHAR(128);

-- Risk residual scoring mode (R-06): CE×Inherent (default) or manual residual P×I.
ALTER TABLE risk_record ADD COLUMN IF NOT EXISTS residual_scoring_mode VARCHAR(40) NOT NULL DEFAULT 'CONTROL_EFFECTIVENESS';
ALTER TABLE risk_record ADD COLUMN IF NOT EXISTS residual_probability VARCHAR(16);
ALTER TABLE risk_record ADD COLUMN IF NOT EXISTS residual_impact VARCHAR(16);
