-- ROPA assigned back to the DPO after Department Head sign-off, to close.

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'ROPA_READY_TO_CLOSE',
       'ROPA ready to close: {{department}}',
       'The {{department}} department ROPA has been signed off by the Department Head. It is assigned back to the Data Privacy Officer to close the department ROPA.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'ROPA_READY_TO_CLOSE');
