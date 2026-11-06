-- ROPA assigned back to the DPO after Department Head sign-off, to close.

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ROPA_READY_TO_CLOSE')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'ROPA_READY_TO_CLOSE', N'ROPA ready to close: {{department}}',
            N'The {{department}} department ROPA has been signed off by the Department Head. It is assigned back to the Data Privacy Officer to close the department ROPA.', 1, 1, 1, 1);
GO
