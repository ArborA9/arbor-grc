-- Excel SoT: named checklists per audit, working-paper rows, Admin CAPA tracker fields.

CREATE TABLE grc_audit_checklist (
    id           BIGINT IDENTITY PRIMARY KEY,
    audit_id     BIGINT        NOT NULL,
    template_key VARCHAR(32)   NOT NULL,
    name         NVARCHAR(200) NOT NULL,
    sort_order   INT           NOT NULL CONSTRAINT df_audit_checklist_sort DEFAULT 0,
    created_at   DATETIME2     NOT NULL CONSTRAINT df_audit_checklist_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_audit_checklist_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE
);
CREATE INDEX idx_audit_checklist_audit ON grc_audit_checklist (audit_id);
GO

ALTER TABLE grc_audit ADD audit_standard VARCHAR(32) NULL;
GO
UPDATE grc_audit SET audit_standard = 'ISO_27001' WHERE audit_standard IS NULL;
GO

ALTER TABLE grc_audit ALTER COLUMN framework_id BIGINT NULL;
GO

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = 'uq_grc_audit_item_node')
    ALTER TABLE grc_audit_checklist_item DROP CONSTRAINT uq_grc_audit_item_node;
GO

ALTER TABLE grc_audit_checklist_item ALTER COLUMN framework_node_id BIGINT NULL;
ALTER TABLE grc_audit_checklist_item ALTER COLUMN node_code VARCHAR(80) NOT NULL;
ALTER TABLE grc_audit_checklist_item ADD checklist_id BIGINT NULL;
ALTER TABLE grc_audit_checklist_item ADD sort_order INT NOT NULL CONSTRAINT df_audit_item_sort DEFAULT 0;
ALTER TABLE grc_audit_checklist_item ADD soa_applicability NVARCHAR(120) NULL;
ALTER TABLE grc_audit_checklist_item ADD finding_description NVARCHAR(MAX) NULL;
ALTER TABLE grc_audit_checklist_item ADD library_locked BIT NOT NULL CONSTRAINT df_audit_item_locked DEFAULT 1;
GO

INSERT INTO grc_audit_checklist (audit_id, template_key, name, sort_order)
SELECT id, 'CUSTOM', CONCAT('Checklist ', COALESCE(framework_code, CAST(id AS VARCHAR(20)))), 0
FROM grc_audit g
WHERE NOT EXISTS (SELECT 1 FROM grc_audit_checklist c WHERE c.audit_id = g.id);
GO

UPDATE i
SET checklist_id = c.id
FROM grc_audit_checklist_item i
CROSS APPLY (
    SELECT TOP 1 id FROM grc_audit_checklist WHERE audit_id = i.audit_id ORDER BY id
) c
WHERE i.checklist_id IS NULL;
GO

ALTER TABLE grc_audit_checklist_item ALTER COLUMN checklist_id BIGINT NOT NULL;
ALTER TABLE grc_audit_checklist_item ADD CONSTRAINT fk_audit_item_checklist
    FOREIGN KEY (checklist_id) REFERENCES grc_audit_checklist (id);
GO

ALTER TABLE capa ADD correction NVARCHAR(MAX) NULL;
ALTER TABLE capa ADD evidence_required NVARCHAR(MAX) NULL;
ALTER TABLE capa ADD contributor_department NVARCHAR(120) NULL;
GO
