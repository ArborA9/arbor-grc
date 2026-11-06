-- Audit management (compliance) + CAPA registers 2/3 (req §16–17).

CREATE TABLE grc_audit (
    id                         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name                       NVARCHAR(255) NOT NULL,
    audit_type                 VARCHAR(32)   NOT NULL,
    audit_mode                 VARCHAR(32)   NOT NULL CONSTRAINT DF_grc_audit_mode DEFAULT 'COMPLIANCE',
    framework_id               BIGINT        NOT NULL,
    framework_code             VARCHAR(64)   NOT NULL,
    framework_name             NVARCHAR(255) NOT NULL,
    framework_version          VARCHAR(64)   NOT NULL,
    status                     VARCHAR(32)   NOT NULL CONSTRAINT DF_grc_audit_status DEFAULT 'PLANNED',
    start_date                 DATE          NULL,
    end_date                   DATE          NULL,
    scope_notes                NVARCHAR(MAX) NULL,
    executive_summary          NVARCHAR(MAX) NULL,
    lead_auditor_user_id       BIGINT        NOT NULL,
    created_by_user_id         BIGINT        NOT NULL,
    auditor_signed_at          DATETIME2     NULL,
    auditor_signed_by_user_id  BIGINT        NULL,
    admin_signed_at            DATETIME2     NULL,
    admin_signed_by_user_id    BIGINT        NULL,
    return_reason              NVARCHAR(MAX) NULL,
    locked                     BIT           NOT NULL CONSTRAINT DF_grc_audit_locked DEFAULT 0,
    created_at                 DATETIME2     NOT NULL CONSTRAINT DF_grc_audit_created DEFAULT SYSUTCDATETIME(),
    updated_at                 DATETIME2     NOT NULL CONSTRAINT DF_grc_audit_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_grc_audit_lead FOREIGN KEY (lead_auditor_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_grc_audit_created FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_grc_audit_auditor_sig FOREIGN KEY (auditor_signed_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_grc_audit_admin_sig FOREIGN KEY (admin_signed_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_grc_audit_status ON grc_audit (status);
CREATE INDEX idx_grc_audit_lead ON grc_audit (lead_auditor_user_id);
CREATE INDEX idx_grc_audit_type ON grc_audit (audit_type);

CREATE TABLE grc_audit_org_unit (
    audit_id    BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    CONSTRAINT PK_grc_audit_org_unit PRIMARY KEY (audit_id, org_unit_id),
    CONSTRAINT fk_grc_audit_ou_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_grc_audit_ou_unit FOREIGN KEY (org_unit_id) REFERENCES org_unit (id)
);

CREATE TABLE grc_audit_checklist_item (
    id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    audit_id           BIGINT        NOT NULL,
    framework_node_id  BIGINT        NOT NULL,
    node_code          VARCHAR(64)   NOT NULL,
    title              NVARCHAR(512) NOT NULL,
    assessment         VARCHAR(32)   NULL,
    notes              NVARCHAR(MAX) NULL,
    updated_by_user_id BIGINT        NULL,
    updated_at         DATETIME2     NOT NULL CONSTRAINT DF_grc_audit_item_upd DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_grc_audit_item_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_grc_audit_item_user FOREIGN KEY (updated_by_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_grc_audit_item_node UNIQUE (audit_id, framework_node_id)
);

CREATE INDEX idx_grc_audit_item_audit ON grc_audit_checklist_item (audit_id);

CREATE TABLE capa (
    id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    register_code        VARCHAR(32)   NOT NULL,
    source_type          VARCHAR(32)   NOT NULL,
    source_record_id     BIGINT        NULL,
    title                NVARCHAR(512) NOT NULL,
    description          NVARCHAR(MAX) NULL,
    recommended_action   NVARCHAR(MAX) NULL,
    control_ref          VARCHAR(100)  NULL,
    severity             VARCHAR(32)   NULL,
    priority             VARCHAR(32)   NOT NULL,
    status               VARCHAR(32)   NOT NULL CONSTRAINT DF_capa_status DEFAULT 'OPEN',
    owner_user_id        BIGINT        NULL,
    assigned_by_user_id  BIGINT        NULL,
    target_date          DATE          NULL,
    root_cause           NVARCHAR(MAX) NULL,
    corrective_action    NVARCHAR(MAX) NULL,
    preventive_action    NVARCHAR(MAX) NULL,
    progress_notes       NVARCHAR(MAX) NULL,
    rejection_reason     NVARCHAR(MAX) NULL,
    work_task_id         BIGINT        NULL,
    closed_at            DATETIME2     NULL,
    closed_by_user_id    BIGINT        NULL,
    created_at           DATETIME2     NOT NULL CONSTRAINT DF_capa_created DEFAULT SYSUTCDATETIME(),
    updated_at           DATETIME2     NOT NULL CONSTRAINT DF_capa_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_capa_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_capa_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_capa_closed_by FOREIGN KEY (closed_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_capa_register ON capa (register_code);
CREATE INDEX idx_capa_status ON capa (status);
CREATE INDEX idx_capa_owner ON capa (owner_user_id);

CREATE TABLE grc_audit_finding (
    id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    audit_id             BIGINT        NOT NULL,
    checklist_item_id    BIGINT        NULL,
    display_code         VARCHAR(64)   NOT NULL,
    severity             VARCHAR(32)   NOT NULL,
    title                NVARCHAR(512) NOT NULL,
    description          NVARCHAR(MAX) NOT NULL,
    recommended_action   NVARCHAR(MAX) NULL,
    control_ref          VARCHAR(100)  NULL,
    created_by_user_id   BIGINT        NOT NULL,
    capa_id              BIGINT        NULL,
    created_at           DATETIME2     NOT NULL CONSTRAINT DF_grc_finding_created DEFAULT SYSUTCDATETIME(),
    updated_at           DATETIME2     NOT NULL CONSTRAINT DF_grc_finding_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_grc_finding_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_grc_finding_item FOREIGN KEY (checklist_item_id) REFERENCES grc_audit_checklist_item (id),
    CONSTRAINT fk_grc_finding_user FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_grc_finding_capa FOREIGN KEY (capa_id) REFERENCES capa (id),
    CONSTRAINT uq_grc_finding_code UNIQUE (audit_id, display_code)
);

CREATE INDEX idx_grc_finding_audit ON grc_audit_finding (audit_id);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('AUDIT_PLANNED',
 N'Audit planned: {{auditName}}',
 N'You have been assigned as lead auditor for "{{auditName}}" ({{auditType}}). Start date: {{startDate}}.',
 1, 1, 1, 1),
('AUDIT_SUBMITTED',
 N'Audit submitted for approval: {{auditName}}',
 N'Lead auditor signed "{{auditName}}". Please review findings and sign as Admin (second signature).',
 1, 1, 1, 1),
('AUDIT_RETURNED',
 N'Audit returned: {{auditName}}',
 N'Admin returned "{{auditName}}" for revision. Reason: {{reason}}',
 1, 1, 1, 1),
('AUDIT_COMPLETED',
 N'Audit completed: {{auditName}}',
 N'Audit "{{auditName}}" is locked. Both signatures recorded. CAPAs created for applicable findings.',
 1, 1, 1, 1),
('CAPA_CREATED',
 N'CAPA opened: {{title}}',
 N'A new CAPA was opened in register {{register}} ({{controlRef}}). Admin will assign an action owner.',
 1, 1, 1, 1),
('CAPA_ASSIGNED',
 N'CAPA assigned: {{title}}',
 N'You have been assigned CAPA "{{title}}" ({{controlRef}}). Target date: {{targetDate}}.',
 1, 1, 1, 1),
('CAPA_SUBMITTED',
 N'CAPA submitted for verification: {{title}}',
 N'Action owner submitted CAPA "{{title}}". Please verify and close or reject.',
 1, 1, 1, 1),
('CAPA_REJECTED',
 N'CAPA rejected: {{title}}',
 N'Admin rejected CAPA "{{title}}". Reason: {{reason}}',
 1, 1, 1, 1),
('CAPA_CLOSED',
 N'CAPA closed: {{title}}',
 N'CAPA "{{title}}" has been closed.',
 1, 1, 1, 1);
