-- F2 management & audit deliverables: scheduled report delivery.

CREATE TABLE report_schedule (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    report_id           NVARCHAR(64)   NOT NULL,
    format              NVARCHAR(16)   NOT NULL,
    frequency           NVARCHAR(16)   NOT NULL,
    recipient_user_ids  NVARCHAR(255)  NOT NULL,
    soa_id              BIGINT,
    gap_id              BIGINT,
    audit_id            BIGINT,
    exercise_id         BIGINT,
    variant             NVARCHAR(16),
    next_run_at         DATETIME2      NOT NULL,
    last_run_at         DATETIME2,
    active              BIT            NOT NULL DEFAULT 1,
    created_by_user_id  BIGINT         NOT NULL,
    created_at          DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_rs_creator FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_report_schedule_next ON report_schedule (active, next_run_at);

CREATE TABLE report_delivery (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    schedule_id         BIGINT,
    report_id           NVARCHAR(64)   NOT NULL,
    filename            NVARCHAR(255)  NOT NULL,
    content_type        NVARCHAR(128)  NOT NULL,
    file_bytes          VARBINARY(MAX) NOT NULL,
    delivered_at        DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),
    recipient_user_id   BIGINT         NOT NULL,
    CONSTRAINT fk_rd_schedule FOREIGN KEY (schedule_id) REFERENCES report_schedule (id),
    CONSTRAINT fk_rd_recipient FOREIGN KEY (recipient_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_report_delivery_recipient ON report_delivery (recipient_user_id, delivered_at DESC);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'REPORT_SCHEDULED' AND version = 1)
INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES ('REPORT_SCHEDULED',
        N'Scheduled report ready — {{reportTitle}}',
        N'Your scheduled {{reportTitle}} ({{format}}) is ready. Open Reports to download from delivery history.',
        0, 1, 1, 1);
