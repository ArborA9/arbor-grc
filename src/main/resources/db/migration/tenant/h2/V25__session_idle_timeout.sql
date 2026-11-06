-- Path C B3: tenant-configurable SPA idle logout threshold (minutes). Default 5, max enforced at API (480).
ALTER TABLE org_profile ADD COLUMN idle_timeout_minutes INT NOT NULL DEFAULT 5;
