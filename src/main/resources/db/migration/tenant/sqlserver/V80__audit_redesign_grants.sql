-- Audit redesign (Excel MVP v2): display IDs, statuses, access grants, view log, checklist working paper.

ALTER TABLE grc_audit ADD display_id VARCHAR(20) NULL;
ALTER TABLE grc_audit ADD auditor_organisation NVARCHAR(120) NULL;
ALTER TABLE grc_audit ADD auditee_org_unit_id BIGINT NULL;
ALTER TABLE grc_audit ADD overall_conclusion NVARCHAR(MAX) NULL;
ALTER TABLE grc_audit ADD report_issued_date DATE NULL;
ALTER TABLE grc_audit ADD grace_days INT NOT NULL CONSTRAINT df_grc_audit_grace DEFAULT 14;
GO

UPDATE grc_audit SET display_id = CONCAT('AUD-', YEAR(created_at), '-', RIGHT(CONCAT('000', CAST(id AS VARCHAR(12))), 3))
WHERE display_id IS NULL;
GO

ALTER TABLE grc_audit ALTER COLUMN display_id VARCHAR(20) NOT NULL;
CREATE UNIQUE INDEX uq_grc_audit_display_id ON grc_audit (display_id);
ALTER TABLE grc_audit ADD CONSTRAINT fk_grc_audit_auditee
    FOREIGN KEY (auditee_org_unit_id) REFERENCES org_unit (id);
GO

UPDATE grc_audit SET status = 'DRAFT' WHERE status = 'PLANNED';
UPDATE grc_audit SET status = 'IN_PROGRESS' WHERE status = 'RETURNED';
UPDATE grc_audit SET status = 'REPORT_ISSUED' WHERE status = 'PENDING_ADMIN';
UPDATE grc_audit SET status = 'CLOSED' WHERE status = 'COMPLETED';
GO

ALTER TABLE grc_audit_checklist_item ADD section NVARCHAR(120) NULL;
ALTER TABLE grc_audit_checklist_item ADD test_focus NVARCHAR(MAX) NULL;
ALTER TABLE grc_audit_checklist_item ADD evidence_module NVARCHAR(300) NULL;
ALTER TABLE grc_audit_checklist_item ADD in_scope BIT NOT NULL CONSTRAINT df_audit_item_scope DEFAULT 1;
ALTER TABLE grc_audit_checklist_item ADD evidence_examined NVARCHAR(MAX) NULL;
ALTER TABLE grc_audit_checklist_item ADD persons_interviewed NVARCHAR(300) NULL;
ALTER TABLE grc_audit_checklist_item ADD reviewed_date DATE NULL;
ALTER TABLE grc_audit_checklist_item ADD conclusion VARCHAR(32) NULL;
ALTER TABLE grc_audit_checklist_item ADD finding_id BIGINT NULL;
GO

CREATE TABLE audit_ref_seq (
    kind     VARCHAR(16) NOT NULL,
    year_num INT         NOT NULL,
    next_val INT         NOT NULL,
    CONSTRAINT pk_audit_ref_seq PRIMARY KEY (kind, year_num)
);

CREATE TABLE audit_access_grant (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    display_id              VARCHAR(32)  NOT NULL,
    audit_id                BIGINT       NOT NULL,
    user_id                 BIGINT       NOT NULL,
    grantee_type            VARCHAR(32)  NOT NULL,
    organisation            NVARCHAR(120) NULL,
    nda_reference           VARCHAR(60) NULL,
    modules_json            NVARCHAR(MAX) NOT NULL,
    scope_boundary          NVARCHAR(500) NULL,
    can_preview_evidence    BIT          NOT NULL CONSTRAINT df_grant_preview DEFAULT 1,
    can_export_notes        BIT          NOT NULL CONSTRAINT df_grant_notes DEFAULT 1,
    can_download_evidence   BIT          NOT NULL CONSTRAINT df_grant_bulk DEFAULT 0,
    valid_from              DATE         NOT NULL,
    valid_to                DATE         NOT NULL,
    grace_days              INT          NOT NULL CONSTRAINT df_grant_grace DEFAULT 14,
    revoked                 BIT          NOT NULL CONSTRAINT df_grant_revoked DEFAULT 0,
    revoked_reason          NVARCHAR(MAX) NULL,
    revoked_at              DATETIME2 NULL,
    revoked_by_user_id      BIGINT NULL,
    created_at              DATETIME2    NOT NULL CONSTRAINT df_grant_created DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2    NOT NULL CONSTRAINT df_grant_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_audit_grant_display UNIQUE (display_id),
    CONSTRAINT fk_audit_grant_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_grant_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_audit_grant_audit ON audit_access_grant (audit_id);
CREATE INDEX idx_audit_grant_user ON audit_access_grant (user_id);

CREATE TABLE audit_access_log (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    grant_id        BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    viewed_at       DATETIME2    NOT NULL CONSTRAINT df_alog_viewed DEFAULT SYSUTCDATETIME(),
    module_code     VARCHAR(40)  NOT NULL,
    record_ref      VARCHAR(60)  NOT NULL,
    action          VARCHAR(40)  NOT NULL,
    document_name   NVARCHAR(250) NULL,
    classification  VARCHAR(30) NULL,
    within_scope    VARCHAR(32)  NOT NULL,
    CONSTRAINT fk_audit_log_grant FOREIGN KEY (grant_id) REFERENCES audit_access_grant (id)
);

CREATE INDEX idx_audit_access_log_grant ON audit_access_log (grant_id);
GO

INSERT INTO audit_access_grant (
    display_id, audit_id, user_id, grantee_type, modules_json, scope_boundary,
    valid_from, valid_to, grace_days, revoked, created_at, updated_at
)
SELECT
    CONCAT('GR-MIG-', t.audit_id, '-', t.user_id),
    t.audit_id,
    t.user_id,
    CASE WHEN EXISTS (
        SELECT 1 FROM user_role ur WHERE ur.user_id = t.user_id AND ur.role = 'EXTERNAL_AUDITOR'
    ) THEN 'EXTERNAL_AUDITOR' ELSE 'INTERNAL_AUDITOR' END,
    '["GAP","SOA","RISK","EVIDENCE"]',
    'Migrated from engagement team',
    COALESCE(a.start_date, CAST(SYSUTCDATETIME() AS DATE)),
    COALESCE(a.end_date, DATEADD(DAY, 14, CAST(SYSUTCDATETIME() AS DATE))),
    14,
    0,
    SYSUTCDATETIME(),
    SYSUTCDATETIME()
FROM grc_audit_team_member t
JOIN grc_audit a ON a.id = t.audit_id
WHERE NOT EXISTS (
    SELECT 1 FROM audit_access_grant g WHERE g.audit_id = t.audit_id AND g.user_id = t.user_id
);
GO

UPDATE grc_audit SET status = 'ACCESS_GRANTED'
WHERE status = 'DRAFT'
  AND EXISTS (SELECT 1 FROM audit_access_grant g WHERE g.audit_id = grc_audit.id AND g.revoked = 0);
GO

INSERT INTO audit_ref_seq (kind, year_num, next_val)
SELECT 'AUD', YEAR(SYSUTCDATETIME()), COALESCE(MAX(id), 0) + 1 FROM grc_audit;
INSERT INTO audit_ref_seq (kind, year_num, next_val)
SELECT 'GR', YEAR(SYSUTCDATETIME()), COALESCE((SELECT COUNT(*) FROM audit_access_grant), 0) + 1;
GO
