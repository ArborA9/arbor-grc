-- V79: clearer email/in-app copy (no enums, no em dashes, Action Required vs FYI).
-- Updates the latest version per trigger; does not re-enable inactive templates.

UPDATE notification_template SET subject_template = '[FYI] - Admin final decision',
    body_template = 'Risk {{displayId}} ({{title}}) - Admin decision: {{decision}}. {{detail}}'
WHERE trigger_type = 'ADMIN_FINAL_DECISION'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'ADMIN_FINAL_DECISION') v);

UPDATE notification_template SET subject_template = '[FYI] - Audit locked: {{name}}',
    body_template = 'Audit "{{name}}" is locked after Admin sign-off.'
WHERE trigger_type = 'AUDIT_COMPLETED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_COMPLETED') v);

UPDATE notification_template SET subject_template = '[Action Required] - External audit access',
    body_template = 'You were invited to review audit "{{auditName}}". Open the secure link: {{link}}'
WHERE trigger_type = 'AUDIT_EXTERNAL_INVITE_LINK'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_EXTERNAL_INVITE_LINK') v);

UPDATE notification_template SET subject_template = '[Action Required] - External audit one-time code',
    body_template = 'Your one-time code for audit "{{auditName}}" is {{otp}}. It expires {{expiry}}. Enter it on the access page.'
WHERE trigger_type = 'AUDIT_EXTERNAL_INVITE_OTP'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_EXTERNAL_INVITE_OTP') v);

UPDATE notification_template SET subject_template = '[Action Required] - Audit finding: {{displayCode}}',
    body_template = 'Finding {{displayCode}} ({{severity}}) logged on audit "{{auditName}}": {{title}}.'
WHERE trigger_type = 'AUDIT_FINDING_LOGGED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_FINDING_LOGGED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Audit overdue: {{name}}',
    body_template = 'Audit "{{name}}" is past end date {{endDate}} (status {{status}}).'
WHERE trigger_type = 'AUDIT_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_OVERDUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Audit planned: {{name}}',
    body_template = 'Audit "{{name}}" ({{frameworkCode}}) is planned. Lead auditor notified.'
WHERE trigger_type = 'AUDIT_PLANNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_PLANNED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Audit awaiting Admin sign-off: {{name}}',
    body_template = 'Audit "{{name}}" is submitted and awaiting Admin approval.'
WHERE trigger_type = 'AUDIT_REPORT_PENDING'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_REPORT_PENDING') v);

UPDATE notification_template SET subject_template = '[Action Required] - Audit returned: {{name}}',
    body_template = 'Audit "{{name}}" was returned to the auditor: {{reason}}'
WHERE trigger_type = 'AUDIT_RETURNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_RETURNED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Audit starts today: {{name}}',
    body_template = 'Audit "{{name}}" ({{frameworkCode}}) start date is {{startDate}}.'
WHERE trigger_type = 'AUDIT_START_DATE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_START_DATE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Audit submitted for Admin sign-off: {{name}}',
    body_template = 'Auditor signed audit "{{name}}" - awaiting Admin second sign.'
WHERE trigger_type = 'AUDIT_SUBMITTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'AUDIT_SUBMITTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Recovery plan needs approval: {{planName}}',
    body_template = 'Recovery plan "{{planName}}" awaits Owner approval.'
WHERE trigger_type = 'BCP_APPROVAL_NEEDED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BCP_APPROVAL_NEEDED') v);

UPDATE notification_template SET subject_template = '[FYI] - Recovery plan approved: {{planName}}',
    body_template = 'Recovery plan "{{planName}}" (v{{version}}) was approved.'
WHERE trigger_type = 'BCP_APPROVED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BCP_APPROVED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Recovery plan expired: {{planName}}',
    body_template = 'Recovery plan "{{planName}}" is past its review date.'
WHERE trigger_type = 'BCP_EXPIRED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BCP_EXPIRED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Recovery plan review due: {{planName}}',
    body_template = 'Recovery plan "{{planName}}" review date {{reviewDate}} is approaching.'
WHERE trigger_type = 'BCP_REVIEW_DUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BCP_REVIEW_DUE') v);

UPDATE notification_template SET subject_template = '[FYI] - BIA approved: {{processName}}',
    body_template = 'BIA "{{processName}}" was approved.'
