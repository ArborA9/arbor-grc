-- Wave 5 Doc 02: treatment custodian; incident ↔ gap assessment link.

IF COL_LENGTH('treatment_plan', 'custodian_user_id') IS NULL
    ALTER TABLE treatment_plan ADD custodian_user_id BIGINT NULL;

IF COL_LENGTH('incident_record', 'linked_gap_assessment_id') IS NULL
    ALTER TABLE incident_record ADD linked_gap_assessment_id BIGINT NULL;
