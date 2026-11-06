-- Path C B1: Admin notified when SSO JIT auto-creates a Contributor.
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'SSO_JIT_PROVISIONED',
       'SSO user auto-provisioned: {{email}}',
       'A new {{role}} account was created via JIT SSO for {{email}}. Assign organisation units if needed.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM notification_template WHERE trigger_type = 'SSO_JIT_PROVISIONED' AND is_active = TRUE
);
