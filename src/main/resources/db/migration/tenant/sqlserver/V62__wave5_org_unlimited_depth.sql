-- Wave 5 Doc 02 R-14 — allow org_unit nesting beyond level 4.

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_org_unit_level')
    ALTER TABLE org_unit DROP CONSTRAINT ck_org_unit_level;
