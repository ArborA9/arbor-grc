-- BCMS: department profiles, BIA, recovery plans, exercises, RTO thresholds, notification templates.

ALTER TABLE org_profile ADD rto_max_critical_hours INT NOT NULL
    CONSTRAINT DF_org_profile_rto_critical DEFAULT 4;
ALTER TABLE org_profile ADD rto_max_high_hours INT NOT NULL
    CONSTRAINT DF_org_profile_rto_high DEFAULT 24;
ALTER TABLE org_profile ADD rto_max_medium_hours INT NOT NULL
    CONSTRAINT DF_org_profile_rto_medium DEFAULT 72;
ALTER TABLE org_profile ADD rto_max_low_hours INT NOT NULL
    CONSTRAINT DF_org_profile_rto_low DEFAULT 168;

CREATE TABLE bcms_dept_profile (
    id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    org_unit_id             BIGINT        NOT NULL,
    total_staff             INT           NULL,
    critical_staff_roles    NVARCHAR(MAX) NULL,
    primary_functions       NVARCHAR(MAX) NULL,
    internal_dependencies   NVARCHAR(MAX) NULL,
    external_dependencies   NVARCHAR(MAX) NULL,
    physical_location       NVARCHAR(512) NULL,
    alternate_work_location NVARCHAR(512) NULL,
    created_at              DATETIME2     NOT NULL CONSTRAINT DF_bdp_created DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2     NOT NULL CONSTRAINT DF_bdp_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_bcms_dept_org UNIQUE (org_unit_id),
    CONSTRAINT fk_bcms_dept_org FOREIGN KEY (org_unit_id) REFERENCES org_unit (id)
);

CREATE TABLE bcms_dept_profile_asset (
    profile_id BIGINT NOT NULL,
    asset_id   BIGINT NOT NULL,
    CONSTRAINT pk_bcms_dpa PRIMARY KEY (profile_id, asset_id),
    CONSTRAINT fk_bcms_dpa_profile FOREIGN KEY (profile_id) REFERENCES bcms_dept_profile (id) ON DELETE CASCADE,
    CONSTRAINT fk_bcms_dpa_asset FOREIGN KEY (asset_id) REFERENCES asset (id)
);

CREATE TABLE bia (
    id                            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    project_id                    BIGINT         NULL,
    org_unit_id                   BIGINT         NOT NULL,
    process_name                  NVARCHAR(255)  NOT NULL,
    description                   NVARCHAR(MAX)  NULL,
    owner_user_id                 BIGINT         NULL,
    criticality                   NVARCHAR(32)   NOT NULL,
    category                      NVARCHAR(32)   NOT NULL,
    rto_hours                     INT            NULL,
    rpo_hours                     INT            NULL,
    mtpd_hours                    INT            NULL,
    mbco                          NVARCHAR(MAX)  NULL,
    financial_impact_per_hour     DECIMAL(18, 2) NULL,
    reputational_impact           NVARCHAR(32)   NULL,
    regulatory_impact             NVARCHAR(32)   NULL,
    operational_impact            NVARCHAR(32)   NULL,
    it_systems_notes              NVARCHAR(MAX)  NULL,
    internal_dependencies         NVARCHAR(MAX)  NULL,
    external_dependencies         NVARCHAR(MAX)  NULL,
    min_staff_required            INT            NULL,
    manual_workaround_available   BIT            NOT NULL CONSTRAINT DF_bia_workaround DEFAULT 0,
    manual_workaround_description NVARCHAR(MAX)  NULL,
    rto_justification             NVARCHAR(MAX)  NULL,
    status                        NVARCHAR(32)   NOT NULL CONSTRAINT DF_bia_status DEFAULT 'DRAFT',
    assigned_to_user_id           BIGINT         NULL,
    due_date                      DATE           NULL,
    work_task_id                  BIGINT         NULL,
    return_reason                 NVARCHAR(MAX)  NULL,
    review_date                   DATE           NULL,
    approved_at                   DATETIME2      NULL,
    approved_by_user_id           BIGINT         NULL,
    created_by_user_id            BIGINT         NOT NULL,
    created_at                    DATETIME2      NOT NULL CONSTRAINT DF_bia_created DEFAULT SYSUTCDATETIME(),
    updated_at                    DATETIME2      NOT NULL CONSTRAINT DF_bia_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_bia_project FOREIGN KEY (project_id) REFERENCES compliance_project (id),
    CONSTRAINT fk_bia_org FOREIGN KEY (org_unit_id) REFERENCES org_unit (id)
);

