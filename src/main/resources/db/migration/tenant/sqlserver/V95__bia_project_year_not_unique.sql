-- Allow more than one BIA project per calendar year. Names stay unique in the API.

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'uq_bia_project_year')
    ALTER TABLE bia_project DROP CONSTRAINT uq_bia_project_year;
