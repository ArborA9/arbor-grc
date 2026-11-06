-- Path C B1: JIT provisioning settings on the tenant's IdP config.
ALTER TABLE tenant ADD jit_enabled BIT NOT NULL
    CONSTRAINT DF_tenant_jit_enabled DEFAULT 0;
ALTER TABLE tenant ADD jit_default_role NVARCHAR(32) NOT NULL
    CONSTRAINT DF_tenant_jit_default_role DEFAULT 'BLOCK';
