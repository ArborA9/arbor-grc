-- Wave 5 Doc 02: treatment custodian; incident ↔ gap assessment link.

ALTER TABLE treatment_plan ADD COLUMN IF NOT EXISTS custodian_user_id BIGINT;

ALTER TABLE incident_record ADD COLUMN IF NOT EXISTS linked_gap_assessment_id BIGINT;
