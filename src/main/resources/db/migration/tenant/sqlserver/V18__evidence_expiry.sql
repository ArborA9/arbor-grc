-- Path B4: Evidence expiry notify flag + EVIDENCE_EXPIRING template.

ALTER TABLE evidence ADD expiry_notified_at DATETIME2 NULL;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('EVIDENCE_EXPIRING',
 '[Action Required] - Evidence expiring soon',
 'Evidence "{{title}}" (id {{evidenceId}}) expires on {{expiresOn}}. Renew or replace before expiry.',
 1, 1, 1, 1);
