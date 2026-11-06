-- Live IS seed progress: GET /api/risk-projects/{id} returns riskCount vs seedTotal while RUNNING.
ALTER TABLE risk_project ADD COLUMN IF NOT EXISTS seed_total INT NOT NULL DEFAULT 0;
ALTER TABLE risk_project ADD COLUMN IF NOT EXISTS seed_status VARCHAR(16) NOT NULL DEFAULT 'DONE';
