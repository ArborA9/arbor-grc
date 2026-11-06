-- Path C B2: IdP group -> Arbor role mapping, applied (union) on SSO login.
CREATE TABLE sso_group_role_mapping (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    idp_group_name  NVARCHAR(255) NOT NULL,
    role            NVARCHAR(40)  NOT NULL,
    created_at      DATETIME2     NOT NULL CONSTRAINT DF_sso_group_role_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_sso_group_role UNIQUE (idp_group_name, role)
);

CREATE INDEX idx_sso_group_role_group ON sso_group_role_mapping (idp_group_name);
