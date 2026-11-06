-- Path B2: Gap submit notify Admin; ensure all currently fired triggers have templates.

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('GAP_CONTROL_SUBMITTED',
 'Gap control submitted: {{nodeCode}}',
 'Contributor submitted control {{nodeCode}} ({{title}}) in assessment "{{assessmentName}}" with status {{status}}. Review and finalize or send back.',
 TRUE, TRUE, 1, TRUE);
