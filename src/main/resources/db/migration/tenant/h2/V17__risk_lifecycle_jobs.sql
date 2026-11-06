-- Path B3: Risk lifecycle job flags (OVERDUE + Tolerate review reminder).

ALTER TABLE risk_record ADD COLUMN overdue_notified_at TIMESTAMP;
ALTER TABLE risk_record ADD COLUMN review_reminder_sent_at TIMESTAMP;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('TREATMENT_OVERDUE',
 '[Action Required] - Treatment overdue - Risk',
 'Risk {{displayId}} ({{title}}) treatment is past target date {{targetDate}}. Status set to OVERDUE.',
 TRUE, TRUE, 1, TRUE),
('RISK_TOLERANCE_REVIEW_DUE',
 '[Action Required] - Tolerance review approaching - Risk',
 'Risk {{displayId}} ({{title}}) tolerance review is due on {{reviewDate}} (within 30 days).',
 TRUE, TRUE, 1, TRUE),
('RISK_TOLERANCE_RETURNED',
 '[Action Required] - Tolerance review date reached - Risk',
 'Risk {{displayId}} ({{title}}) returned to ASSIGNED for re-evaluation (review date {{reviewDate}}).',
 TRUE, TRUE, 1, TRUE);
