-- ROPA project assessment year (create screen dropdown, same pattern as BIA projects).

ALTER TABLE ropa_project ADD COLUMN IF NOT EXISTS assessment_year INT;