WHERE trigger_type = 'BIA_APPROVED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_APPROVED') v);

UPDATE notification_template SET subject_template = '[Action Required] - BIA assigned: {{processName}}',
    body_template = 'BIA "{{processName}}" was assigned to you. Due {{dueDate}}.'
WHERE trigger_type = 'BIA_ASSIGNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_ASSIGNED') v);

UPDATE notification_template SET subject_template = '[FYI] - BIA closed: {{processName}}',
    body_template = 'Department BIA "{{processName}}" was closed.'
WHERE trigger_type = 'BIA_CLOSED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_CLOSED') v);

UPDATE notification_template SET subject_template = '[Action Required] - BIA overdue: {{processName}}',
    body_template = 'BIA "{{processName}}" is past due ({{dueDate}}) and not submitted.'
WHERE trigger_type = 'BIA_NOT_SUBMITTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_NOT_SUBMITTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - BIA ready for BC Head: {{processName}}',
    body_template = 'Department BIA "{{processName}}" is waiting for Business Continuity Head review.'
WHERE trigger_type = 'BIA_PENDING_BC_HEAD'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_PENDING_BC_HEAD') v);

UPDATE notification_template SET subject_template = '[FYI] - BIA approved by BC Head: {{processName}}',
    body_template = 'BC Head approved BIA "{{processName}}". Close the department BIA when ready.'
WHERE trigger_type = 'BIA_READY_TO_CLOSE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_READY_TO_CLOSE') v);

UPDATE notification_template SET subject_template = '[Action Required] - BIA returned: {{processName}}',
    body_template = 'BIA "{{processName}}" was returned: {{reason}}'
WHERE trigger_type = 'BIA_RETURNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_RETURNED') v);

UPDATE notification_template SET subject_template = '[Action Required] - BIA returned by BC Head: {{processName}}',
    body_template = 'BC Head returned BIA "{{processName}}": {{reason}}'
WHERE trigger_type = 'BIA_RETURNED_TO_ADMIN'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_RETURNED_TO_ADMIN') v);

UPDATE notification_template SET subject_template = '[Action Required] - BIA review due: {{processName}}',
    body_template = 'BIA "{{processName}}" review date {{reviewDate}} is approaching.'
WHERE trigger_type = 'BIA_REVIEW_DUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_REVIEW_DUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - BIA submitted: {{processName}}',
    body_template = 'Contributor submitted BIA "{{processName}}" for review.'
WHERE trigger_type = 'BIA_SUBMITTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BIA_SUBMITTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Breach 48h warning: {{displayId}}',
    body_template = 'Breach {{displayId}} ({{title}}) - 24 hours remain before the 72-hour PDPL notification deadline ({{reportDeadlineAt}}).'
WHERE trigger_type = 'BREACH_48H_WARNING'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BREACH_48H_WARNING') v);

UPDATE notification_template SET subject_template = '[Action Required] - Breach OVERDUE - 72h clock: {{displayId}}',
    body_template = 'CRITICAL: Breach {{displayId}} ({{title}}) has passed the 72-hour notification deadline ({{reportDeadlineAt}}) without authority reporting.'
WHERE trigger_type = 'BREACH_72H_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BREACH_72H_OVERDUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Personal data breach logged: {{displayId}}',
    body_template = 'Breach {{displayId}} - {{title}} was logged. 72-hour authority notification deadline: {{reportDeadlineAt}}.'
WHERE trigger_type = 'BREACH_RECORDED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'BREACH_RECORDED') v);

UPDATE notification_template SET subject_template = '[Action Required] - CAPA assigned: {{title}}',
    body_template = 'CAPA "{{title}}" was assigned to you. Target: {{targetDate}}.'
WHERE trigger_type = 'CAPA_ASSIGNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'CAPA_ASSIGNED') v);

UPDATE notification_template SET subject_template = '[FYI] - CAPA closed: {{title}}',
    body_template = 'CAPA "{{title}}" was closed.'
WHERE trigger_type = 'CAPA_CLOSED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'CAPA_CLOSED') v);

UPDATE notification_template SET subject_template = '[FYI] - CAPA opened: {{title}}',
    body_template = 'A CAPA was opened ({{registerCode}}): {{title}}.'
