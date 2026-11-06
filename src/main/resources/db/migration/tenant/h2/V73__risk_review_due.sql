-- Excel Next Review Date (AL) reminder — distinct from Tolerate-only RISK_TOLERANCE_REVIEW_DUE.

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'RISK_REVIEW_DUE',
       '[Action Required] - Risk review due - Risk',
       'Risk {{displayId}} ({{title}}) Next Review Date {{reviewDate}} is within 30 days.',
       TRUE, TRUE, 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'RISK_REVIEW_DUE');
