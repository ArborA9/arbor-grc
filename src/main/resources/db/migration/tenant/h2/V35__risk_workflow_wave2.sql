-- Risk Wave 2: workflow fidelity — InfoSec status, chase/escalation stamps, notification catalog.

ALTER TABLE risk_record ADD infosec_status_initial CLOB;
ALTER TABLE risk_record ADD infosec_status_contributor CLOB;
ALTER TABLE risk_record ADD infosec_status_final CLOB;
ALTER TABLE risk_record ADD assignment_chase_sent_at TIMESTAMP;
ALTER TABLE risk_record ADD owner_chase_sent_at TIMESTAMP;
ALTER TABLE risk_record ADD overdue_escalation_sent_at TIMESTAMP;

ALTER TABLE treatment_milestone ADD progress_notes CLOB;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_NOT_RESPONDED',
       '[Action Required] - Risk not responded - Risk',
       'Risk {{displayId}} ({{title}}) has had no Contributor action for 3+ days since assignment.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_NOT_RESPONDED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_RETURNED',
       '[Action Required] - Risk returned - Risk',
       'Risk {{displayId}} ({{title}}) was returned for revision. Reason: {{reason}}',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_RETURNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_OWNER_NOT_RESPONDED',
       '[Action Required] - Risk Owner not responded - Risk',
       'Risk {{displayId}} ({{title}}) awaits Risk Owner acceptance — no action for 5+ days.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_OWNER_NOT_RESPONDED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_OWNER_REJECTED',
       '[Action Required] - Risk Owner rejected - Risk',
       'Risk {{displayId}} ({{title}}) acceptance was rejected by the Risk Owner. Reason: {{reason}}',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_OWNER_REJECTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'ADMIN_FINAL_DECISION',
       '[FYI] - Admin final decision - Risk',
       'Risk {{displayId}} ({{title}}) — Admin decision: {{decision}}. {{detail}}',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'ADMIN_FINAL_DECISION');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_RE_OPENED',
       '[Action Required] - Risk re-opened - Risk',
       'Risk {{displayId}} ({{title}}) was re-opened. Reason: {{reason}}',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_RE_OPENED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_OVERDUE_ESCALATION',
       '[Escalation] - Risk overdue 7 days - Risk',
       'Risk {{displayId}} ({{title}}) has been overdue for 7+ days and is escalated to the Owner.',
       TRUE, FALSE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_OVERDUE_ESCALATION');
