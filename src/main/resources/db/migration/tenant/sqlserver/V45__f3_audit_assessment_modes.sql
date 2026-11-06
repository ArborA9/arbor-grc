-- F3 audit & compliance modes: assessment audit gap link + domain narratives.

ALTER TABLE grc_audit ADD gap_assessment_id BIGINT NULL;
GO
ALTER TABLE grc_audit ADD CONSTRAINT fk_audit_gap_assessment
    FOREIGN KEY (gap_assessment_id) REFERENCES gap_assessment (id);

ALTER TABLE grc_audit ADD domain_narrative_json NVARCHAR(MAX) NULL;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'GAP_ASSESSMENT_COMPLETED' AND version = 1)
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES ('GAP_ASSESSMENT_COMPLETED',
        N'Gap assessment completed — {{assessmentName}}',
        N'Gap assessment {{assessmentName}} ({{frameworkCode}}) has been marked complete. Overall score: {{overallScore}}.',
        1, 1, 1, 1);
