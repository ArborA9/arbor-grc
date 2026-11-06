-- Optional free text when Data Residency is Other.
-- Azure SQL / UAT: GO after ADD before any later use of the column (error 207).
-- Do not ALTER COLUMN while a unique index or constraint depends on it (error 5074).

IF COL_LENGTH('asset', 'data_residency_other') IS NULL
BEGIN
    ALTER TABLE asset ADD data_residency_other NVARCHAR(255) NULL;
END
GO
