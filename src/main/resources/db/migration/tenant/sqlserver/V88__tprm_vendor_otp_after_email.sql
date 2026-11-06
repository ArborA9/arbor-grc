-- Vendor portal: invite email has the link only. OTP is sent after the vendor
-- confirms the invitee email on the portal (no pre-OTP on assign / return).

UPDATE notification_template SET
    subject_template = N'[Action Required] - {{assessmentTitle}} has been assigned',
    body_template = N'The questionnaire "{{assessmentTitle}}" has been assigned to {{vendorName}}. Open the secure link: {{accessLink}} (expires {{expiresAt}}). Enter the email this invite was sent to. We will then send a one-time code.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_SENT' ORDER BY version DESC
    ) x
);
GO

UPDATE notification_template SET
    subject_template = N'[Action Required] - Questionnaire returned: {{displayId}}',
    body_template = N'Some controls on "{{assessmentTitle}}" were returned for {{vendorName}}. Open this secure link to update the returned answers: {{accessLink}} (expires {{expiresAt}}). Enter the email this invite was sent to. We will then send a one-time code.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_RETURNED' ORDER BY version DESC
    ) x
);
GO
