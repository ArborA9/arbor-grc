-- Framework Import V2 Phase 6C: soft-retire + Super Admin content audit.
ALTER TABLE framework_node ADD retired_at DATETIME2 NULL;
GO

CREATE TABLE catalog_content_audit (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    framework_id    BIGINT       NOT NULL,
    node_id         BIGINT       NULL,
    action          VARCHAR(32)  NOT NULL,
    field_name      VARCHAR(64)  NULL,
    old_value       NVARCHAR(MAX) NULL,
    new_value       NVARCHAR(MAX) NULL,
    actor_email     VARCHAR(255) NULL,
    created_at      DATETIME2    NOT NULL
);
GO

CREATE INDEX idx_cca_framework ON catalog_content_audit (framework_id);
CREATE INDEX idx_cca_node ON catalog_content_audit (node_id);
GO
