-- Immutable audit trail (req §8). Application must never UPDATE/DELETE rows.
CREATE TABLE audit_log (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id      VARCHAR(64)    NOT NULL,
    user_id        BIGINT         NULL,
    user_role      NVARCHAR(255)  NULL,
    action_type    VARCHAR(40)    NOT NULL,
    module         VARCHAR(64)    NOT NULL,
    record_type    VARCHAR(64)    NULL,
    record_id      VARCHAR(64)    NULL,
    record_name    NVARCHAR(500)  NULL,
    field_changed  NVARCHAR(255)  NULL,
    old_value      NVARCHAR(2000) NULL,
    new_value      NVARCHAR(2000) NULL,
    ip_address     VARCHAR(64)    NULL,
    auth_method    VARCHAR(16)    NULL,
    created_at     DATETIME2      NOT NULL CONSTRAINT DF_audit_log_created DEFAULT SYSUTCDATETIME()
);

CREATE INDEX idx_audit_log_created ON audit_log (created_at);
CREATE INDEX idx_audit_log_user ON audit_log (user_id);
CREATE INDEX idx_audit_log_action ON audit_log (action_type);
CREATE INDEX idx_audit_log_module ON audit_log (module);
