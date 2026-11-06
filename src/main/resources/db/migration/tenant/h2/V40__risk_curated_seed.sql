-- J9: link IS risks seeded from curated library / Compliance Project.

ALTER TABLE risk_record ADD COLUMN source_compliance_project_id BIGINT;
ALTER TABLE risk_record ADD COLUMN source_risk_template_id BIGINT;

CREATE INDEX idx_risk_source_project ON risk_record (source_compliance_project_id);