WHERE trigger_type = 'CAPA_CREATED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'CAPA_CREATED') v);

UPDATE notification_template SET subject_template = '[Action Required] - CAPA due soon: {{title}}',
    body_template = 'CAPA "{{title}}" is due on {{targetDate}} (within 3 days).'
WHERE trigger_type = 'CAPA_DUE_SOON'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'CAPA_DUE_SOON') v);

UPDATE notification_template SET subject_template = '[Action Required] - CAPA overdue: {{title}}',
    body_template = 'CAPA "{{title}}" is overdue (target {{targetDate}}).'
WHERE trigger_type = 'CAPA_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'CAPA_OVERDUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - CAPA overdue escalation: {{title}}',
    body_template = 'CAPA "{{title}}" remains overdue 7+ days past target {{targetDate}}.'
WHERE trigger_type = 'CAPA_OVERDUE_ESCALATION'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'CAPA_OVERDUE_ESCALATION') v);

UPDATE notification_template SET subject_template = '[Action Required] - CAPA returned: {{title}}',
    body_template = 'CAPA "{{title}}" was returned: {{reason}}'
WHERE trigger_type = 'CAPA_REJECTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'CAPA_REJECTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - CAPA submitted: {{title}}',
    body_template = 'Owner submitted CAPA "{{title}}" for closure review.'
WHERE trigger_type = 'CAPA_SUBMITTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'CAPA_SUBMITTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - DPIA required: {{activityName}}',
    body_template = 'ROPA activity "{{activityName}}" ({{displayId}}) requires a Data Protection Impact Assessment before approval.'
WHERE trigger_type = 'DPIA_REQUIRED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'DPIA_REQUIRED') v);

UPDATE notification_template SET subject_template = '[Action Required] - DSR day 20 warning: {{displayId}}',
    body_template = 'DSR {{displayId}} ({{requestType}} for {{subjectName}}) has 10 days remaining. Due date: {{dueAt}}.'
WHERE trigger_type = 'DSR_DAY_20'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'DSR_DAY_20') v);

UPDATE notification_template SET subject_template = '[Action Required] - DSR day 28 urgent: {{displayId}}',
    body_template = 'URGENT: DSR {{displayId}} ({{requestType}} for {{subjectName}}) has 2 days remaining. Due date: {{dueAt}}.'
WHERE trigger_type = 'DSR_DAY_28'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'DSR_DAY_28') v);

UPDATE notification_template SET subject_template = '[Action Required] - DSR OVERDUE - PDPL breach risk: {{displayId}}',
    body_template = 'CRITICAL: DSR {{displayId}} ({{requestType}} for {{subjectName}}) is past the 30-day UAE PDPL deadline (due {{dueAt}}). Immediate action required.'
WHERE trigger_type = 'DSR_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'DSR_OVERDUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - New DSR received: {{displayId}}',
    body_template = 'A data subject request ({{requestType}}) for {{subjectName}} has been logged as {{displayId}}. Response is due by {{dueAt}}.'
WHERE trigger_type = 'DSR_RECEIVED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'DSR_RECEIVED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Evidence expired: {{title}}',
    body_template = 'Evidence "{{title}}" expired on {{expiresOn}}.'
WHERE trigger_type = 'EVIDENCE_EXPIRED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'EVIDENCE_EXPIRED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Evidence expiring: {{title}}',
    body_template = 'Evidence "{{title}}" expires on {{expiryDate}}.'
WHERE trigger_type = 'EVIDENCE_EXPIRING'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'EVIDENCE_EXPIRING') v);

UPDATE notification_template SET subject_template = '[Action Required] - Exercise due: {{exerciseName}}',
    body_template = 'BCMS exercise "{{exerciseName}}" is scheduled for {{scheduledDate}}.'
WHERE trigger_type = 'EXERCISE_DUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'EXERCISE_DUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Exercise overdue: {{exerciseName}}',
    body_template = 'BCMS exercise "{{exerciseName}}" is overdue ({{scheduledDate}}).'
WHERE trigger_type = 'EXERCISE_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'EXERCISE_OVERDUE') v);

UPDATE notification_template SET subject_template = '[FYI] - Gap assessment completed - {{assessmentName}}',
    body_template = 'Gap assessment {{assessmentName}} ({{frameworkCode}}) has been marked complete. Overall score: {{overallScore}}.'
