-- Arbor UAT privacy feedback: DPIA per processing activity, assessment create-risk flag,
-- ROPA other-detail text, wider subject list, Privacy Risk processing activity.

ALTER TABLE dpia_department ADD COLUMN IF NOT EXISTS ropa_process_id BIGINT;

ALTER TABLE dpia_dpo_assessment_row ADD COLUMN IF NOT EXISTS create_privacy_risk BOOLEAN;

ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS personal_data_other VARCHAR(500);
ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS sensitive_data_other VARCHAR(500);
ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS data_subject_other VARCHAR(500);
ALTER TABLE privacy_ropa ALTER COLUMN data_subjects VARCHAR(2000);

ALTER TABLE risk_record ADD COLUMN IF NOT EXISTS processing_activity VARCHAR(255);

UPDATE notification_template
SET subject_template = 'Action Required | ROPA | Approve RoPA for {{department}}',
    body_template = 'The Record of Processing Activities (RoPA) for {{department}} has been completed and is ready for your review. Please review the submitted processing activities and either approve them or return them for updates where necessary.'
WHERE trigger_type = 'ROPA_HEAD_SIGN_OFF';

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'DPIA_HEAD_RETURNED',
       'Action Required | DPIA | Returned for assessment',
       'The Department Head has returned the DPIA for {{department}} for further assessment. Please review the findings and update the compliance and risk assessment in Arbor GRC.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'DPIA_HEAD_RETURNED');
