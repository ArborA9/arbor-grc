-- One CAPA register per calendar year. Existing per-audit rows keep their year and name.

IF COL_LENGTH('grc_capa_register', 'register_year') IS NULL
    ALTER TABLE grc_capa_register ADD register_year INT NULL;
GO

IF COL_LENGTH('grc_capa_register', 'name') IS NULL
    ALTER TABLE grc_capa_register ADD name NVARCHAR(200) NULL;
GO

UPDATE grc_capa_register
SET register_year = YEAR(created_at),
    name = CASE
        WHEN description IS NOT NULL AND LEN(LTRIM(RTRIM(description))) > 0 AND LEN(LTRIM(RTRIM(description))) <= 200
            THEN LTRIM(RTRIM(LEFT(description, 200)))
        ELSE N'CAPA Register ' + CAST(YEAR(created_at) AS NVARCHAR(8))
    END
WHERE register_year IS NULL OR name IS NULL;
GO

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'uq_capa_register_audit')
    ALTER TABLE grc_capa_register DROP CONSTRAINT uq_capa_register_audit;
GO

ALTER TABLE grc_capa_register ALTER COLUMN register_year INT NOT NULL;
ALTER TABLE grc_capa_register ALTER COLUMN name NVARCHAR(200) NOT NULL;
ALTER TABLE grc_capa_register ALTER COLUMN audit_id BIGINT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_capa_register_year')
    CREATE INDEX idx_capa_register_year ON grc_capa_register (register_year);
GO