WHERE trigger_type = 'GAP_ASSESSMENT_COMPLETED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'GAP_ASSESSMENT_COMPLETED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Gap control assigned: {{nodeCode}}',
    body_template = 'Control {{nodeCode}} on assessment {{assessmentName}} was assigned to you.'
WHERE trigger_type = 'GAP_CONTROL_ASSIGNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'GAP_CONTROL_ASSIGNED') v);

UPDATE notification_template SET subject_template = '[FYI] - Gap control finalized: {{nodeCode}}',
    body_template = 'Admin finalized {{nodeCode}} on {{assessmentName}}.'
WHERE trigger_type = 'GAP_CONTROL_FINALIZED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'GAP_CONTROL_FINALIZED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Gap control sent back: {{nodeCode}}',
    body_template = 'Admin sent back {{nodeCode}}: {{reason}}'
WHERE trigger_type = 'GAP_CONTROL_SENT_BACK'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'GAP_CONTROL_SENT_BACK') v);

UPDATE notification_template SET subject_template = '[Action Required] - Gap control submitted: {{nodeCode}}',
    body_template = 'Contributor submitted {{nodeCode}} on {{assessmentName}} for review.'
WHERE trigger_type = 'GAP_CONTROL_SUBMITTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'GAP_CONTROL_SUBMITTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Gap control overdue: {{nodeCode}}',
    body_template = 'Control {{nodeCode}} ({{title}}) is past due ({{dueDate}}) and not submitted.'
WHERE trigger_type = 'GAP_NOT_RESPONDED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'GAP_NOT_RESPONDED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Incident assigned',
    body_template = 'Incident {{displayId}} ({{title}}) was assigned to you. Priority {{priority}}, response due {{responseDue}}.'
WHERE trigger_type = 'INCIDENT_ASSIGNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'INCIDENT_ASSIGNED') v);

UPDATE notification_template SET subject_template = '[FYI] - Incident closed',
    body_template = 'Incident {{displayId}} ({{title}}) was closed by Admin.'
WHERE trigger_type = 'INCIDENT_CLOSED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'INCIDENT_CLOSED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Incident response overdue',
    body_template = 'Incident {{displayId}} ({{title}}) is past its response target ({{responseDue}}).'
WHERE trigger_type = 'INCIDENT_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'INCIDENT_OVERDUE') v);

UPDATE notification_template SET subject_template = '[Critical] - P1 incident reported',
    body_template = 'P1 Critical incident {{displayId}} ({{title}}) was reported. Immediate response required (target {{responseDue}}).'
WHERE trigger_type = 'INCIDENT_P1_REPORTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'INCIDENT_P1_REPORTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - P2 incident reported',
    body_template = 'P2 High incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.'
WHERE trigger_type = 'INCIDENT_P2_REPORTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'INCIDENT_P2_REPORTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - P3 incident reported',
    body_template = 'P3 Medium incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.'
WHERE trigger_type = 'INCIDENT_P3_REPORTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'INCIDENT_P3_REPORTED') v);

UPDATE notification_template SET subject_template = '[FYI] - P4 incident reported',
    body_template = 'P4 Low incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.'
WHERE trigger_type = 'INCIDENT_P4_REPORTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'INCIDENT_P4_REPORTED') v);

UPDATE notification_template SET subject_template = '[FYI] - Incident resolved',
    body_template = 'Incident {{displayId}} ({{title}}) was marked Resolved.'
WHERE trigger_type = 'INCIDENT_RESOLVED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'INCIDENT_RESOLVED') v);

UPDATE notification_template SET subject_template = '[Warning] - KRI Amber breach',
    body_template = 'KRI {{displayId}} ({{name}}) entered Amber (value {{value}}, threshold {{threshold}}).'
WHERE trigger_type = 'KRI_AMBER_BREACH'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'KRI_AMBER_BREACH') v);

UPDATE notification_template SET subject_template = '[FYI] - KRI Red acknowledged',
    body_template = 'KRI {{displayId}} ({{name}}) Red breach was acknowledged by Admin.'
WHERE trigger_type = 'KRI_RED_ACKNOWLEDGED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'KRI_RED_ACKNOWLEDGED') v);

