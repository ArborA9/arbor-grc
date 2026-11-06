-- Return-to-vendor mail must include the portal URL. Generic ACS footer is skipped
-- for QUESTIONNAIRE_RETURNED, and the prior body had no {{accessLink}}.

UPDATE notification_template SET
    subject_template = '[Action Required] - Questionnaire returned: {{displayId}}',
    body_template = 'Some controls on "{{assessmentTitle}}" were returned for {{vendorName}}. Open this secure link to update the returned answers: {{accessLink}} (expires {{expiresAt}}). A new one-time code follows in a separate email.'
WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_RETURNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_RETURNED') v);
