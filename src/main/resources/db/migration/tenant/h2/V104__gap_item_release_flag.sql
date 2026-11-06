-- Hold a Gap decision until Send, and mark a control for later work.

ALTER TABLE gap_item ADD COLUMN IF NOT EXISTS released_to_contributor BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE gap_item ADD COLUMN IF NOT EXISTS flagged_for_later BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE gap_item ADD COLUMN IF NOT EXISTS pending_due_date DATE;

UPDATE gap_item SET released_to_contributor = TRUE WHERE work_task_id IS NOT NULL;

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_CONTROLS_RELEASED',
       'Gap Controls Assigned: {{assessmentName}}',
       'These controls on {{assessmentName}} are ready for your response:' || CHAR(10) || '{{controlList}}',
       TRUE, FALSE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_CONTROLS_RELEASED');

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'GAP_CONTROLS_SENT_BACK',
       'Gap Controls Sent Back: {{assessmentName}}',
       'These controls on {{assessmentName}} were sent back.' || CHAR(10) || '{{controlList}}' || CHAR(10) || CHAR(10) || 'Reason: {{reason}}',
       TRUE, FALSE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_CONTROLS_SENT_BACK');
