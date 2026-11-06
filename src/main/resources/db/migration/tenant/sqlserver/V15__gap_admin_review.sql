-- Gap Admin review: separate final status + send-back; immutable Contributor submit snapshots.

ALTER TABLE gap_item ADD review_status VARCHAR(32) NOT NULL
    CONSTRAINT DF_gap_item_review_status DEFAULT 'OPEN';
ALTER TABLE gap_item ADD final_implementation_status VARCHAR(32) NULL;
ALTER TABLE gap_item ADD admin_review_notes NVARCHAR(MAX) NULL;
ALTER TABLE gap_item ADD send_back_reason NVARCHAR(MAX) NULL;
ALTER TABLE gap_item ADD contributor_submitted_at DATETIME2 NULL;
ALTER TABLE gap_item ADD admin_reviewed_at DATETIME2 NULL;
ALTER TABLE gap_item ADD admin_reviewed_by_user_id BIGINT NULL;
GO

ALTER TABLE gap_item ADD CONSTRAINT fk_gap_item_admin_reviewed_by
    FOREIGN KEY (admin_reviewed_by_user_id) REFERENCES app_user (id);

CREATE TABLE gap_item_submission (
    id                        BIGINT IDENTITY(1,1) PRIMARY KEY,
    gap_item_id               BIGINT         NOT NULL,
    submission_number         INT            NOT NULL,
    implementation_status     VARCHAR(32)    NOT NULL,
    observations              NVARCHAR(MAX)  NULL,
    gap_description           NVARCHAR(MAX)  NULL,
    score                     INT            NULL,
    submitted_by_user_id      BIGINT         NOT NULL,
    submitted_at              DATETIME2      NOT NULL,
    work_task_id              BIGINT         NULL,
    CONSTRAINT fk_gis_item FOREIGN KEY (gap_item_id) REFERENCES gap_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_gis_user FOREIGN KEY (submitted_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_gis_task FOREIGN KEY (work_task_id) REFERENCES work_task (id),
    CONSTRAINT uq_gis_item_num UNIQUE (gap_item_id, submission_number)
);

CREATE INDEX idx_gis_item ON gap_item_submission (gap_item_id);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('GAP_CONTROL_SENT_BACK',
 'Gap control sent back: {{nodeCode}}',
 'Admin sent back control {{nodeCode}} ({{title}}) in assessment "{{assessmentName}}". Reason: {{reason}}',
 1, 1, 1, 1),
('GAP_CONTROL_FINALIZED',
 'Gap control finalized: {{nodeCode}}',
 'Admin set final status {{finalStatus}} for control {{nodeCode}} ({{title}}) in assessment "{{assessmentName}}".',
 1, 1, 1, 1);
