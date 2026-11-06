-- One CAPA register per calendar year. Existing per-audit rows keep their year and name.

ALTER TABLE grc_capa_register ADD COLUMN IF NOT EXISTS register_year INT;
ALTER TABLE grc_capa_register ADD COLUMN IF NOT EXISTS name VARCHAR(200);

UPDATE grc_capa_register
SET register_year = YEAR(created_at),
    name = CASE
        WHEN description IS NOT NULL AND LENGTH(TRIM(description)) > 0 AND LENGTH(TRIM(description)) <= 200
            THEN TRIM(description)
        ELSE CONCAT('CAPA Register ', CAST(YEAR(created_at) AS VARCHAR))
    END
WHERE register_year IS NULL OR name IS NULL;

ALTER TABLE grc_capa_register ALTER COLUMN register_year INT NOT NULL;
ALTER TABLE grc_capa_register ALTER COLUMN name VARCHAR(200) NOT NULL;

ALTER TABLE grc_capa_register ALTER COLUMN audit_id BIGINT NULL;
ALTER TABLE grc_capa_register DROP CONSTRAINT IF EXISTS uq_capa_register_audit;

CREATE INDEX IF NOT EXISTS idx_capa_register_year ON grc_capa_register (register_year);
