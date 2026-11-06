-- One CAPA Register per Audit Project (Excel journey STEP 4). Tracker rows live on capa.

CREATE TABLE grc_capa_register (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    audit_id            BIGINT         NOT NULL,
    conducted_by        NVARCHAR(200)  NOT NULL,
    description         NVARCHAR(MAX)  NOT NULL,
    created_by_user_id  BIGINT         NOT NULL,
    created_at          DATETIME2      NOT NULL CONSTRAINT DF_capa_register_created DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2      NOT NULL CONSTRAINT DF_capa_register_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_capa_register_audit UNIQUE (audit_id),
    CONSTRAINT fk_capa_register_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id),
    CONSTRAINT fk_capa_register_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_capa_register_audit')
    CREATE INDEX idx_capa_register_audit ON grc_capa_register (audit_id);
GO

IF COL_LENGTH('capa', 'capa_register_id') IS NULL
    ALTER TABLE capa ADD capa_register_id BIGINT NULL;
GO

INSERT INTO grc_capa_register (audit_id, conducted_by, description, created_by_user_id, created_at, updated_at)
SELECT a.audit_id,
       COALESCE(NULLIF(LTRIM(RTRIM(c.conducted_by)), ''), N'Admin'),
       COALESCE(c.description, N'CAPA register'),
       COALESCE(c.assigned_by_user_id, (SELECT MIN(id) FROM app_user)),
       c.created_at,
       c.updated_at
FROM (
    SELECT audit_id, MIN(id) AS capa_id FROM capa WHERE audit_id IS NOT NULL GROUP BY audit_id
) a
JOIN capa c ON c.id = a.capa_id
WHERE NOT EXISTS (SELECT 1 FROM grc_capa_register r WHERE r.audit_id = a.audit_id);
GO

UPDATE c
SET capa_register_id = r.id
FROM capa c
INNER JOIN grc_capa_register r ON r.audit_id = c.audit_id
WHERE c.audit_id IS NOT NULL AND c.capa_register_id IS NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_capa_register')
    ALTER TABLE capa ADD CONSTRAINT fk_capa_register
        FOREIGN KEY (capa_register_id) REFERENCES grc_capa_register (id);
GO
