-- Risk Wave 2: workflow fidelity — SQL Server twin.

ALTER TABLE risk_record ADD infosec_status_initial NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD infosec_status_contributor NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD infosec_status_final NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD assignment_chase_sent_at DATETIME2 NULL;
ALTER TABLE risk_record ADD owner_chase_sent_at DATETIME2 NULL;
ALTER TABLE risk_record ADD overdue_escalation_sent_at DATETIME2 NULL;

ALTER TABLE treatment_milestone ADD progress_notes NVARCHAR(MAX) NULL;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_NOT_RESPONDED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_NOT_RESPONDED', N'[Action Required] - Risk not responded - Risk',
            N'Risk {{displayId}} ({{title}}) has had no Contributor action for 3+ days since assignment.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_RETURNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_RETURNED', N'[Action Required] - Risk returned - Risk',
            N'Risk {{displayId}} ({{title}}) was returned for revision. Reason: {{reason}}', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_OWNER_NOT_RESPONDED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_OWNER_NOT_RESPONDED', N'[Action Required] - Risk Owner not responded - Risk',
            N'Risk {{displayId}} ({{title}}) awaits Risk Owner acceptance — no action for 5+ days.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_OWNER_REJECTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_OWNER_REJECTED', N'[Action Required] - Risk Owner rejected - Risk',
            N'Risk {{displayId}} ({{title}}) acceptance was rejected by the Risk Owner. Reason: {{reason}}', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'ADMIN_FINAL_DECISION')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'ADMIN_FINAL_DECISION', N'[FYI] - Admin final decision - Risk',
            N'Risk {{displayId}} ({{title}}) — Admin decision: {{decision}}. {{detail}}', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_RE_OPENED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_RE_OPENED', N'[Action Required] - Risk re-opened - Risk',
            N'Risk {{displayId}} ({{title}}) was re-opened. Reason: {{reason}}', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'RISK_OVERDUE_ESCALATION')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'RISK_OVERDUE_ESCALATION', N'[Escalation] - Risk overdue 7 days - Risk',
            N'Risk {{displayId}} ({{title}}) has been overdue for 7+ days and is escalated to the Owner.', 1, 0, 1, 1);
END;
