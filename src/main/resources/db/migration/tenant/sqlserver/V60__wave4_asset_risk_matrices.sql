-- Wave 4 Doc 02: Asset CIA 1–5 (Excel Valuation), Application type, Other subtype text.

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_asset_cia_c')
    ALTER TABLE asset DROP CONSTRAINT ck_asset_cia_c;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_asset_cia_i')
    ALTER TABLE asset DROP CONSTRAINT ck_asset_cia_i;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_asset_cia_a')
    ALTER TABLE asset DROP CONSTRAINT ck_asset_cia_a;

ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_c CHECK (confidentiality BETWEEN 1 AND 5);
ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_i CHECK (integrity_rating BETWEEN 1 AND 5);
ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_a CHECK (availability BETWEEN 1 AND 5);

ALTER TABLE asset ADD asset_sub_type NVARCHAR(128) NULL;

ALTER TABLE risk_record ADD residual_scoring_mode NVARCHAR(40) NOT NULL
    CONSTRAINT DF_risk_residual_scoring_mode DEFAULT 'CONTROL_EFFECTIVENESS';
ALTER TABLE risk_record ADD residual_probability NVARCHAR(16) NULL;
ALTER TABLE risk_record ADD residual_impact NVARCHAR(16) NULL;
