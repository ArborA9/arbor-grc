-- Per-tenant SSO / AD (OIDC) configuration. Identity from the tenant's AD; roles managed in Arbor.
ALTER TABLE tenant ADD sso_enabled BIT NOT NULL
    CONSTRAINT DF_tenant_sso_enabled DEFAULT 0;
ALTER TABLE tenant ADD oidc_issuer NVARCHAR(1000) NULL;
ALTER TABLE tenant ADD oidc_client_id NVARCHAR(255) NULL;
ALTER TABLE tenant ADD oidc_client_secret NVARCHAR(500) NULL;
