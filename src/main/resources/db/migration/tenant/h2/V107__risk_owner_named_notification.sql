INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_OWNER_NAMED', 'You are the Risk Owner: {{displayId}}', 'You are the Risk Owner for {{displayId}} ({{title}}).', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_OWNER_NAMED');
