-- Privacy journey email + in-app templates (ROPA / DPIA).

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ROPA_ASSIGNED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'ROPA_ASSIGNED', N'Action Required | ROPA | Assigned',
            N'A Record of Processing Activities (ROPA) for {{department}} has been assigned to you as Privacy Champion. Open Arbor GRC to add processing activities and submit for Data Privacy Officer review.',
            1, 1, 1, 1);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ROPA_SUBMITTED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'ROPA_SUBMITTED', N'Action Required | ROPA | Submitted for Review',
            N'The Privacy Champion has submitted the ROPA for {{department}}. Please review each processing activity in Arbor GRC.',
            1, 1, 1, 1);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ROPA_RETURNED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'ROPA_RETURNED', N'Action Required | ROPA | Returned',
            N'The ROPA for {{department}} has been returned for updates. Open Arbor GRC to revise the returned processing activities and resubmit.',
            1, 1, 1, 1);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ROPA_SEND_TO_HEAD')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'ROPA_SEND_TO_HEAD', N'Action Required | ROPA | Send to Department Head',
            N'The Data Privacy Officer has completed review of the ROPA for {{department}}. Open Arbor GRC and send it to the Department Head for sign-off.',
            1, 1, 1, 1);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ROPA_HEAD_SIGN_OFF')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'ROPA_HEAD_SIGN_OFF', N'Action Required | ROPA | Department Head Sign-off',
            N'The ROPA for {{department}} is ready for your Department Head review. Open Arbor GRC to approve or return each processing activity.',
            1, 1, 1, 1);
GO

UPDATE notification_template
SET subject_template = N'Confirmation | ROPA | Ready to Close',
    body_template = N'The Department Head has signed off the ROPA for {{department}}. Open Arbor GRC to close the department ROPA.',
    channel_email = 1,
    channel_in_app = 1,
    is_active = 1
WHERE trigger_type = N'ROPA_READY_TO_CLOSE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = N'ROPA_READY_TO_CLOSE') v);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DPIA_ASSIGNED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DPIA_ASSIGNED', N'Action Required | DPIA | Assigned',
            N'A Data Protection Impact Assessment (DPIA) for {{department}} has been assigned to you as Privacy Champion. Open Arbor GRC to complete the questionnaire and submit for Data Privacy Officer review.',
            1, 1, 1, 1);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DPIA_SUBMITTED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DPIA_SUBMITTED', N'Action Required | DPIA | Submitted for Review',
            N'The Privacy Champion has submitted the DPIA questionnaire for {{department}}. Please review it in Arbor GRC.',
            1, 1, 1, 1);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DPIA_RETURNED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DPIA_RETURNED', N'Action Required | DPIA | Returned',
            N'The DPIA for {{department}} has been returned for updates. Open Arbor GRC to revise your answers and resubmit.',
            1, 1, 1, 1);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DPIA_HEAD_SIGN_OFF')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DPIA_HEAD_SIGN_OFF', N'Action Required | DPIA | Department Head Sign-off',
            N'The DPIA assessment for {{department}} is ready for your Department Head review. Open Arbor GRC to approve or return it.',
            1, 1, 1, 1);
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DPIA_READY_TO_CLOSE')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DPIA_READY_TO_CLOSE', N'Confirmation | DPIA | Ready to Close',
            N'The Department Head has approved the DPIA for {{department}}. Open Arbor GRC to close the department DPIA.',
            1, 1, 1, 1);
GO
