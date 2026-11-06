-- Policy repository (§17.3) — SQL Server twin.

CREATE TABLE policy (
    id                        BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    title                     NVARCHAR(255) NOT NULL,
    category                  NVARCHAR(32)  NOT NULL,
    status                    NVARCHAR(32)  NOT NULL CONSTRAINT DF_policy_status DEFAULT N'DRAFT',
    org_unit_id               BIGINT        NULL,
    major_version             INT           NOT NULL CONSTRAINT DF_policy_major DEFAULT 0,
    minor_version             INT           NOT NULL CONSTRAINT DF_policy_minor DEFAULT 0,
    review_date               DATE          NULL,
    ack_required              BIT           NOT NULL CONSTRAINT DF_policy_ack_req DEFAULT 1,
    ack_scope_type            NVARCHAR(32)  NOT NULL CONSTRAINT DF_policy_ack_scope DEFAULT N'ALL_ACTIVE_USERS',
    ack_deadline              DATE          NULL,
    reject_reason             NVARCHAR(MAX) NULL,
    review_notes              NVARCHAR(MAX) NULL,
    body_draft                NVARCHAR(MAX) NULL,
    storage_key_draft         NVARCHAR(512) NULL,
    original_filename_draft   NVARCHAR(255) NULL,
    content_type_draft        NVARCHAR(128) NULL,
    size_bytes_draft          BIGINT        NULL,
    owner_user_id             BIGINT        NULL,
    created_by_user_id        BIGINT        NOT NULL,
    current_version_id        BIGINT        NULL,
    review_due_notified_at    DATETIME2     NULL,
    expired_notified_at       DATETIME2     NULL,
    ack_deadline_notified_at  DATETIME2     NULL,
    ack_overdue_notified_at   DATETIME2     NULL,
    created_at                DATETIME2     NOT NULL CONSTRAINT DF_policy_created DEFAULT SYSUTCDATETIME(),
    updated_at                DATETIME2     NOT NULL CONSTRAINT DF_policy_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_policy_org FOREIGN KEY (org_unit_id) REFERENCES org_unit (id),
    CONSTRAINT fk_policy_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_policy_status ON policy (status);
CREATE INDEX idx_policy_category ON policy (category);
CREATE INDEX idx_policy_review ON policy (review_date);

CREATE TABLE policy_version (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    policy_id           BIGINT         NOT NULL,
    major_version       INT            NOT NULL,
    minor_version       INT            NOT NULL,
    body_text           NVARCHAR(MAX)  NULL,
    storage_key         NVARCHAR(512)  NULL,
    original_filename   NVARCHAR(255)  NULL,
    content_type        NVARCHAR(128)  NULL,
    size_bytes          BIGINT         NULL,
    approved_at         DATETIME2      NULL,
    approved_by_user_id BIGINT         NULL,
    created_by_user_id  BIGINT         NOT NULL,
    created_at          DATETIME2      NOT NULL CONSTRAINT DF_pv_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_pv_policy FOREIGN KEY (policy_id) REFERENCES policy (id) ON DELETE CASCADE,
    CONSTRAINT uq_pv_policy_ver UNIQUE (policy_id, major_version, minor_version)
);

CREATE INDEX idx_pv_policy ON policy_version (policy_id);

CREATE TABLE policy_framework (
    policy_id       BIGINT         NOT NULL,
    framework_id    BIGINT         NOT NULL,
    framework_code  NVARCHAR(64)   NOT NULL,
    framework_name  NVARCHAR(255)  NOT NULL,
    CONSTRAINT pk_policy_fw PRIMARY KEY (policy_id, framework_id),
    CONSTRAINT fk_pf_policy FOREIGN KEY (policy_id) REFERENCES policy (id) ON DELETE CASCADE
);

CREATE TABLE policy_ack_org_unit (
    policy_id   BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    CONSTRAINT pk_pao PRIMARY KEY (policy_id, org_unit_id),
    CONSTRAINT fk_pao_policy FOREIGN KEY (policy_id) REFERENCES policy (id) ON DELETE CASCADE,
    CONSTRAINT fk_pao_org FOREIGN KEY (org_unit_id) REFERENCES org_unit (id)
);

CREATE TABLE policy_ack_user (
    policy_id BIGINT NOT NULL,
    user_id   BIGINT NOT NULL,
    CONSTRAINT pk_pau PRIMARY KEY (policy_id, user_id),
    CONSTRAINT fk_pau_policy FOREIGN KEY (policy_id) REFERENCES policy (id) ON DELETE CASCADE,
    CONSTRAINT fk_pau_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE policy_acknowledgement (
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    policy_version_id BIGINT    NOT NULL,
    user_id           BIGINT    NOT NULL,
    acknowledged_at   DATETIME2 NOT NULL,
    CONSTRAINT fk_pack_version FOREIGN KEY (policy_version_id) REFERENCES policy_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_pack_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT uq_pack_version_user UNIQUE (policy_version_id, user_id)
);

CREATE INDEX idx_pack_version ON policy_acknowledgement (policy_version_id);

-- Allow one WorkTask per assignee for the same source (policy acknowledgements).
ALTER TABLE work_task DROP CONSTRAINT uq_wt_source;
ALTER TABLE work_task ADD CONSTRAINT uq_wt_source_assignee
    UNIQUE (source_module, source_record_type, source_record_id, assigned_to_user_id);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'POLICY_SUBMITTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'POLICY_SUBMITTED', N'Policy submitted for review: {{title}}', N'Policy “{{title}}” was submitted for review.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'POLICY_APPROVED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'POLICY_APPROVED', N'Policy approved: {{title}}', N'Policy “{{title}}” (v{{version}}) was approved. Acknowledgement may be required.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'POLICY_ACK_REQUIRED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'POLICY_ACK_REQUIRED', N'Acknowledge policy: {{title}}', N'Please read and acknowledge “{{title}}” (v{{version}}).', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'POLICY_ACK_DEADLINE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'POLICY_ACK_DEADLINE', N'Policy acknowledgement due soon: {{title}}', N'Acknowledgement for “{{title}}” is due by {{ackDeadline}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'POLICY_ACK_OVERDUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'POLICY_ACK_OVERDUE', N'Policy acknowledgements overdue: {{title}}', N'Some users have not acknowledged “{{title}}” (deadline {{ackDeadline}}).', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'POLICY_REVIEW_DUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'POLICY_REVIEW_DUE', N'Policy review due soon: {{title}}', N'Policy “{{title}}” review date {{reviewDate}} is approaching.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'POLICY_EXPIRED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'POLICY_EXPIRED', N'Policy due for review: {{title}}', N'Policy “{{title}}” is past its review date and marked Due for Review.', 1, 1, 1, 1);
END;
