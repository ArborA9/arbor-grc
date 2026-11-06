IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'GAP_ASSESSMENT_CREATED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (
        N'GAP_ASSESSMENT_CREATED',
        N'Gap assessment created: {{assessmentName}}',
        N'You have been named as the Assessor for {{assessmentName}} ({{frameworkName}}). Your access is unchanged. Target date: {{targetDate}}.',
        1, 1, 1, 1
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'GAP_ASSESSMENT_COMPLETED_ASSESSOR')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (
        N'GAP_ASSESSMENT_COMPLETED_ASSESSOR',
        N'Gap assessment completed: {{assessmentName}}',
        N'Gap assessment {{assessmentName}} ({{frameworkName}}) has been completed. Overall score: {{overallScore}}.',
        1, 1, 1, 1
    );
END;
GO
