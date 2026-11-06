-- Full 4T treatment paths (Tolerate / Transfer / Treat / Terminate).

ALTER TABLE risk_record ADD elimination_justification NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD elimination_how NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD activity_stopped BIT NULL;
ALTER TABLE risk_record ADD transfer_method VARCHAR(32) NULL;
ALTER TABLE risk_record ADD third_party_name NVARCHAR(512) NULL;
ALTER TABLE risk_record ADD transfer_description NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD residual_after_transfer NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD transfer_compensating_controls NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD validation_notes NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD rejection_reason NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD validated_at DATETIME2 NULL;
ALTER TABLE risk_record ADD validated_by_user_id BIGINT NULL;
ALTER TABLE risk_record ADD next_review_date DATE NULL;
GO

ALTER TABLE risk_record ADD CONSTRAINT fk_risk_validated_by
    FOREIGN KEY (validated_by_user_id) REFERENCES app_user (id);

CREATE TABLE treatment_plan (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    risk_id               BIGINT         NOT NULL,
    action_description    NVARCHAR(MAX)  NULL,
    action_owner_id       BIGINT         NULL,
    start_date            DATE           NULL,
    target_date           DATE           NULL,
    resources_required    NVARCHAR(MAX)  NULL,
    cost_estimate_aed     DECIMAL(14, 2) NULL,
    evidence_required     NVARCHAR(MAX)  NULL,
    status                VARCHAR(32)    NOT NULL CONSTRAINT DF_tp_status DEFAULT 'NOT_STARTED',
    completion_date       DATE           NULL,
    created_at            DATETIME2      NOT NULL CONSTRAINT DF_tp_created DEFAULT SYSUTCDATETIME(),
    updated_at            DATETIME2      NOT NULL CONSTRAINT DF_tp_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_treatment_plan_risk UNIQUE (risk_id),
    CONSTRAINT fk_tp_risk FOREIGN KEY (risk_id) REFERENCES risk_record (id),
    CONSTRAINT fk_tp_owner FOREIGN KEY (action_owner_id) REFERENCES app_user (id)
);

CREATE TABLE treatment_milestone (
    id                    BIGINT IDENTITY(1,1) PRIMARY KEY,
    treatment_plan_id     BIGINT         NOT NULL,
    milestone_title       NVARCHAR(512)  NOT NULL,
    due_date              DATE           NULL,
    completed             BIT            NOT NULL CONSTRAINT DF_tm_completed DEFAULT 0,
    completed_date        DATE           NULL,
    completed_by_user_id  BIGINT         NULL,
    created_at            DATETIME2      NOT NULL CONSTRAINT DF_tm_created DEFAULT SYSUTCDATETIME(),
    updated_at            DATETIME2      NOT NULL CONSTRAINT DF_tm_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_tm_plan FOREIGN KEY (treatment_plan_id) REFERENCES treatment_plan (id),
    CONSTRAINT fk_tm_completed_by FOREIGN KEY (completed_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_tm_plan ON treatment_milestone (treatment_plan_id);

CREATE TABLE risk_acceptance_form (
    id                          BIGINT IDENTITY(1,1) PRIMARY KEY,
    risk_id                     BIGINT         NOT NULL,
    justification               NVARCHAR(MAX)  NULL,
    business_rationale          NVARCHAR(MAX)  NULL,
    conditions_of_acceptance    NVARCHAR(MAX)  NULL,
    compensating_controls       NVARCHAR(MAX)  NULL,
    comp_control_owner_id       BIGINT         NULL,
    comp_control_effectiveness  VARCHAR(16)    NULL,
    review_date                 DATE           NULL,
    contributor_declaration     BIT            NOT NULL CONSTRAINT DF_raf_decl DEFAULT 0,
    risk_owner_decision         VARCHAR(32)    NULL,
    risk_owner_comments         NVARCHAR(MAX)  NULL,
    risk_owner_reviewed_at      DATETIME2      NULL,
    admin_decision              VARCHAR(32)    NULL,
    admin_comments              NVARCHAR(MAX)  NULL,
    admin_reviewed_at           DATETIME2      NULL,
    status                      VARCHAR(32)    NOT NULL CONSTRAINT DF_raf_status DEFAULT 'DRAFT',
    created_at                  DATETIME2      NOT NULL CONSTRAINT DF_raf_created DEFAULT SYSUTCDATETIME(),
    updated_at                  DATETIME2      NOT NULL CONSTRAINT DF_raf_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_acceptance_risk UNIQUE (risk_id),
    CONSTRAINT fk_raf_risk FOREIGN KEY (risk_id) REFERENCES risk_record (id),
    CONSTRAINT fk_raf_comp_owner FOREIGN KEY (comp_control_owner_id) REFERENCES app_user (id)
);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('RISK_SUBMITTED',
 '[Info] - Risk submitted - Risk',
 'Risk {{displayId}} ({{title}}) was submitted for validation.',
 1, 1, 1, 1),
('RISK_VALIDATION_REJECTED',
 '[Action Required] - Risk validation rejected - Risk',
 'Risk {{displayId}} was sent back. Reason: {{reason}}.',
 1, 1, 1, 1),
('RISK_PENDING_OWNER',
 '[Action Required] - Risk acceptance awaiting Risk Owner - Risk',
 'Risk {{displayId}} ({{title}}) awaits your acceptance decision.',
 1, 1, 1, 1),
('RISK_PENDING_ADMIN',
 '[Action Required] - Risk acceptance awaiting Admin - Risk',
 'Risk {{displayId}} ({{title}}) Risk Owner approved; awaiting Admin final acceptance.',
 1, 1, 1, 1),
('RISK_TOLERATED',
 '[Info] - Risk tolerated - Risk',
 'Risk {{displayId}} ({{title}}) was formally tolerated. Next review: {{reviewDate}}.',
 1, 1, 1, 1),
('RISK_MITIGATING',
 '[Info] - Risk mitigating - Risk',
 'Risk {{displayId}} ({{title}}) treatment plan approved; now mitigating.',
 1, 1, 1, 1);
