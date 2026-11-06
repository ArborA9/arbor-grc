-- Path C B5: ensure every fired trigger has a template (SQL Server).

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'USER_INVITED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'USER_INVITED', N'You are invited to Arbor GRC', N'Hello {{email}}, you have been invited. Sign in to activate your account.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'SSO_NO_ROLE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'SSO_NO_ROLE', N'SSO login blocked: {{email}}', N'An SSO user ({{email}}) could not sign in — no Arbor account / role. Invite them or enable JIT.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'SSO_JIT_PROVISIONED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'SSO_JIT_PROVISIONED', N'SSO user auto-provisioned: {{email}}', N'A new {{role}} account was created via JIT SSO for {{email}}. Assign organisation units if needed.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'LOCAL_AUTH_USED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'LOCAL_AUTH_USED', N'Local login while SSO is enabled', N'{{email}} signed in with a local password while SSO is enabled for this workspace.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'GAP_CONTROL_ASSIGNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'GAP_CONTROL_ASSIGNED', N'Gap control assigned: {{nodeCode}}', N'Control {{nodeCode}} on assessment {{assessmentName}} was assigned to you.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'GAP_CONTROL_SUBMITTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'GAP_CONTROL_SUBMITTED', N'Gap control submitted: {{nodeCode}}', N'Contributor submitted {{nodeCode}} on {{assessmentName}} for review.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'GAP_CONTROL_SENT_BACK')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'GAP_CONTROL_SENT_BACK', N'Gap control sent back: {{nodeCode}}', N'Admin sent back {{nodeCode}}: {{reason}}', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'GAP_CONTROL_FINALIZED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'GAP_CONTROL_FINALIZED', N'Gap control finalized: {{nodeCode}}', N'Admin finalized {{nodeCode}} on {{assessmentName}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_ASSIGNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_ASSIGNED', N'Risk assigned: {{displayId}}', N'Risk {{displayId}} ({{title}}) was assigned to you.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_SUBMITTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_SUBMITTED', N'Risk submitted: {{displayId}}', N'Contributor submitted risk {{displayId}} for validation.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_VALIDATION_REJECTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_VALIDATION_REJECTED', N'Risk validation rejected: {{displayId}}', N'Risk {{displayId}} was sent back for revision.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_PENDING_ADMIN')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_PENDING_ADMIN', N'Risk pending Admin: {{displayId}}', N'Risk {{displayId}} awaits Admin decision.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_PENDING_OWNER')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_PENDING_OWNER', N'Risk pending Owner: {{displayId}}', N'Risk {{displayId}} awaits Risk Owner acceptance.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_TOLERATED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_TOLERATED', N'Risk tolerated: {{displayId}}', N'Risk {{displayId}} was accepted / tolerated.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_MITIGATING')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_MITIGATING', N'Risk mitigating: {{displayId}}', N'Risk {{displayId}} treatment is in progress.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'TREATMENT_OVERDUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'TREATMENT_OVERDUE', N'Treatment overdue: {{displayId}}', N'Treatment for {{displayId}} is past target date {{targetDate}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_TOLERANCE_REVIEW_DUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_TOLERANCE_REVIEW_DUE', N'Tolerance review due: {{displayId}}', N'Tolerated risk {{displayId}} is approaching its review date.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_TOLERANCE_RETURNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_TOLERANCE_RETURNED', N'Tolerance returned: {{displayId}}', N'Tolerated risk {{displayId}} returned for re-assessment.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'EVIDENCE_EXPIRING')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'EVIDENCE_EXPIRING', N'Evidence expiring: {{title}}', N'Evidence "{{title}}" expires on {{expiryDate}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'AUDIT_PLANNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'AUDIT_PLANNED', N'Audit planned: {{name}}', N'Audit "{{name}}" ({{frameworkCode}}) is planned. Lead auditor notified.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'AUDIT_SUBMITTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'AUDIT_SUBMITTED', N'Audit submitted for Admin sign-off: {{name}}', N'Auditor signed audit "{{name}}" — awaiting Admin second sign.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'AUDIT_COMPLETED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'AUDIT_COMPLETED', N'Audit locked: {{name}}', N'Audit "{{name}}" is locked after Admin sign-off.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'AUDIT_RETURNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'AUDIT_RETURNED', N'Audit returned: {{name}}', N'Audit "{{name}}" was returned to the auditor: {{reason}}', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'AUDIT_EXTERNAL_INVITE_LINK')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'AUDIT_EXTERNAL_INVITE_LINK', N'External audit access link', N'You were invited to audit "{{auditName}}". Open: {{link}}', 1, 0, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'AUDIT_EXTERNAL_INVITE_OTP')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'AUDIT_EXTERNAL_INVITE_OTP', N'External audit one-time code', N'Your OTP for audit "{{auditName}}" is {{otp}}. Expires {{expiry}}.', 1, 0, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'CAPA_CREATED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'CAPA_CREATED', N'CAPA opened: {{title}}', N'A CAPA was opened ({{registerCode}}): {{title}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'CAPA_ASSIGNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'CAPA_ASSIGNED', N'CAPA assigned: {{title}}', N'CAPA "{{title}}" was assigned to you. Target: {{targetDate}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'CAPA_SUBMITTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'CAPA_SUBMITTED', N'CAPA submitted: {{title}}', N'Owner submitted CAPA "{{title}}" for closure review.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'CAPA_CLOSED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'CAPA_CLOSED', N'CAPA closed: {{title}}', N'CAPA "{{title}}" was closed.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'CAPA_REJECTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'CAPA_REJECTED', N'CAPA returned: {{title}}', N'CAPA "{{title}}" was returned: {{reason}}', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'CAPA_DUE_SOON')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'CAPA_DUE_SOON', N'CAPA due soon: {{title}}', N'CAPA "{{title}}" is due on {{targetDate}} (within 3 days).', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'CAPA_OVERDUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'CAPA_OVERDUE', N'CAPA overdue: {{title}}', N'CAPA "{{title}}" is overdue (target {{targetDate}}).', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'CAPA_OVERDUE_ESCALATION')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'CAPA_OVERDUE_ESCALATION', N'CAPA overdue escalation: {{title}}', N'CAPA "{{title}}" remains overdue 7+ days past target {{targetDate}}.', 1, 1, 1, 1);
END;
