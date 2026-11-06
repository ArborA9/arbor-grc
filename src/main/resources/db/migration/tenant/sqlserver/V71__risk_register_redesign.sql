-- Risk register redesign: projects, Excel scoring columns, org-locked matrix, 3-approver forms.
-- GO splits: SQL Server cannot UPDATE/INDEX columns added in the same batch.

CREATE TABLE risk_project (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    project_type        VARCHAR(16)  NOT NULL,
    name                NVARCHAR(255) NOT NULL,
    assessment_year     INT          NOT NULL,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    framework_id        BIGINT       NULL,
    created_by_user_id  BIGINT       NOT NULL,
    created_at          DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_risk_project_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_risk_project_type ON risk_project (project_type, status);
GO

ALTER TABLE org_profile ADD risk_matrix_grid_size INT NOT NULL CONSTRAINT df_org_matrix_size DEFAULT 5;
ALTER TABLE org_profile ADD risk_matrix_locked BIT NOT NULL CONSTRAINT df_org_matrix_locked DEFAULT 0;
ALTER TABLE org_profile ADD risk_appetite_rating VARCHAR(16) NOT NULL CONSTRAINT df_org_appetite DEFAULT 'LOW';
GO

UPDATE org_profile SET risk_matrix_locked = 1 WHERE wizard_completed_at IS NOT NULL;
GO

ALTER TABLE risk_record ADD risk_project_id BIGINT NULL;
ALTER TABLE risk_record ADD date_identified DATE NULL;
ALTER TABLE risk_record ADD cause NVARCHAR(128) NULL;
ALTER TABLE risk_record ADD inherent_score INT NULL;
ALTER TABLE risk_record ADD residual_score INT NULL;
ALTER TABLE risk_record ADD risk_reduction_pct DECIMAL(5, 2) NULL;
ALTER TABLE risk_record ADD within_appetite BIT NULL;
ALTER TABLE risk_record ADD evaluation_decision VARCHAR(32) NULL;
ALTER TABLE risk_record ADD acceptance_required BIT NOT NULL CONSTRAINT df_risk_acc_req DEFAULT 0;
ALTER TABLE risk_record ADD target_control_effectiveness VARCHAR(16) NULL;
ALTER TABLE risk_record ADD target_residual_score INT NULL;
ALTER TABLE risk_record ADD target_within_appetite BIT NULL;
ALTER TABLE risk_record ADD custodian_comments NVARCHAR(MAX) NULL;
ALTER TABLE risk_record ADD post_treatment_at DATETIME2 NULL;
ALTER TABLE risk_record ADD post_treatment_by_user_id BIGINT NULL;
GO

ALTER TABLE risk_record ADD CONSTRAINT fk_risk_project
    FOREIGN KEY (risk_project_id) REFERENCES risk_project (id);
ALTER TABLE risk_record ADD CONSTRAINT fk_risk_post_treatment_by
    FOREIGN KEY (post_treatment_by_user_id) REFERENCES app_user (id);

CREATE INDEX idx_risk_project_id ON risk_record (risk_project_id);
GO

ALTER TABLE risk_acceptance_form ADD form_kind VARCHAR(16) NOT NULL CONSTRAINT df_raf_kind DEFAULT 'ACCEPT';
ALTER TABLE risk_acceptance_form ADD display_id VARCHAR(32) NULL;
ALTER TABLE risk_acceptance_form ADD cycle_no INT NOT NULL CONSTRAINT df_raf_cycle DEFAULT 1;
ALTER TABLE risk_acceptance_form ADD approver1_user_id BIGINT NULL;
ALTER TABLE risk_acceptance_form ADD approver2_user_id BIGINT NULL;
ALTER TABLE risk_acceptance_form ADD approver3_user_id BIGINT NULL;
ALTER TABLE risk_acceptance_form ADD approver2_decision VARCHAR(32) NULL;
ALTER TABLE risk_acceptance_form ADD approver2_comments NVARCHAR(MAX) NULL;
ALTER TABLE risk_acceptance_form ADD approver2_reviewed_at DATETIME2 NULL;
ALTER TABLE risk_acceptance_form ADD approver3_decision VARCHAR(32) NULL;
ALTER TABLE risk_acceptance_form ADD approver3_comments NVARCHAR(MAX) NULL;
ALTER TABLE risk_acceptance_form ADD approver3_reviewed_at DATETIME2 NULL;
ALTER TABLE risk_acceptance_form ADD submitted_at DATETIME2 NULL;
ALTER TABLE risk_acceptance_form ADD submitted_by_user_id BIGINT NULL;
GO

ALTER TABLE risk_acceptance_form ADD CONSTRAINT fk_raf_a1 FOREIGN KEY (approver1_user_id) REFERENCES app_user (id);
ALTER TABLE risk_acceptance_form ADD CONSTRAINT fk_raf_a2 FOREIGN KEY (approver2_user_id) REFERENCES app_user (id);
ALTER TABLE risk_acceptance_form ADD CONSTRAINT fk_raf_a3 FOREIGN KEY (approver3_user_id) REFERENCES app_user (id);
ALTER TABLE risk_acceptance_form ADD CONSTRAINT fk_raf_submitted_by FOREIGN KEY (submitted_by_user_id) REFERENCES app_user (id);

CREATE UNIQUE INDEX uq_raf_display_id ON risk_acceptance_form (display_id) WHERE display_id IS NOT NULL;