UPDATE notification_template SET subject_template = '[Critical] - KRI Red breach',
    body_template = 'KRI {{displayId}} ({{name}}) entered Red (value {{value}}, threshold {{threshold}}). Acknowledge required.'
WHERE trigger_type = 'KRI_RED_BREACH'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'KRI_RED_BREACH') v);

UPDATE notification_template SET subject_template = '[Action Required] - KRI value due',
    body_template = 'KRI {{displayId}} ({{name}}) is due for measurement on {{dueDate}}.'
WHERE trigger_type = 'KRI_VALUE_DUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'KRI_VALUE_DUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - KRI value overdue',
    body_template = 'KRI {{displayId}} ({{name}}) is overdue for measurement (due {{dueDate}}).'
WHERE trigger_type = 'KRI_VALUE_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'KRI_VALUE_OVERDUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Emergency password login used',
    body_template = '{{email}} signed in with email and password while Microsoft sign-in is enabled for this workspace.'
WHERE trigger_type = 'LOCAL_AUTH_USED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'LOCAL_AUTH_USED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Policy acknowledgement due soon: {{title}}',
    body_template = 'Acknowledgement for "{{title}}" is due by {{ackDeadline}}.'
WHERE trigger_type = 'POLICY_ACK_DEADLINE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'POLICY_ACK_DEADLINE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Policy acknowledgements overdue: {{title}}',
    body_template = 'Some users have not acknowledged "{{title}}" (deadline {{ackDeadline}}).'
WHERE trigger_type = 'POLICY_ACK_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'POLICY_ACK_OVERDUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Acknowledge policy: {{title}}',
    body_template = 'Please read and acknowledge "{{title}}" (v{{version}}).'
WHERE trigger_type = 'POLICY_ACK_REQUIRED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'POLICY_ACK_REQUIRED') v);

UPDATE notification_template SET subject_template = '[FYI] - Policy approved: {{title}}',
    body_template = 'Policy "{{title}}" (v{{version}}) was approved. Acknowledgement may be required.'
WHERE trigger_type = 'POLICY_APPROVED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'POLICY_APPROVED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Policy due for review: {{title}}',
    body_template = 'Policy "{{title}}" is past its review date and marked Due for Review.'
WHERE trigger_type = 'POLICY_EXPIRED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'POLICY_EXPIRED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Policy review due soon: {{title}}',
    body_template = 'Policy "{{title}}" review date {{reviewDate}} is approaching.'
WHERE trigger_type = 'POLICY_REVIEW_DUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'POLICY_REVIEW_DUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Policy submitted for review: {{title}}',
    body_template = 'Policy "{{title}}" was submitted for review.'
WHERE trigger_type = 'POLICY_SUBMITTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'POLICY_SUBMITTED') v);

UPDATE notification_template SET subject_template = '[FYI] - Scheduled report ready - {{reportTitle}}',
    body_template = 'Your scheduled {{reportTitle}} ({{format}}) is ready. Open Reports to download from delivery history.'
WHERE trigger_type = 'REPORT_SCHEDULED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'REPORT_SCHEDULED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk assigned: {{displayId}}',
    body_template = 'Risk {{displayId}} ({{title}}) was assigned to you.'
WHERE trigger_type = 'RISK_ASSIGNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_ASSIGNED') v);

UPDATE notification_template SET subject_template = '[FYI] - Risk auto-closed',
    body_template = 'Risk {{displayId}} ({{title}}) was auto-closed because residual risk is {{residualRisk}}. No treatment workflow is required.'
WHERE trigger_type = 'RISK_AUTO_CLOSED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_AUTO_CLOSED') v);

UPDATE notification_template SET subject_template = '[FYI] - Risk matrix updated',
    body_template = 'The tenant risk matrices were updated (version {{version}}). Reason: {{reason}}. Existing risk scores are not recalculated.'
WHERE trigger_type = 'RISK_MATRIX_CHANGED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_MATRIX_CHANGED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk mitigating: {{displayId}}',
    body_template = 'Risk {{displayId}} treatment is in progress.'
WHERE trigger_type = 'RISK_MITIGATING'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_MITIGATING') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk not responded',
    body_template = 'Risk {{displayId}} ({{title}}) has had no Contributor action for 3+ days since assignment.'
