-- Risk engine v1 (IS Risk + ERM-ready). Matrices hardcoded in app for v1.

CREATE TABLE risk_record (
    id                         BIGINT IDENTITY(1,1) PRIMARY KEY,
    risk_type                  VARCHAR(16)    NOT NULL,
    display_id                 VARCHAR(32)    NOT NULL,
    title                      NVARCHAR(512)  NOT NULL,
    description                NVARCHAR(MAX)  NULL,
    category                   VARCHAR(64)    NOT NULL,
    status                     VARCHAR(32)    NOT NULL CONSTRAINT DF_risk_status DEFAULT 'DRAFT',
    org_unit_id                BIGINT         NULL,
    asset_id                   BIGINT         NULL,
    framework_id               BIGINT         NULL,
    framework_code             VARCHAR(64)    NULL,
    framework_node_id          BIGINT         NULL,
    node_code                  VARCHAR(64)    NULL,
    observed_gap               NVARCHAR(MAX)  NULL,
    threats                    NVARCHAR(MAX)  NULL,
    probability                VARCHAR(16)    NOT NULL,
    impact                     VARCHAR(16)    NOT NULL,
    inherent_risk              VARCHAR(16)    NOT NULL,
    existing_controls          NVARCHAR(MAX)  NULL,
    control_effectiveness      VARCHAR(16)    NOT NULL,
    residual_risk              VARCHAR(16)    NOT NULL,
    treatment_option           VARCHAR(32)    NULL,
    treatment_plan             NVARCHAR(MAX)  NULL,
    treatment_change_justification NVARCHAR(MAX) NULL,
    risk_owner_user_id         BIGINT         NULL,
    assigned_contributor_id    BIGINT         NULL,
    work_task_id               BIGINT         NULL,
    admin_notes                NVARCHAR(MAX)  NULL,
    strategic_objective        NVARCHAR(512)  NULL,
    financial_impact_aed       DECIMAL(14, 2) NULL,
    created_by_user_id         BIGINT         NOT NULL,
    assigned_at                DATETIME2      NULL,
    submitted_at               DATETIME2      NULL,
    closed_at                  DATETIME2      NULL,
    created_at                 DATETIME2      NOT NULL CONSTRAINT DF_risk_created DEFAULT SYSUTCDATETIME(),
    updated_at                 DATETIME2      NOT NULL CONSTRAINT DF_risk_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_risk_display_id UNIQUE (display_id),
    CONSTRAINT fk_risk_org FOREIGN KEY (org_unit_id) REFERENCES org_unit (id),
    CONSTRAINT fk_risk_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT fk_risk_owner FOREIGN KEY (risk_owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_risk_contributor FOREIGN KEY (assigned_contributor_id) REFERENCES app_user (id),
    CONSTRAINT fk_risk_task FOREIGN KEY (work_task_id) REFERENCES work_task (id),
    CONSTRAINT fk_risk_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_risk_type_status ON risk_record (risk_type, status);
CREATE INDEX idx_risk_org ON risk_record (org_unit_id);
CREATE INDEX idx_risk_contributor ON risk_record (assigned_contributor_id);

CREATE TABLE risk_id_seq (
    risk_type VARCHAR(16) NOT NULL,
    year_num  INT         NOT NULL,
    next_val  INT         NOT NULL,
    CONSTRAINT pk_risk_id_seq PRIMARY KEY (risk_type, year_num)
);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('RISK_ASSIGNED',
 '[Action Required] - Risk assigned - Risk',
 'Risk {{displayId}} ({{title}}) was assigned to you. Due: {{dueDate}}.',
 1, 1, 1, 1);
