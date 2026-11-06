-- DPIA Phase B: PIMS screening Q1–7, methodology JSON, Mode B assign.

ALTER TABLE privacy_dpia ADD COLUMN intake_mode VARCHAR(32) NOT NULL DEFAULT 'FORM';
ALTER TABLE privacy_dpia ADD COLUMN assigned_contributor_id BIGINT;
ALTER TABLE privacy_dpia ADD COLUMN work_task_id BIGINT;
ALTER TABLE privacy_dpia ADD COLUMN assigned_at TIMESTAMP;

ALTER TABLE privacy_dpia ADD COLUMN screening_q1 VARCHAR(2000);
ALTER TABLE privacy_dpia ADD COLUMN screening_q2 VARCHAR(2000);
ALTER TABLE privacy_dpia ADD COLUMN screening_q3 BOOLEAN;
ALTER TABLE privacy_dpia ADD COLUMN screening_q4 BOOLEAN;
ALTER TABLE privacy_dpia ADD COLUMN screening_q5 BOOLEAN;
ALTER TABLE privacy_dpia ADD COLUMN screening_q6 BOOLEAN;
ALTER TABLE privacy_dpia ADD COLUMN screening_q7 BOOLEAN;
ALTER TABLE privacy_dpia ADD COLUMN full_dpia_mandatory BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE privacy_dpia ADD COLUMN questions_json CLOB;
ALTER TABLE privacy_dpia ADD COLUMN assessment_json CLOB;
ALTER TABLE privacy_dpia ADD COLUMN overview_risks_json CLOB;

CREATE INDEX idx_privacy_dpia_intake ON privacy_dpia (intake_mode);
CREATE INDEX idx_privacy_dpia_assignee ON privacy_dpia (assigned_contributor_id);
