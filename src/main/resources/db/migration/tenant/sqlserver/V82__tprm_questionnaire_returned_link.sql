-- Return-to-vendor mail must include the portal URL. Generic ACS footer is skipped
-- for QUESTIONNAIRE_RETURNED, and the prior body had no {{accessLink}}.

UPDATE notification_template SET
    subject_template = N'[Action Required] - Questionnaire returned: {{displayId}}',
    body_template = N'Some controls on "{{assessmentTitle}}" were returned for {{vendorName}}. Open this secure link to update the returned answers: {{accessLink}} (expires {{expiresAt}}). A new one-time code follows in a separate email.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_RETURNED' ORDER BY version DESC
    ) x
);
