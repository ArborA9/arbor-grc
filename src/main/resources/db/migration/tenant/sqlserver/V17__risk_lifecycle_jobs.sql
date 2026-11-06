-- Path B3: Risk lifecycle job flags (OVERDUE + Tolerate review reminder).

ALTER TABLE risk_record ADD overdue_notified_at DATETIME2 NULL;
ALTER TABLE risk_record ADD review_reminder_sent_at DATETIME2 NULL;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('TREATMENT_OVERDUE',
 '[Action Required] - Treatment overdue - Risk',
 'Risk {{displayId}} ({{title}}) treatment is past target date {{targetDate}}. Status set to OVERDUE.',
 1, 1, 1, 1),
('RISK_TOLERANCE_REVIEW_DUE',
 '[Action Required] - Tolerance review approaching - Risk',
 'Risk {{displayId}} ({{title}}) tolerance review is due on {{reviewDate}} (within 30 days).',
 1, 1, 1, 1),
('RISK_TOLERANCE_RETURNED',
 '[Action Required] - Tolerance review date reached - Risk',
 'Risk {{displayId}} ({{title}}) returned to ASSIGNED for re-evaluation (review date {{reviewDate}}).',
 1, 1, 1, 1);
