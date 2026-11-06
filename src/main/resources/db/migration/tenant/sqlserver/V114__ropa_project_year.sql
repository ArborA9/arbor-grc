-- ROPA project assessment year (create screen dropdown, same pattern as BIA projects).

IF COL_LENGTH('ropa_project', 'assessment_year') IS NULL
BEGIN
    ALTER TABLE ropa_project ADD assessment_year INT NULL;
END
GO
