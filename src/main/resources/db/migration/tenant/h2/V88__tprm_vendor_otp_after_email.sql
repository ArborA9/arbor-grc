-- Vendor portal: invite email has the link only. OTP is sent after the vendor
-- confirms the invitee email on the portal (no pre-OTP on assign / return).

UPDATE notification_template SET
    subject_template = '[Action Required] - {{assessmentTitle}} has been assigned',
    body_template = 'The questionnaire "{{assessmentTitle}}" has been assigned to {{vendorName}}. Open the secure link: {{accessLink}} (expires {{expiresAt}}). Enter the email this invite was sent to. We will then send a one-time code.'
WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_SENT'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_SENT') v);

UPDATE notification_template SET
    subject_template = '[Action Required] - Questionnaire returned: {{displayId}}',
    body_template = 'Some controls on "{{assessmentTitle}}" were returned for {{vendorName}}. Open this secure link to update the returned answers: {{accessLink}} (expires {{expiresAt}}). Enter the email this invite was sent to. We will then send a one-time code.'
WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_RETURNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_RETURNED') v);
