-- Contributor v1: org-unit membership, My Tasks, Gap item assignment.

CREATE TABLE user_org_unit (
    user_id     BIGINT NOT NULL,
    org_unit_id BIGINT NOT NULL,
    CONSTRAINT pk_user_org_unit PRIMARY KEY (user_id, org_unit_id),
    CONSTRAINT fk_uou_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_uou_unit FOREIGN KEY (org_unit_id) REFERENCES org_unit (id) ON DELETE CASCADE
);

CREATE INDEX idx_uou_unit ON user_org_unit (org_unit_id);

CREATE TABLE work_task (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    source_module        VARCHAR(32)    NOT NULL,
    source_record_type   VARCHAR(64)    NOT NULL,
    source_record_id     BIGINT         NOT NULL,
    title                NVARCHAR(512)  NOT NULL,
    detail               NVARCHAR(1000) NULL,
    assigned_to_user_id  BIGINT         NOT NULL,
    assigned_by_user_id  BIGINT         NOT NULL,
    status               VARCHAR(32)    NOT NULL CONSTRAINT DF_wt_status DEFAULT 'PENDING',
    due_date             DATE           NULL,
    submitted_at         DATETIME2      NULL,
    created_at           DATETIME2      NOT NULL CONSTRAINT DF_wt_created DEFAULT SYSUTCDATETIME(),
    updated_at           DATETIME2      NOT NULL CONSTRAINT DF_wt_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_wt_assignee FOREIGN KEY (assigned_to_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_wt_assigner FOREIGN KEY (assigned_by_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_wt_source UNIQUE (source_module, source_record_type, source_record_id)
);

CREATE INDEX idx_wt_assignee_status ON work_task (assigned_to_user_id, status);
CREATE INDEX idx_wt_module ON work_task (source_module);

ALTER TABLE gap_item ADD assigned_to_user_id BIGINT NULL;
ALTER TABLE gap_item ADD work_task_id BIGINT NULL;
GO
ALTER TABLE gap_item ADD CONSTRAINT fk_gap_item_assignee FOREIGN KEY (assigned_to_user_id) REFERENCES app_user (id);
ALTER TABLE gap_item ADD CONSTRAINT fk_gap_item_task FOREIGN KEY (work_task_id) REFERENCES work_task (id);

CREATE INDEX idx_gap_item_assignee ON gap_item (assigned_to_user_id);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('GAP_CONTROL_ASSIGNED',
 '[Action Required] - Gap control assigned - Gap Assessment',
 'Control {{nodeCode}} ({{title}}) was assigned to you for Gap Assessment {{assessmentName}}. Due: {{dueDate}}.',
 1, 1, 1, 1);
