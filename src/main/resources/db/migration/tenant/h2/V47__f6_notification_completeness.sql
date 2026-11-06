-- F6 — notification lifecycle stamps + missing templates

ALTER TABLE bia ADD COLUMN IF NOT EXISTS review_due_notified_at TIMESTAMP;
ALTER TABLE bia ADD COLUMN IF NOT EXISTS not_submitted_notified_at TIMESTAMP;

ALTER TABLE recovery_plan ADD COLUMN IF NOT EXISTS review_due_notified_at TIMESTAMP;
ALTER TABLE recovery_plan ADD COLUMN IF NOT EXISTS expired_notified_at TIMESTAMP;

ALTER TABLE bcms_exercise ADD COLUMN IF NOT EXISTS due_notified_at TIMESTAMP;
ALTER TABLE bcms_exercise ADD COLUMN IF NOT EXISTS overdue_notified_at TIMESTAMP;

ALTER TABLE grc_audit ADD COLUMN IF NOT EXISTS start_notified_at TIMESTAMP;
ALTER TABLE grc_audit ADD COLUMN IF NOT EXISTS overdue_notified_at TIMESTAMP;
ALTER TABLE grc_audit ADD COLUMN IF NOT EXISTS report_pending_notified_at TIMESTAMP;

ALTER TABLE work_task ADD COLUMN IF NOT EXISTS not_responded_notified_at TIMESTAMP;

ALTER TABLE evidence ADD COLUMN IF NOT EXISTS expired_notified_at TIMESTAMP;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_NOT_RESPONDED', 'Gap control overdue: {{nodeCode}}', 'Control {{nodeCode}} ({{title}}) is past due ({{dueDate}}) and not submitted.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_NOT_RESPONDED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_FINDING_LOGGED', 'Audit finding: {{displayCode}}', 'Finding {{displayCode}} ({{severity}}) logged on audit "{{auditName}}": {{title}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_FINDING_LOGGED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_START_DATE', 'Audit starts today: {{name}}', 'Audit "{{name}}" ({{frameworkCode}}) start date is {{startDate}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_START_DATE');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_OVERDUE', 'Audit overdue: {{name}}', 'Audit "{{name}}" is past end date {{endDate}} (status {{status}}).', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_OVERDUE');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_REPORT_PENDING', 'Audit awaiting Admin sign-off: {{name}}', 'Audit "{{name}}" is submitted and awaiting Admin approval.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_REPORT_PENDING');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'EVIDENCE_EXPIRED', 'Evidence expired: {{title}}', 'Evidence "{{title}}" expired on {{expiresOn}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'EVIDENCE_EXPIRED');
