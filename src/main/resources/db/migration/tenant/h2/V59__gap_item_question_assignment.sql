ALTER TABLE gap_item ADD COLUMN question_assignment_mode VARCHAR(16) DEFAULT 'ALL' NOT NULL;
ALTER TABLE gap_item ADD COLUMN assigned_framework_question_ids_json CLOB;
ALTER TABLE gap_item ADD COLUMN assigned_custom_question_ids_json CLOB;
