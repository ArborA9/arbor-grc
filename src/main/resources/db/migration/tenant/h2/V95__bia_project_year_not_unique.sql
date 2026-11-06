-- Allow more than one BIA project per calendar year. Names stay unique in the API.

ALTER TABLE bia_project DROP CONSTRAINT IF EXISTS uq_bia_project_year;
