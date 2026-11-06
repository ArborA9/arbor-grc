-- Path C B1: Admin notified when SSO JIT auto-creates a Contributor.
IF NOT EXISTS (
    SELECT 1 FROM notification_template WHERE trigger_type = N'SSO_JIT_PROVISIONED' AND is_active = 1
)
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (
        N'SSO_JIT_PROVISIONED',
        N'SSO user auto-provisioned: {{email}}',
        N'A new {{role}} account was created via JIT SSO for {{email}}. Assign organisation units if needed.',
        1, 1, 1, 1
    );
END;
