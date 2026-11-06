-- Risk Wave 5: KRI — SQL Server twin.

CREATE TABLE kri_id_seq (
    year_val   INT NOT NULL,
    next_val   INT NOT NULL,
    PRIMARY KEY (year_val)
);

CREATE TABLE kri (
    id                       BIGINT IDENTITY(1,1) PRIMARY KEY,
    display_id               NVARCHAR(32)   NOT NULL,
    name                     NVARCHAR(512)  NOT NULL,
    linked_risk_id           BIGINT         NULL,
    metric_description       NVARCHAR(MAX)  NULL,
    unit                     NVARCHAR(64)   NULL,
    frequency                NVARCHAR(16)   NOT NULL,
    owner_user_id            BIGINT         NOT NULL,
    direction                NVARCHAR(32)   NOT NULL,
    green_threshold          DECIMAL(18,4)  NOT NULL,
    amber_threshold          DECIMAL(18,4)  NOT NULL,
    red_threshold            DECIMAL(18,4)  NOT NULL,
    current_value            DECIMAL(18,4)  NULL,
    previous_value           DECIMAL(18,4)  NULL,
    rag                      NVARCHAR(16)   NULL,
    trend                    NVARCHAR(32)   NULL,
    next_due_date            DATE           NULL,
    last_measured_at         DATETIME2      NULL,
    due_notified_at          DATETIME2      NULL,
    overdue_notified_at      DATETIME2      NULL,
    red_acknowledged_at      DATETIME2      NULL,
    red_acknowledged_by      BIGINT         NULL,
    status                   NVARCHAR(16)   NOT NULL CONSTRAINT df_kri_status DEFAULT N'ACTIVE',
    work_task_id             BIGINT         NULL,
    created_by_user_id       BIGINT         NOT NULL,
    created_at               DATETIME2      NOT NULL CONSTRAINT df_kri_created DEFAULT SYSUTCDATETIME(),
    updated_at               DATETIME2      NOT NULL CONSTRAINT df_kri_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_kri_display UNIQUE (display_id),
    CONSTRAINT fk_kri_risk FOREIGN KEY (linked_risk_id) REFERENCES risk_record (id),
    CONSTRAINT fk_kri_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_kri_created FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_kri_status ON kri (status);
CREATE INDEX idx_kri_owner ON kri (owner_user_id);
CREATE INDEX idx_kri_due ON kri (next_due_date);
CREATE INDEX idx_kri_rag ON kri (rag);

CREATE TABLE kri_reading (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    kri_id               BIGINT         NOT NULL,
    metric_value         DECIMAL(18,4)  NOT NULL,
    rag                  NVARCHAR(16)   NOT NULL,
    notes                NVARCHAR(MAX)  NULL,
    recorded_by_user_id  BIGINT         NOT NULL,
    recorded_at          DATETIME2      NOT NULL CONSTRAINT df_kri_reading_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_kri_reading_kri FOREIGN KEY (kri_id) REFERENCES kri (id) ON DELETE CASCADE,
    CONSTRAINT fk_kri_reading_user FOREIGN KEY (recorded_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_kri_reading_kri ON kri_reading (kri_id, recorded_at);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'KRI_VALUE_DUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'KRI_VALUE_DUE', N'[Action Required] - KRI value due - KRI',
            N'KRI {{displayId}} ({{name}}) is due for measurement on {{dueDate}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'KRI_VALUE_OVERDUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'KRI_VALUE_OVERDUE', N'[Action Required] - KRI value overdue - KRI',
            N'KRI {{displayId}} ({{name}}) is overdue for measurement (due {{dueDate}}).', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'KRI_AMBER_BREACH')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'KRI_AMBER_BREACH', N'[Warning] - KRI Amber breach - KRI',
            N'KRI {{displayId}} ({{name}}) entered Amber (value {{value}}, threshold {{threshold}}).', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'KRI_RED_BREACH')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'KRI_RED_BREACH', N'[Critical] - KRI Red breach - KRI',
            N'KRI {{displayId}} ({{name}}) entered Red (value {{value}}, threshold {{threshold}}). Acknowledge required.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'KRI_RED_ACKNOWLEDGED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'KRI_RED_ACKNOWLEDGED', N'[FYI] - KRI Red acknowledged - KRI',
            N'KRI {{displayId}} ({{name}}) Red breach was acknowledged by Admin.', 0, 1, 1, 1);
END;