WHERE trigger_type = 'RISK_NOT_RESPONDED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_NOT_RESPONDED') v);

UPDATE notification_template SET subject_template = '[Escalation] - Risk overdue 7 days',
    body_template = 'Risk {{displayId}} ({{title}}) has been overdue for 7+ days and is escalated to the Owner.'
WHERE trigger_type = 'RISK_OVERDUE_ESCALATION'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_OVERDUE_ESCALATION') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk Owner not responded',
    body_template = 'Risk {{displayId}} ({{title}}) awaits Risk Owner acceptance - no action for 5+ days.'
WHERE trigger_type = 'RISK_OWNER_NOT_RESPONDED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_OWNER_NOT_RESPONDED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk Owner rejected',
    body_template = 'Risk {{displayId}} ({{title}}) acceptance was rejected by the Risk Owner. Reason: {{reason}}'
WHERE trigger_type = 'RISK_OWNER_REJECTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_OWNER_REJECTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk pending Admin: {{displayId}}',
    body_template = 'Risk {{displayId}} awaits Admin decision.'
WHERE trigger_type = 'RISK_PENDING_ADMIN'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_PENDING_ADMIN') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk pending Owner: {{displayId}}',
    body_template = 'Risk {{displayId}} awaits Risk Owner acceptance.'
WHERE trigger_type = 'RISK_PENDING_OWNER'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_PENDING_OWNER') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk returned',
    body_template = 'Risk {{displayId}} ({{title}}) was returned for revision. Reason: {{reason}}'
WHERE trigger_type = 'RISK_RETURNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_RETURNED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk review due',
    body_template = 'Risk {{displayId}} ({{title}}) Next Review Date {{reviewDate}} is within 30 days.'
WHERE trigger_type = 'RISK_REVIEW_DUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_REVIEW_DUE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk re-opened',
    body_template = 'Risk {{displayId}} ({{title}}) was re-opened. Reason: {{reason}}'
WHERE trigger_type = 'RISK_RE_OPENED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_RE_OPENED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk submitted: {{displayId}}',
    body_template = 'Contributor submitted risk {{displayId}} for validation.'
WHERE trigger_type = 'RISK_SUBMITTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_SUBMITTED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Tolerance returned: {{displayId}}',
    body_template = 'Tolerated risk {{displayId}} returned for re-assessment.'
WHERE trigger_type = 'RISK_TOLERANCE_RETURNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_TOLERANCE_RETURNED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Tolerance review due: {{displayId}}',
    body_template = 'Tolerated risk {{displayId}} is approaching its review date.'
WHERE trigger_type = 'RISK_TOLERANCE_REVIEW_DUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_TOLERANCE_REVIEW_DUE') v);

UPDATE notification_template SET subject_template = '[FYI] - Risk tolerated: {{displayId}}',
    body_template = 'Risk {{displayId}} was accepted / tolerated.'
WHERE trigger_type = 'RISK_TOLERATED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_TOLERATED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Risk validation rejected: {{displayId}}',
    body_template = 'Risk {{displayId}} was sent back for revision.'
WHERE trigger_type = 'RISK_VALIDATION_REJECTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'RISK_VALIDATION_REJECTED') v);

UPDATE notification_template SET subject_template = '[FYI] - Your Arbor GRC roles were updated',
    body_template = 'Your roles on workspace {{workspaceId}} are now: {{roles}}. Sign in again if menus look wrong.'
WHERE trigger_type = 'ROLE_CHANGED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'ROLE_CHANGED') v);

UPDATE notification_template SET subject_template = '[FYI] - New user created from Microsoft sign-in',
    body_template = '{{email}} signed in with Microsoft and was created as {{role}}. Assign organisation units if needed.'
WHERE trigger_type = 'SSO_JIT_PROVISIONED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'SSO_JIT_PROVISIONED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Microsoft sign-in blocked',
    body_template = 'Someone tried to sign in with Microsoft as {{email}}, but they have no Arbor account yet. Invite them if they should have access.'
WHERE trigger_type = 'SSO_NO_ROLE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'SSO_NO_ROLE') v);

UPDATE notification_template SET subject_template = '[Action Required] - Treatment overdue: {{displayId}}',
    body_template = 'Treatment for {{displayId}} is past target date {{targetDate}}.'
