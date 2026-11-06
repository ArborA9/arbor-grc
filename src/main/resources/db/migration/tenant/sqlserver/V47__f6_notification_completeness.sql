-- F6 — notification lifecycle stamps + missing templates

IF COL_LENGTH('bia', 'review_due_notified_at') IS NULL
    ALTER TABLE bia ADD review_due_notified_at DATETIME2 NULL;
IF COL_LENGTH('bia', 'not_submitted_notified_at') IS NULL
    ALTER TABLE bia ADD not_submitted_notified_at DATETIME2 NULL;

IF COL_LENGTH('recovery_plan', 'review_due_notified_at') IS NULL
    ALTER TABLE recovery_plan ADD review_due_notified_at DATETIME2 NULL;
IF COL_LENGTH('recovery_plan', 'expired_notified_at') IS NULL
    ALTER TABLE recovery_plan ADD expired_notified_at DATETIME2 NULL;

IF COL_LENGTH('bcms_exercise', 'due_notified_at') IS NULL
    ALTER TABLE bcms_exercise ADD due_notified_at DATETIME2 NULL;
IF COL_LENGTH('bcms_exercise', 'overdue_notified_at') IS NULL
    ALTER TABLE bcms_exercise ADD overdue_notified_at DATETIME2 NULL;

IF COL_LENGTH('grc_audit', 'start_notified_at') IS NULL
    ALTER TABLE grc_audit ADD start_notified_at DATETIME2 NULL;
IF COL_LENGTH('grc_audit', 'overdue_notified_at') IS NULL
    ALTER TABLE grc_audit ADD overdue_notified_at DATETIME2 NULL;
IF COL_LENGTH('grc_audit', 'report_pending_notified_at') IS NULL
    ALTER TABLE grc_audit ADD report_pending_notified_at DATETIME2 NULL;

IF COL_LENGTH('work_task', 'not_responded_notified_at') IS NULL
    ALTER TABLE work_task ADD not_responded_notified_at DATETIME2 NULL;

IF COL_LENGTH('evidence', 'expired_notified_at') IS NULL
    ALTER TABLE evidence ADD expired_notified_at DATETIME2 NULL;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_NOT_RESPONDED')
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES ('GAP_NOT_RESPONDED', 'Gap control overdue: {{nodeCode}}', 'Control {{nodeCode}} ({{title}}) is past due ({{dueDate}}) and not submitted.', 1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_FINDING_LOGGED')
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES ('AUDIT_FINDING_LOGGED', 'Audit finding: {{displayCode}}', 'Finding {{displayCode}} ({{severity}}) logged on audit "{{auditName}}": {{title}}.', 1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_START_DATE')
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES ('AUDIT_START_DATE', 'Audit starts today: {{name}}', 'Audit "{{name}}" ({{frameworkCode}}) start date is {{startDate}}.', 1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_OVERDUE')
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES ('AUDIT_OVERDUE', 'Audit overdue: {{name}}', 'Audit "{{name}}" is past end date {{endDate}} (status {{status}}).', 1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_REPORT_PENDING')
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES ('AUDIT_REPORT_PENDING', 'Audit awaiting Admin sign-off: {{name}}', 'Audit "{{name}}" is submitted and awaiting Admin approval.', 1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'EVIDENCE_EXPIRED')
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES ('EVIDENCE_EXPIRED', 'Evidence expired: {{title}}', 'Evidence "{{title}}" expired on {{expiresOn}}.', 1, 1, 1, 1);
