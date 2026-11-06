-- Per-tenant SSO / AD (OIDC) configuration. Identity from the tenant's AD; roles managed in Arbor.
ALTER TABLE tenant ADD COLUMN sso_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant ADD COLUMN oidc_issuer VARCHAR(1000);
ALTER TABLE tenant ADD COLUMN oidc_client_id VARCHAR(255);
ALTER TABLE tenant ADD COLUMN oidc_client_secret VARCHAR(500);