CREATE INDEX idx_bia_status ON bia (status);
CREATE INDEX idx_bia_org ON bia (org_unit_id);
CREATE INDEX idx_bia_project ON bia (project_id);
CREATE INDEX idx_bia_assigned ON bia (assigned_to_user_id);

CREATE TABLE recovery_plan (
    id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    project_id              BIGINT        NULL,
    name                    NVARCHAR(255) NOT NULL,
    plan_type               NVARCHAR(32)  NOT NULL,
    scope                   NVARCHAR(MAX) NULL,
    version_num             INT           NOT NULL CONSTRAINT DF_rp_version DEFAULT 1,
    status                  NVARCHAR(32)  NOT NULL CONSTRAINT DF_rp_status DEFAULT 'DRAFT',
    crisis_team             NVARCHAR(MAX) NULL,
    escalation_contacts     NVARCHAR(MAX) NULL,
    communication_tree      NVARCHAR(MAX) NULL,
    resource_requirements   NVARCHAR(MAX) NULL,
    alternate_site_details  NVARCHAR(MAX) NULL,
    owner_user_id           BIGINT        NULL,
    created_by_user_id      BIGINT        NOT NULL,
    submitted_at            DATETIME2     NULL,
    approved_at             DATETIME2     NULL,
    approved_by_user_id     BIGINT        NULL,
    review_date             DATE          NULL,
    created_at              DATETIME2     NOT NULL CONSTRAINT DF_rp_created DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2     NOT NULL CONSTRAINT DF_rp_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_rp_project FOREIGN KEY (project_id) REFERENCES compliance_project (id)
);

CREATE INDEX idx_rp_status ON recovery_plan (status);
CREATE INDEX idx_rp_type ON recovery_plan (plan_type);

CREATE TABLE recovery_plan_bia (
    plan_id BIGINT NOT NULL,
    bia_id  BIGINT NOT NULL,
    CONSTRAINT pk_rpb PRIMARY KEY (plan_id, bia_id),
    CONSTRAINT fk_rpb_plan FOREIGN KEY (plan_id) REFERENCES recovery_plan (id) ON DELETE CASCADE,
    CONSTRAINT fk_rpb_bia FOREIGN KEY (bia_id) REFERENCES bia (id)
);

CREATE TABLE recovery_plan_procedure (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    plan_id        BIGINT        NOT NULL,
    bia_id         BIGINT        NULL,
    process_name   NVARCHAR(255) NOT NULL,
    procedure_text NVARCHAR(MAX) NULL,
    sort_order     INT           NOT NULL CONSTRAINT DF_rpp_sort DEFAULT 0,
    CONSTRAINT fk_rpp_plan FOREIGN KEY (plan_id) REFERENCES recovery_plan (id) ON DELETE CASCADE,
    CONSTRAINT fk_rpp_bia FOREIGN KEY (bia_id) REFERENCES bia (id)
);

CREATE INDEX idx_rpp_plan ON recovery_plan_procedure (plan_id);

