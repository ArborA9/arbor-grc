-- Path C B5: ensure every trigger currently fired by Gap/Risk/Audit/CAPA/SSO/Evidence has a template.
-- Idempotent: insert only when no row exists for that trigger_type (any version).

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'USER_INVITED', 'You are invited to Arbor GRC', 'Hello {{email}}, you have been invited. Sign in to activate your account.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'USER_INVITED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'SSO_NO_ROLE', 'SSO login blocked: {{email}}', 'An SSO user ({{email}}) could not sign in — no Arbor account / role. Invite them or enable JIT.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'SSO_NO_ROLE');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'SSO_JIT_PROVISIONED', 'SSO user auto-provisioned: {{email}}', 'A new {{role}} account was created via JIT SSO for {{email}}. Assign organisation units if needed.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'SSO_JIT_PROVISIONED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'LOCAL_AUTH_USED', 'Local login while SSO is enabled', '{{email}} signed in with a local password while SSO is enabled for this workspace.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'LOCAL_AUTH_USED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_CONTROL_ASSIGNED', 'Gap control assigned: {{nodeCode}}', 'Control {{nodeCode}} on assessment {{assessmentName}} was assigned to you.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_CONTROL_ASSIGNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_CONTROL_SUBMITTED', 'Gap control submitted: {{nodeCode}}', 'Contributor submitted {{nodeCode}} on {{assessmentName}} for review.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_CONTROL_SUBMITTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_CONTROL_SENT_BACK', 'Gap control sent back: {{nodeCode}}', 'Admin sent back {{nodeCode}}: {{reason}}', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_CONTROL_SENT_BACK');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_CONTROL_FINALIZED', 'Gap control finalized: {{nodeCode}}', 'Admin finalized {{nodeCode}} on {{assessmentName}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_CONTROL_FINALIZED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_ASSIGNED', 'Risk assigned: {{displayId}}', 'Risk {{displayId}} ({{title}}) was assigned to you.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_ASSIGNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_SUBMITTED', 'Risk submitted: {{displayId}}', 'Contributor submitted risk {{displayId}} for validation.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_SUBMITTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_VALIDATION_REJECTED', 'Risk validation rejected: {{displayId}}', 'Risk {{displayId}} was sent back for revision.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_VALIDATION_REJECTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_PENDING_ADMIN', 'Risk pending Admin: {{displayId}}', 'Risk {{displayId}} awaits Admin decision.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_PENDING_ADMIN');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_PENDING_OWNER', 'Risk pending Owner: {{displayId}}', 'Risk {{displayId}} awaits Risk Owner acceptance.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_PENDING_OWNER');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_TOLERATED', 'Risk tolerated: {{displayId}}', 'Risk {{displayId}} was accepted / tolerated.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_TOLERATED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_MITIGATING', 'Risk mitigating: {{displayId}}', 'Risk {{displayId}} treatment is in progress.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_MITIGATING');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'TREATMENT_OVERDUE', 'Treatment overdue: {{displayId}}', 'Treatment for {{displayId}} is past target date {{targetDate}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'TREATMENT_OVERDUE');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_TOLERANCE_REVIEW_DUE', 'Tolerance review due: {{displayId}}', 'Tolerated risk {{displayId}} is approaching its review date.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_TOLERANCE_REVIEW_DUE');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_TOLERANCE_RETURNED', 'Tolerance returned: {{displayId}}', 'Tolerated risk {{displayId}} returned for re-assessment.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_TOLERANCE_RETURNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'EVIDENCE_EXPIRING', 'Evidence expiring: {{title}}', 'Evidence "{{title}}" expires on {{expiryDate}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'EVIDENCE_EXPIRING');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_PLANNED', 'Audit planned: {{name}}', 'Audit "{{name}}" ({{frameworkCode}}) is planned. Lead auditor notified.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_PLANNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_SUBMITTED', 'Audit submitted for Admin sign-off: {{name}}', 'Auditor signed audit "{{name}}" — awaiting Admin second sign.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_SUBMITTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_COMPLETED', 'Audit locked: {{name}}', 'Audit "{{name}}" is locked after Admin sign-off.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_COMPLETED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_RETURNED', 'Audit returned: {{name}}', 'Audit "{{name}}" was returned to the auditor: {{reason}}', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_RETURNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_EXTERNAL_INVITE_LINK', 'External audit access link', 'You were invited to audit "{{auditName}}". Open: {{link}}', TRUE, FALSE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_EXTERNAL_INVITE_LINK');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'AUDIT_EXTERNAL_INVITE_OTP', 'External audit one-time code', 'Your OTP for audit "{{auditName}}" is {{otp}}. Expires {{expiry}}.', TRUE, FALSE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'AUDIT_EXTERNAL_INVITE_OTP');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'CAPA_CREATED', 'CAPA opened: {{title}}', 'A CAPA was opened ({{registerCode}}): {{title}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'CAPA_CREATED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'CAPA_ASSIGNED', 'CAPA assigned: {{title}}', 'CAPA "{{title}}" was assigned to you. Target: {{targetDate}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'CAPA_ASSIGNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'CAPA_SUBMITTED', 'CAPA submitted: {{title}}', 'Owner submitted CAPA "{{title}}" for closure review.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'CAPA_SUBMITTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'CAPA_CLOSED', 'CAPA closed: {{title}}', 'CAPA "{{title}}" was closed.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'CAPA_CLOSED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'CAPA_REJECTED', 'CAPA returned: {{title}}', 'CAPA "{{title}}" was returned: {{reason}}', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'CAPA_REJECTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'CAPA_DUE_SOON', 'CAPA due soon: {{title}}', 'CAPA "{{title}}" is due on {{targetDate}} (within 3 days).', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'CAPA_DUE_SOON');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'CAPA_OVERDUE', 'CAPA overdue: {{title}}', 'CAPA "{{title}}" is overdue (target {{targetDate}}).', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'CAPA_OVERDUE');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'CAPA_OVERDUE_ESCALATION', 'CAPA overdue escalation: {{title}}', 'CAPA "{{title}}" remains overdue 7+ days past target {{targetDate}}.', TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'CAPA_OVERDUE_ESCALATION');
