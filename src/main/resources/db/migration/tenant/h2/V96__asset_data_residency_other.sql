-- Optional free text when Data Residency is Other.
ALTER TABLE asset ADD COLUMN IF NOT EXISTS data_residency_other VARCHAR(255);