CREATE TABLE bcms_exercise (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    project_id          BIGINT        NULL,
    name                NVARCHAR(255) NOT NULL,
    exercise_type       NVARCHAR(32)  NOT NULL,
    scope               NVARCHAR(MAX) NULL,
    facilitator_user_id BIGINT        NULL,
    objectives          NVARCHAR(MAX) NULL,
    scenarios_tested    NVARCHAR(MAX) NULL,
    results             NVARCHAR(MAX) NULL,
    gaps_identified     NVARCHAR(MAX) NULL,
    lessons_learned     NVARCHAR(MAX) NULL,
    scheduled_date      DATE          NULL,
    status              NVARCHAR(32)  NOT NULL CONSTRAINT DF_ex_status DEFAULT 'PLANNED',
    completed_at        DATETIME2     NULL,
    created_by_user_id  BIGINT        NOT NULL,
    created_at          DATETIME2     NOT NULL CONSTRAINT DF_ex_created DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2     NOT NULL CONSTRAINT DF_ex_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_ex_project FOREIGN KEY (project_id) REFERENCES compliance_project (id)
);

CREATE INDEX idx_ex_status ON bcms_exercise (status);
CREATE INDEX idx_ex_scheduled ON bcms_exercise (scheduled_date);

CREATE TABLE bcms_exercise_plan (
    exercise_id BIGINT NOT NULL,
    plan_id     BIGINT NOT NULL,
    CONSTRAINT pk_exp PRIMARY KEY (exercise_id, plan_id),
    CONSTRAINT fk_exp_ex FOREIGN KEY (exercise_id) REFERENCES bcms_exercise (id) ON DELETE CASCADE,
    CONSTRAINT fk_exp_plan FOREIGN KEY (plan_id) REFERENCES recovery_plan (id)
);

CREATE TABLE bcms_exercise_participant (
    id               BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    exercise_id      BIGINT        NOT NULL,
    user_id          BIGINT        NULL,
    participant_name NVARCHAR(255) NULL,
    CONSTRAINT fk_exp_part_ex FOREIGN KEY (exercise_id) REFERENCES bcms_exercise (id) ON DELETE CASCADE,
    CONSTRAINT fk_exp_part_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_exp_part_ex ON bcms_exercise_participant (exercise_id);

-- Notification templates (idempotent)
IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_ASSIGNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_ASSIGNED', N'BIA assigned: {{processName}}', N'BIA “{{processName}}” was assigned to you. Due {{dueDate}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_NOT_SUBMITTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_NOT_SUBMITTED', N'BIA overdue: {{processName}}', N'BIA “{{processName}}” is past due ({{dueDate}}) and not submitted.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_SUBMITTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_SUBMITTED', N'BIA submitted: {{processName}}', N'Contributor submitted BIA “{{processName}}” for review.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_RETURNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_RETURNED', N'BIA returned: {{processName}}', N'BIA “{{processName}}” was returned: {{reason}}', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_APPROVED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_APPROVED', N'BIA approved: {{processName}}', N'BIA “{{processName}}” was approved.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BIA_REVIEW_DUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BIA_REVIEW_DUE', N'BIA review due: {{processName}}', N'BIA “{{processName}}” review date {{reviewDate}} is approaching.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BCP_APPROVAL_NEEDED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BCP_APPROVAL_NEEDED', N'Recovery plan needs approval: {{planName}}', N'Recovery plan “{{planName}}” awaits Owner approval.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BCP_APPROVED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BCP_APPROVED', N'Recovery plan approved: {{planName}}', N'Recovery plan “{{planName}}” (v{{version}}) was approved.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BCP_REVIEW_DUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BCP_REVIEW_DUE', N'Recovery plan review due: {{planName}}', N'Recovery plan “{{planName}}” review date {{reviewDate}} is approaching.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'BCP_EXPIRED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'BCP_EXPIRED', N'Recovery plan expired: {{planName}}', N'Recovery plan “{{planName}}” is past its review date.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'EXERCISE_DUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'EXERCISE_DUE', N'Exercise due: {{exerciseName}}', N'BCMS exercise “{{exerciseName}}” is scheduled for {{scheduledDate}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'EXERCISE_OVERDUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'EXERCISE_OVERDUE', N'Exercise overdue: {{exerciseName}}', N'BCMS exercise “{{exerciseName}}” is overdue ({{scheduledDate}}).', 1, 1, 1, 1);
END;
