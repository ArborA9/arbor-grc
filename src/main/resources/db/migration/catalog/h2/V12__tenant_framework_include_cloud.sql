-- Framework Import V2 Phase 4: cloud scope on tenant activation.
ALTER TABLE tenant_framework ADD COLUMN include_cloud BOOLEAN NOT NULL DEFAULT FALSE;
