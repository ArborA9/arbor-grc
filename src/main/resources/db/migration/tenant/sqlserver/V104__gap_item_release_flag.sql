-- Hold a Gap decision until Send, and mark a control for later work.

IF COL_LENGTH('gap_item', 'released_to_contributor') IS NULL
BEGIN
    ALTER TABLE gap_item ADD released_to_contributor BIT NOT NULL
        CONSTRAINT df_gap_item_released DEFAULT 0;
END
GO

IF COL_LENGTH('gap_item', 'flagged_for_later') IS NULL
BEGIN
    ALTER TABLE gap_item ADD flagged_for_later BIT NOT NULL
        CONSTRAINT df_gap_item_flagged DEFAULT 0;
END
GO

IF COL_LENGTH('gap_item', 'pending_due_date') IS NULL
BEGIN
    ALTER TABLE gap_item ADD pending_due_date DATE NULL;
END
GO

UPDATE gap_item SET released_to_contributor = 1 WHERE work_task_id IS NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'GAP_CONTROLS_RELEASED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'GAP_CONTROLS_RELEASED',
            N'Gap Controls Assigned: {{assessmentName}}',
            N'These controls on {{assessmentName}} are ready for your response:' + CHAR(10) + N'{{controlList}}',
            1, 0, 1, 1);
END
GO

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'GAP_CONTROLS_SENT_BACK')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'GAP_CONTROLS_SENT_BACK',
            N'Gap Controls Sent Back: {{assessmentName}}',
            N'These controls on {{assessmentName}} were sent back.' + CHAR(10) + N'{{controlList}}' + CHAR(10) + CHAR(10) + N'Reason: {{reason}}',
            1, 0, 1, 1);
END
GO
