-- J9: link IS risks seeded from curated library / Compliance Project.

ALTER TABLE risk_record ADD source_compliance_project_id BIGINT NULL;
ALTER TABLE risk_record ADD source_risk_template_id BIGINT NULL;
GO

CREATE INDEX idx_risk_source_project ON risk_record (source_compliance_project_id);
