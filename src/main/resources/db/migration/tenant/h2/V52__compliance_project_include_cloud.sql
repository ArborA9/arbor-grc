-- Framework Import V2 Phase 4: cloud scope on compliance projects.
ALTER TABLE compliance_project ADD COLUMN include_cloud BOOLEAN NOT NULL DEFAULT FALSE;