WHERE trigger_type = 'TREATMENT_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'TREATMENT_OVERDUE') v);

UPDATE notification_template SET subject_template = '[FYI] - Your Arbor GRC account was deactivated',
    body_template = 'Your account ({{email}}) on workspace {{workspaceId}} was deactivated. You can no longer sign in. Contact your administrator if this is unexpected.'
WHERE trigger_type = 'USER_DEACTIVATED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'USER_DEACTIVATED') v);

UPDATE notification_template SET subject_template = '[Action Required] - You are invited to Arbor GRC',
    body_template = 'You have been invited to Arbor GRC as {{roles}}. {{signInHint}}'
WHERE trigger_type = 'USER_INVITED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'USER_INVITED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Vendor contract expiry: {{vendorName}}',
    body_template = 'Contract {{contractCode}} for {{vendorName}} ends on {{contractEnd}}.'
WHERE trigger_type = 'VENDOR_CONTRACT_EXPIRY'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_CONTRACT_EXPIRY') v);

UPDATE notification_template SET subject_template = '[Action Required] - High-risk vendor detected: {{vendorName}}',
    body_template = 'Vendor "{{vendorName}}" scored a {{residualRisk}} residual risk on assessment "{{assessmentTitle}}". Consider raising a linked ERM risk.'
WHERE trigger_type = 'VENDOR_HIGH_RISK'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_HIGH_RISK') v);

UPDATE notification_template SET subject_template = '[Action Required] - Vendor not compliant: {{vendorName}}',
    body_template = 'Vendor "{{vendorName}}" did not meet the tier posture target on {{displayId}}. Consider raising a linked ERM risk.'
WHERE trigger_type = 'VENDOR_NOT_COMPLIANT'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_NOT_COMPLIANT') v);

UPDATE notification_template SET subject_template = '[Action Required] - Your vendor questionnaire code',
    body_template = 'Your one-time code for the vendor security questionnaire is {{otpCode}}. It is valid until {{expiresAt}}.'
WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_OTP'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_OTP') v);

UPDATE notification_template SET subject_template = '[Action Required] - Questionnaire returned: {{displayId}}',
    body_template = 'Some controls on "{{assessmentTitle}}" were returned for {{vendorName}}. Please reopen the secure link and update the returned answers.'
WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_RETURNED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_RETURNED') v);

UPDATE notification_template SET subject_template = '[Action Required] - Security questionnaire for {{vendorName}}',
    body_template = 'Please complete the security questionnaire for {{vendorName}}. Open the secure link: {{accessLink}} (expires {{expiresAt}}). A one-time code follows in a separate email.'
WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_SENT'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_SENT') v);

UPDATE notification_template SET subject_template = '[Action Required] - Vendor questionnaire submitted: {{displayId}}',
    body_template = 'The vendor has submitted their responses for "{{assessmentTitle}}" ({{displayId}}). Please review and score the assessment.'
WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_SUBMITTED'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_QUESTIONNAIRE_SUBMITTED') v);

UPDATE notification_template SET subject_template = '[FYI] - TPRM review complete: {{vendorName}}',
    body_template = 'Review of {{displayId}} for {{vendorName}} is complete. Posture {{posturePercent}}%. Compliance: {{complianceStatus}}.'
WHERE trigger_type = 'VENDOR_REVIEW_COMPLETE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_REVIEW_COMPLETE') v);

UPDATE notification_template SET subject_template = '[Reminder] Vendor review due - {{vendorName}}',
    body_template = 'Vendor {{vendorName}} is due for review on {{reviewDate}}. Residual risk: {{residualRisk}}.'
WHERE trigger_type = 'VENDOR_REVIEW_DUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_REVIEW_DUE') v);

UPDATE notification_template SET subject_template = '[Overdue] Vendor review past due - {{vendorName}}',
    body_template = 'Vendor {{vendorName}} review was due on {{reviewDate}}. Please complete the review.'
WHERE trigger_type = 'VENDOR_REVIEW_OVERDUE'
  AND version = (SELECT MAX(version) FROM (SELECT version FROM notification_template WHERE trigger_type = 'VENDOR_REVIEW_OVERDUE') v);
