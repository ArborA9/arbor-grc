-- Wave 5 Doc 02 R-14 — allow org_unit nesting beyond level 4.

ALTER TABLE org_unit DROP CONSTRAINT IF EXISTS ck_org_unit_level;
