-- V79: clearer email/in-app copy (no enums, no em dashes, Action Required vs FYI).
-- Updates the latest version per trigger; does not re-enable inactive templates.

UPDATE notification_template SET subject_template = N'[FYI] - Admin final decision',
    body_template = N'Risk {{displayId}} ({{title}}) - Admin decision: {{decision}}. {{detail}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'ADMIN_FINAL_DECISION' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Audit locked: {{name}}',
    body_template = N'Audit "{{name}}" is locked after Admin sign-off.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_COMPLETED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - External audit access',
    body_template = N'You were invited to review audit "{{auditName}}". Open the secure link: {{link}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_EXTERNAL_INVITE_LINK' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - External audit one-time code',
    body_template = N'Your one-time code for audit "{{auditName}}" is {{otp}}. It expires {{expiry}}. Enter it on the access page.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_EXTERNAL_INVITE_OTP' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Audit finding: {{displayCode}}',
    body_template = N'Finding {{displayCode}} ({{severity}}) logged on audit "{{auditName}}": {{title}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_FINDING_LOGGED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Audit overdue: {{name}}',
    body_template = N'Audit "{{name}}" is past end date {{endDate}} (status {{status}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Audit planned: {{name}}',
    body_template = N'Audit "{{name}}" ({{frameworkCode}}) is planned. Lead auditor notified.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_PLANNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Audit awaiting Admin sign-off: {{name}}',
    body_template = N'Audit "{{name}}" is submitted and awaiting Admin approval.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_REPORT_PENDING' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Audit returned: {{name}}',
    body_template = N'Audit "{{name}}" was returned to the auditor: {{reason}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_RETURNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Audit starts today: {{name}}',
    body_template = N'Audit "{{name}}" ({{frameworkCode}}) start date is {{startDate}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_START_DATE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Audit submitted for Admin sign-off: {{name}}',
    body_template = N'Auditor signed audit "{{name}}" - awaiting Admin second sign.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'AUDIT_SUBMITTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Recovery plan needs approval: {{planName}}',
    body_template = N'Recovery plan "{{planName}}" awaits Owner approval.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BCP_APPROVAL_NEEDED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Recovery plan approved: {{planName}}',
    body_template = N'Recovery plan "{{planName}}" (v{{version}}) was approved.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BCP_APPROVED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Recovery plan expired: {{planName}}',
    body_template = N'Recovery plan "{{planName}}" is past its review date.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BCP_EXPIRED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Recovery plan review due: {{planName}}',
    body_template = N'Recovery plan "{{planName}}" review date {{reviewDate}} is approaching.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BCP_REVIEW_DUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - BIA approved: {{processName}}',
    body_template = N'BIA "{{processName}}" was approved.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_APPROVED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - BIA assigned: {{processName}}',
    body_template = N'BIA "{{processName}}" was assigned to you. Due {{dueDate}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_ASSIGNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - BIA closed: {{processName}}',
    body_template = N'Department BIA "{{processName}}" was closed.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_CLOSED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - BIA overdue: {{processName}}',
    body_template = N'BIA "{{processName}}" is past due ({{dueDate}}) and not submitted.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_NOT_SUBMITTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - BIA ready for BC Head: {{processName}}',
    body_template = N'Department BIA "{{processName}}" is waiting for Business Continuity Head review.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_PENDING_BC_HEAD' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - BIA approved by BC Head: {{processName}}',
    body_template = N'BC Head approved BIA "{{processName}}". Close the department BIA when ready.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_READY_TO_CLOSE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - BIA returned: {{processName}}',
    body_template = N'BIA "{{processName}}" was returned: {{reason}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_RETURNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - BIA returned by BC Head: {{processName}}',
    body_template = N'BC Head returned BIA "{{processName}}": {{reason}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_RETURNED_TO_ADMIN' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - BIA review due: {{processName}}',
    body_template = N'BIA "{{processName}}" review date {{reviewDate}} is approaching.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_REVIEW_DUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - BIA submitted: {{processName}}',
    body_template = N'Contributor submitted BIA "{{processName}}" for review.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BIA_SUBMITTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Breach 48h warning: {{displayId}}',
    body_template = N'Breach {{displayId}} ({{title}}) - 24 hours remain before the 72-hour PDPL notification deadline ({{reportDeadlineAt}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BREACH_48H_WARNING' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Breach OVERDUE - 72h clock: {{displayId}}',
    body_template = N'CRITICAL: Breach {{displayId}} ({{title}}) has passed the 72-hour notification deadline ({{reportDeadlineAt}}) without authority reporting.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BREACH_72H_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Personal data breach logged: {{displayId}}',
    body_template = N'Breach {{displayId}} - {{title}} was logged. 72-hour authority notification deadline: {{reportDeadlineAt}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'BREACH_RECORDED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - CAPA assigned: {{title}}',
    body_template = N'CAPA "{{title}}" was assigned to you. Target: {{targetDate}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'CAPA_ASSIGNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - CAPA closed: {{title}}',
    body_template = N'CAPA "{{title}}" was closed.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'CAPA_CLOSED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - CAPA opened: {{title}}',
    body_template = N'A CAPA was opened ({{registerCode}}): {{title}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'CAPA_CREATED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - CAPA due soon: {{title}}',
    body_template = N'CAPA "{{title}}" is due on {{targetDate}} (within 3 days).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'CAPA_DUE_SOON' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - CAPA overdue: {{title}}',
    body_template = N'CAPA "{{title}}" is overdue (target {{targetDate}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'CAPA_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - CAPA overdue escalation: {{title}}',
    body_template = N'CAPA "{{title}}" remains overdue 7+ days past target {{targetDate}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'CAPA_OVERDUE_ESCALATION' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - CAPA returned: {{title}}',
    body_template = N'CAPA "{{title}}" was returned: {{reason}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'CAPA_REJECTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - CAPA submitted: {{title}}',
    body_template = N'Owner submitted CAPA "{{title}}" for closure review.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'CAPA_SUBMITTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - DPIA required: {{activityName}}',
    body_template = N'ROPA activity "{{activityName}}" ({{displayId}}) requires a Data Protection Impact Assessment before approval.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'DPIA_REQUIRED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - DSR day 20 warning: {{displayId}}',
    body_template = N'DSR {{displayId}} ({{requestType}} for {{subjectName}}) has 10 days remaining. Due date: {{dueAt}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'DSR_DAY_20' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - DSR day 28 urgent: {{displayId}}',
    body_template = N'URGENT: DSR {{displayId}} ({{requestType}} for {{subjectName}}) has 2 days remaining. Due date: {{dueAt}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'DSR_DAY_28' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - DSR OVERDUE - PDPL breach risk: {{displayId}}',
    body_template = N'CRITICAL: DSR {{displayId}} ({{requestType}} for {{subjectName}}) is past the 30-day UAE PDPL deadline (due {{dueAt}}). Immediate action required.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'DSR_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - New DSR received: {{displayId}}',
    body_template = N'A data subject request ({{requestType}}) for {{subjectName}} has been logged as {{displayId}}. Response is due by {{dueAt}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'DSR_RECEIVED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Evidence expired: {{title}}',
    body_template = N'Evidence "{{title}}" expired on {{expiresOn}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'EVIDENCE_EXPIRED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Evidence expiring: {{title}}',
    body_template = N'Evidence "{{title}}" expires on {{expiryDate}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'EVIDENCE_EXPIRING' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Exercise due: {{exerciseName}}',
    body_template = N'BCMS exercise "{{exerciseName}}" is scheduled for {{scheduledDate}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'EXERCISE_DUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Exercise overdue: {{exerciseName}}',
    body_template = N'BCMS exercise "{{exerciseName}}" is overdue ({{scheduledDate}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'EXERCISE_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Gap assessment completed - {{assessmentName}}',
    body_template = N'Gap assessment {{assessmentName}} ({{frameworkCode}}) has been marked complete. Overall score: {{overallScore}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'GAP_ASSESSMENT_COMPLETED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Gap control assigned: {{nodeCode}}',
    body_template = N'Control {{nodeCode}} on assessment {{assessmentName}} was assigned to you.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'GAP_CONTROL_ASSIGNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Gap control finalized: {{nodeCode}}',
    body_template = N'Admin finalized {{nodeCode}} on {{assessmentName}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'GAP_CONTROL_FINALIZED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Gap control sent back: {{nodeCode}}',
    body_template = N'Admin sent back {{nodeCode}}: {{reason}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'GAP_CONTROL_SENT_BACK' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Gap control submitted: {{nodeCode}}',
    body_template = N'Contributor submitted {{nodeCode}} on {{assessmentName}} for review.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'GAP_CONTROL_SUBMITTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Gap control overdue: {{nodeCode}}',
    body_template = N'Control {{nodeCode}} ({{title}}) is past due ({{dueDate}}) and not submitted.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'GAP_NOT_RESPONDED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Incident assigned',
    body_template = N'Incident {{displayId}} ({{title}}) was assigned to you. Priority {{priority}}, response due {{responseDue}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'INCIDENT_ASSIGNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Incident closed',
    body_template = N'Incident {{displayId}} ({{title}}) was closed by Admin.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'INCIDENT_CLOSED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Incident response overdue',
    body_template = N'Incident {{displayId}} ({{title}}) is past its response target ({{responseDue}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'INCIDENT_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Critical] - P1 incident reported',
    body_template = N'P1 Critical incident {{displayId}} ({{title}}) was reported. Immediate response required (target {{responseDue}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'INCIDENT_P1_REPORTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - P2 incident reported',
    body_template = N'P2 High incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'INCIDENT_P2_REPORTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - P3 incident reported',
    body_template = N'P3 Medium incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'INCIDENT_P3_REPORTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - P4 incident reported',
    body_template = N'P4 Low incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'INCIDENT_P4_REPORTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Incident resolved',
    body_template = N'Incident {{displayId}} ({{title}}) was marked Resolved.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'INCIDENT_RESOLVED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Warning] - KRI Amber breach',
    body_template = N'KRI {{displayId}} ({{name}}) entered Amber (value {{value}}, threshold {{threshold}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'KRI_AMBER_BREACH' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - KRI Red acknowledged',
    body_template = N'KRI {{displayId}} ({{name}}) Red breach was acknowledged by Admin.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'KRI_RED_ACKNOWLEDGED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Critical] - KRI Red breach',
    body_template = N'KRI {{displayId}} ({{name}}) entered Red (value {{value}}, threshold {{threshold}}). Acknowledge required.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'KRI_RED_BREACH' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - KRI value due',
    body_template = N'KRI {{displayId}} ({{name}}) is due for measurement on {{dueDate}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'KRI_VALUE_DUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - KRI value overdue',
    body_template = N'KRI {{displayId}} ({{name}}) is overdue for measurement (due {{dueDate}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'KRI_VALUE_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Emergency password login used',
    body_template = N'{{email}} signed in with email and password while Microsoft sign-in is enabled for this workspace.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'LOCAL_AUTH_USED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Policy acknowledgement due soon: {{title}}',
    body_template = N'Acknowledgement for "{{title}}" is due by {{ackDeadline}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'POLICY_ACK_DEADLINE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Policy acknowledgements overdue: {{title}}',
    body_template = N'Some users have not acknowledged "{{title}}" (deadline {{ackDeadline}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'POLICY_ACK_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Acknowledge policy: {{title}}',
    body_template = N'Please read and acknowledge "{{title}}" (v{{version}}).'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'POLICY_ACK_REQUIRED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Policy approved: {{title}}',
    body_template = N'Policy "{{title}}" (v{{version}}) was approved. Acknowledgement may be required.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'POLICY_APPROVED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Policy due for review: {{title}}',
    body_template = N'Policy "{{title}}" is past its review date and marked Due for Review.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'POLICY_EXPIRED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Policy review due soon: {{title}}',
    body_template = N'Policy "{{title}}" review date {{reviewDate}} is approaching.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'POLICY_REVIEW_DUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Policy submitted for review: {{title}}',
    body_template = N'Policy "{{title}}" was submitted for review.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'POLICY_SUBMITTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Scheduled report ready - {{reportTitle}}',
    body_template = N'Your scheduled {{reportTitle}} ({{format}}) is ready. Open Reports to download from delivery history.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'REPORT_SCHEDULED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk assigned: {{displayId}}',
    body_template = N'Risk {{displayId}} ({{title}}) was assigned to you.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_ASSIGNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Risk auto-closed',
    body_template = N'Risk {{displayId}} ({{title}}) was auto-closed because residual risk is {{residualRisk}}. No treatment workflow is required.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_AUTO_CLOSED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Risk matrix updated',
    body_template = N'The tenant risk matrices were updated (version {{version}}). Reason: {{reason}}. Existing risk scores are not recalculated.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_MATRIX_CHANGED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk mitigating: {{displayId}}',
    body_template = N'Risk {{displayId}} treatment is in progress.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_MITIGATING' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk not responded',
    body_template = N'Risk {{displayId}} ({{title}}) has had no Contributor action for 3+ days since assignment.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_NOT_RESPONDED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Escalation] - Risk overdue 7 days',
    body_template = N'Risk {{displayId}} ({{title}}) has been overdue for 7+ days and is escalated to the Owner.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_OVERDUE_ESCALATION' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk Owner not responded',
    body_template = N'Risk {{displayId}} ({{title}}) awaits Risk Owner acceptance - no action for 5+ days.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_OWNER_NOT_RESPONDED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk Owner rejected',
    body_template = N'Risk {{displayId}} ({{title}}) acceptance was rejected by the Risk Owner. Reason: {{reason}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_OWNER_REJECTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk pending Admin: {{displayId}}',
    body_template = N'Risk {{displayId}} awaits Admin decision.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_PENDING_ADMIN' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk pending Owner: {{displayId}}',
    body_template = N'Risk {{displayId}} awaits Risk Owner acceptance.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_PENDING_OWNER' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk returned',
    body_template = N'Risk {{displayId}} ({{title}}) was returned for revision. Reason: {{reason}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_RETURNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk review due',
    body_template = N'Risk {{displayId}} ({{title}}) Next Review Date {{reviewDate}} is within 30 days.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_REVIEW_DUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk re-opened',
    body_template = N'Risk {{displayId}} ({{title}}) was re-opened. Reason: {{reason}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_RE_OPENED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk submitted: {{displayId}}',
    body_template = N'Contributor submitted risk {{displayId}} for validation.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_SUBMITTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Tolerance returned: {{displayId}}',
    body_template = N'Tolerated risk {{displayId}} returned for re-assessment.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_TOLERANCE_RETURNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Tolerance review due: {{displayId}}',
    body_template = N'Tolerated risk {{displayId}} is approaching its review date.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_TOLERANCE_REVIEW_DUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Risk tolerated: {{displayId}}',
    body_template = N'Risk {{displayId}} was accepted / tolerated.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_TOLERATED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Risk validation rejected: {{displayId}}',
    body_template = N'Risk {{displayId}} was sent back for revision.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'RISK_VALIDATION_REJECTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Your Arbor GRC roles were updated',
    body_template = N'Your roles on workspace {{workspaceId}} are now: {{roles}}. Sign in again if menus look wrong.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'ROLE_CHANGED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - New user created from Microsoft sign-in',
    body_template = N'{{email}} signed in with Microsoft and was created as {{role}}. Assign organisation units if needed.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'SSO_JIT_PROVISIONED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Microsoft sign-in blocked',
    body_template = N'Someone tried to sign in with Microsoft as {{email}}, but they have no Arbor account yet. Invite them if they should have access.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'SSO_NO_ROLE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Treatment overdue: {{displayId}}',
    body_template = N'Treatment for {{displayId}} is past target date {{targetDate}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'TREATMENT_OVERDUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - Your Arbor GRC account was deactivated',
    body_template = N'Your account ({{email}}) on workspace {{workspaceId}} was deactivated. You can no longer sign in. Contact your administrator if this is unexpected.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'USER_DEACTIVATED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - You are invited to Arbor GRC',
    body_template = N'You have been invited to Arbor GRC as {{roles}}. {{signInHint}}'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'USER_INVITED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Vendor contract expiry: {{vendorName}}',
    body_template = N'Contract {{contractCode}} for {{vendorName}} ends on {{contractEnd}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_CONTRACT_EXPIRY' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - High-risk vendor detected: {{vendorName}}',
    body_template = N'Vendor "{{vendorName}}" scored a {{residualRisk}} residual risk on assessment "{{assessmentTitle}}". Consider raising a linked ERM risk.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_HIGH_RISK' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Vendor not compliant: {{vendorName}}',
    body_template = N'Vendor "{{vendorName}}" did not meet the tier posture target on {{displayId}}. Consider raising a linked ERM risk.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_NOT_COMPLIANT' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Your vendor questionnaire code',
    body_template = N'Your one-time code for the vendor security questionnaire is {{otpCode}}. It is valid until {{expiresAt}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_OTP' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Questionnaire returned: {{displayId}}',
    body_template = N'Some controls on "{{assessmentTitle}}" were returned for {{vendorName}}. Please reopen the secure link and update the returned answers.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_RETURNED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Security questionnaire for {{vendorName}}',
    body_template = N'Please complete the security questionnaire for {{vendorName}}. Open the secure link: {{accessLink}} (expires {{expiresAt}}). A one-time code follows in a separate email.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_SENT' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Action Required] - Vendor questionnaire submitted: {{displayId}}',
    body_template = N'The vendor has submitted their responses for "{{assessmentTitle}}" ({{displayId}}). Please review and score the assessment.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_SUBMITTED' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[FYI] - TPRM review complete: {{vendorName}}',
    body_template = N'Review of {{displayId}} for {{vendorName}} is complete. Posture {{posturePercent}}%. Compliance: {{complianceStatus}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_REVIEW_COMPLETE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Reminder] Vendor review due - {{vendorName}}',
    body_template = N'Vendor {{vendorName}} is due for review on {{reviewDate}}. Residual risk: {{residualRisk}}.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_REVIEW_DUE' ORDER BY version DESC
    ) x
);

UPDATE notification_template SET subject_template = N'[Overdue] Vendor review past due - {{vendorName}}',
    body_template = N'Vendor {{vendorName}} review was due on {{reviewDate}}. Please complete the review.'
WHERE id = (
    SELECT id FROM (
        SELECT TOP (1) id FROM notification_template WHERE trigger_type = N'VENDOR_REVIEW_OVERDUE' ORDER BY version DESC
    ) x
);
