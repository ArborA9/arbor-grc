ALTER TABLE gap_item ADD question_assignment_mode NVARCHAR(16) NOT NULL
    CONSTRAINT DF_gap_item_q_assign_mode DEFAULT 'ALL';
ALTER TABLE gap_item ADD assigned_framework_question_ids_json NVARCHAR(MAX) NULL;
ALTER TABLE gap_item ADD assigned_custom_question_ids_json NVARCHAR(MAX) NULL;
