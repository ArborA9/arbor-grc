-- Save draft may omit CIA. Rated values stay 1-5. Derived score/class empty until rated.

ALTER TABLE asset DROP CONSTRAINT IF EXISTS ck_asset_cia_c;
ALTER TABLE asset DROP CONSTRAINT IF EXISTS ck_asset_cia_i;
ALTER TABLE asset DROP CONSTRAINT IF EXISTS ck_asset_cia_a;

ALTER TABLE asset ALTER COLUMN confidentiality DROP NOT NULL;
ALTER TABLE asset ALTER COLUMN integrity_rating DROP NOT NULL;
ALTER TABLE asset ALTER COLUMN availability DROP NOT NULL;
ALTER TABLE asset ALTER COLUMN cia_score DROP NOT NULL;
ALTER TABLE asset ALTER COLUMN criticality DROP NOT NULL;
ALTER TABLE asset ALTER COLUMN classification DROP NOT NULL;

ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_c CHECK (confidentiality IS NULL OR confidentiality BETWEEN 1 AND 5);
ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_i CHECK (integrity_rating IS NULL OR integrity_rating BETWEEN 1 AND 5);
ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_a CHECK (availability IS NULL OR availability BETWEEN 1 AND 5);
