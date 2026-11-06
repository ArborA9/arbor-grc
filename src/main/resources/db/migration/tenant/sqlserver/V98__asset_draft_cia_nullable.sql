-- Save draft may omit CIA. Rated values stay 1-5. Derived score/class empty until rated.
-- Azure SQL: drop CHECKs before ALTER COLUMN (error 5074). GO between DDL batches.

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'ck_asset_cia_c')
    ALTER TABLE asset DROP CONSTRAINT ck_asset_cia_c;
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'ck_asset_cia_i')
    ALTER TABLE asset DROP CONSTRAINT ck_asset_cia_i;
GO
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'ck_asset_cia_a')
    ALTER TABLE asset DROP CONSTRAINT ck_asset_cia_a;
GO

ALTER TABLE asset ALTER COLUMN confidentiality INT NULL;
GO
ALTER TABLE asset ALTER COLUMN integrity_rating INT NULL;
GO
ALTER TABLE asset ALTER COLUMN availability INT NULL;
GO
ALTER TABLE asset ALTER COLUMN cia_score DECIMAL(4, 2) NULL;
GO
ALTER TABLE asset ALTER COLUMN criticality VARCHAR(32) NULL;
GO
ALTER TABLE asset ALTER COLUMN classification VARCHAR(32) NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'ck_asset_cia_c')
    ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_c CHECK (confidentiality IS NULL OR confidentiality BETWEEN 1 AND 5);
GO
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'ck_asset_cia_i')
    ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_i CHECK (integrity_rating IS NULL OR integrity_rating BETWEEN 1 AND 5);
GO
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'ck_asset_cia_a')
    ALTER TABLE asset ADD CONSTRAINT ck_asset_cia_a CHECK (availability IS NULL OR availability BETWEEN 1 AND 5);
GO
