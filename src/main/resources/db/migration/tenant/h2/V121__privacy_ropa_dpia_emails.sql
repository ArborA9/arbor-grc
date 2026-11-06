-- Privacy journey email + in-app templates (ROPA / DPIA). channel_email TRUE for branded HTML.

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'ROPA_ASSIGNED',
       'Action Required | ROPA | Assigned',
       'A Record of Processing Activities (ROPA) for {{department}} has been assigned to you as Privacy Champion. Open Arbor GRC to add processing activities and submit for Data Privacy Officer review.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'ROPA_ASSIGNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'ROPA_SUBMITTED',
       'Action Required | ROPA | Submitted for Review',
       'The Privacy Champion has submitted the ROPA for {{department}}. Please review each processing activity in Arbor GRC.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'ROPA_SUBMITTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'ROPA_RETURNED',
       'Action Required | ROPA | Returned',
       'The ROPA for {{department}} has been returned for updates. Open Arbor GRC to revise the returned processing activities and resubmit.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'ROPA_RETURNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'ROPA_SEND_TO_HEAD',
       'Action Required | ROPA | Send to Department Head',
       'The Data Privacy Officer has completed review of the ROPA for {{department}}. Open Arbor GRC and send it to the Department Head for sign-off.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'ROPA_SEND_TO_HEAD');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'ROPA_HEAD_SIGN_OFF',
       'Action Required | ROPA | Department Head Sign-off',
       'The ROPA for {{department}} is ready for your Department Head review. Open Arbor GRC to approve or return each processing activity.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'ROPA_HEAD_SIGN_OFF');

UPDATE notification_template
SET subject_template = 'Confirmation | ROPA | Ready to Close',
    body_template = 'The Department Head has signed off the ROPA for {{department}}. Open Arbor GRC to close the department ROPA.',
    channel_email = TRUE,
    channel_in_app = TRUE,
    is_active = TRUE
WHERE trigger_type = 'ROPA_READY_TO_CLOSE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'ROPA_READY_TO_CLOSE') v);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'DPIA_ASSIGNED',
       'Action Required | DPIA | Assigned',
       'A Data Protection Impact Assessment (DPIA) for {{department}} has been assigned to you as Privacy Champion. Open Arbor GRC to complete the questionnaire and submit for Data Privacy Officer review.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'DPIA_ASSIGNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'DPIA_SUBMITTED',
       'Action Required | DPIA | Submitted for Review',
       'The Privacy Champion has submitted the DPIA questionnaire for {{department}}. Please review it in Arbor GRC.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'DPIA_SUBMITTED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'DPIA_RETURNED',
       'Action Required | DPIA | Returned',
       'The DPIA for {{department}} has been returned for updates. Open Arbor GRC to revise your answers and resubmit.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'DPIA_RETURNED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'DPIA_HEAD_SIGN_OFF',
       'Action Required | DPIA | Department Head Sign-off',
       'The DPIA assessment for {{department}} is ready for your Department Head review. Open Arbor GRC to approve or return it.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'DPIA_HEAD_SIGN_OFF');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'DPIA_READY_TO_CLOSE',
       'Confirmation | DPIA | Ready to Close',
       'The Department Head has approved the DPIA for {{department}}. Open Arbor GRC to close the department DPIA.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'DPIA_READY_TO_CLOSE');
