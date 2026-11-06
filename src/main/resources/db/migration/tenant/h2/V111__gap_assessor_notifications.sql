INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_ASSESSMENT_CREATED',
       'Gap assessment created: {{assessmentName}}',
       'You have been named as the Assessor for {{assessmentName}} ({{frameworkName}}). Your access is unchanged. Target date: {{targetDate}}.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_ASSESSMENT_CREATED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_ASSESSMENT_COMPLETED_ASSESSOR',
       'Gap assessment completed: {{assessmentName}}',
       'Gap assessment {{assessmentName}} ({{frameworkName}}) has been completed. Overall score: {{overallScore}}.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_ASSESSMENT_COMPLETED_ASSESSOR');
