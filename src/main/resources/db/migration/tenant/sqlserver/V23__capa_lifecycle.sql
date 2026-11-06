-- Path C A4: CAPA overdue lifecycle notification flags.

ALTER TABLE capa ADD due_soon_notified_at DATETIME2;
ALTER TABLE capa ADD overdue_notified_at DATETIME2;
ALTER TABLE capa ADD escalation_notified_at DATETIME2;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('CAPA_DUE_SOON',
 'CAPA due soon: {{title}}',
 'CAPA "{{title}}" ({{controlRef}}) is due in 3 days on {{targetDate}}. Please complete corrective action.',
 1, 1, 1, 1),
('CAPA_OVERDUE',
 '[Action Required] CAPA overdue: {{title}}',
 'CAPA "{{title}}" ({{controlRef}}) passed target date {{targetDate}}. Status set to OVERDUE.',
 1, 1, 1, 1),
('CAPA_OVERDUE_ESCALATION',
 '[Escalation] CAPA overdue 7+ days: {{title}}',
 'CAPA "{{title}}" ({{controlRef}}) is 7+ days past target date {{targetDate}}. Immediate attention required.',
 1, 1, 1, 1);
