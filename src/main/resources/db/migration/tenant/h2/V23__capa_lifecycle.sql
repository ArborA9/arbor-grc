-- Path C A4: CAPA overdue lifecycle notification flags.

ALTER TABLE capa ADD COLUMN due_soon_notified_at TIMESTAMP;
ALTER TABLE capa ADD COLUMN overdue_notified_at TIMESTAMP;
ALTER TABLE capa ADD COLUMN escalation_notified_at TIMESTAMP;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('CAPA_DUE_SOON',
 'CAPA due soon: {{title}}',
 'CAPA "{{title}}" ({{controlRef}}) is due in 3 days on {{targetDate}}. Please complete corrective action.',
 TRUE, TRUE, 1, TRUE),
('CAPA_OVERDUE',
 '[Action Required] CAPA overdue: {{title}}',
 'CAPA "{{title}}" ({{controlRef}}) passed target date {{targetDate}}. Status set to OVERDUE.',
 TRUE, TRUE, 1, TRUE),
('CAPA_OVERDUE_ESCALATION',
 '[Escalation] CAPA overdue 7+ days: {{title}}',
 'CAPA "{{title}}" ({{controlRef}}) is 7+ days past target date {{targetDate}}. Immediate attention required.',
 TRUE, TRUE, 1, TRUE);
