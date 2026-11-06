-- Copies of the catalog title taken when a project, SoA, Gap, policy, or audit was created.
UPDATE soa SET framework_name = 'ISO 20000' WHERE framework_code = 'ISO_20000';
UPDATE gap_assessment SET framework_name = 'ISO 20000' WHERE framework_code = 'ISO_20000';
UPDATE compliance_project_framework SET framework_name = 'ISO 20000' WHERE framework_code = 'ISO_20000';
UPDATE policy_framework SET framework_name = 'ISO 20000' WHERE framework_code = 'ISO_20000';
UPDATE grc_audit SET framework_name = 'ISO 20000' WHERE framework_code = 'ISO_20000';
