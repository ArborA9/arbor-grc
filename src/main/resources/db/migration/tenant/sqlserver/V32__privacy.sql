-- Privacy module MVP — Data Inventory, ROPA, DSR + PDPL 30-day clock (req §18 / Journey 8) — SQL Server twin.

CREATE TABLE privacy_data_inventory (
    id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    display_id           NVARCHAR(32)  NOT NULL,
    data_type            NVARCHAR(255) NOT NULL,
    data_subjects        NVARCHAR(500) NULL,
    volume               NVARCHAR(255) NULL,
    format               NVARCHAR(255) NULL,
    storage_asset_id     BIGINT        NULL,
    storage_location     NVARCHAR(500) NULL,
    storage_country      NVARCHAR(255) NULL,
    retention_period     NVARCHAR(255) NULL,
    deletion_method      NVARCHAR(255) NULL,
    org_unit_id          BIGINT        NULL,
    status               NVARCHAR(32)  NOT NULL CONSTRAINT DF_privacy_inventory_status DEFAULT N'ACTIVE',
    notes                NVARCHAR(2000) NULL,
    created_by_user_id   BIGINT        NOT NULL,
    created_at           DATETIME2     NOT NULL CONSTRAINT DF_privacy_inventory_created DEFAULT SYSUTCDATETIME(),
    updated_at           DATETIME2     NOT NULL CONSTRAINT DF_privacy_inventory_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_privacy_inventory_display_id UNIQUE (display_id),
    CONSTRAINT fk_privacy_inventory_asset FOREIGN KEY (storage_asset_id) REFERENCES asset (id),
    CONSTRAINT fk_privacy_inventory_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_unit (id),
    CONSTRAINT fk_privacy_inventory_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_privacy_inventory_status ON privacy_data_inventory (status);
CREATE INDEX idx_privacy_inventory_org ON privacy_data_inventory (org_unit_id);

CREATE TABLE privacy_ropa (
    id                          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    display_id                  NVARCHAR(32)  NOT NULL,
    activity_name               NVARCHAR(255) NOT NULL,
    purpose                     NVARCHAR(2000) NULL,
    legal_basis                 NVARCHAR(64)  NOT NULL,
    data_categories             NVARCHAR(1000) NULL,
    data_subjects               NVARCHAR(500) NULL,
    recipients                  NVARCHAR(1000) NULL,
    third_country_transfers     BIT           NOT NULL CONSTRAINT DF_privacy_ropa_transfers DEFAULT 0,
    third_country_transfer_details NVARCHAR(2000) NULL,
    retention_period            NVARCHAR(255) NULL,
    security_measures           NVARCHAR(2000) NULL,
    designation                 NVARCHAR(64)  NOT NULL,
    org_unit_id                 BIGINT        NULL,
    status                      NVARCHAR(32)  NOT NULL CONSTRAINT DF_privacy_ropa_status DEFAULT N'DRAFT',
    review_notes                NVARCHAR(2000) NULL,
    reviewed_by_user_id         BIGINT        NULL,
    reviewed_at                 DATETIME2     NULL,
    created_by_user_id          BIGINT        NOT NULL,
    created_at                  DATETIME2     NOT NULL CONSTRAINT DF_privacy_ropa_created DEFAULT SYSUTCDATETIME(),
    updated_at                  DATETIME2     NOT NULL CONSTRAINT DF_privacy_ropa_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_privacy_ropa_display_id UNIQUE (display_id),
    CONSTRAINT fk_privacy_ropa_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_unit (id),
    CONSTRAINT fk_privacy_ropa_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_privacy_ropa_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_privacy_ropa_status ON privacy_ropa (status);
CREATE INDEX idx_privacy_ropa_org ON privacy_ropa (org_unit_id);

CREATE TABLE privacy_dsr (
    id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    display_id           NVARCHAR(32)  NOT NULL,
    request_type         NVARCHAR(32)  NOT NULL,
    subject_name         NVARCHAR(255) NOT NULL,
    subject_email        NVARCHAR(255) NULL,
    subject_reference    NVARCHAR(255) NULL,
    description          NVARCHAR(MAX) NULL,
    status               NVARCHAR(32)  NOT NULL CONSTRAINT DF_privacy_dsr_status DEFAULT N'RECEIVED',
    assigned_user_id     BIGINT        NULL,
    received_at          DATETIME2     NOT NULL,
    due_at               DATE          NOT NULL,
    closed_at            DATETIME2     NULL,
    close_outcome        NVARCHAR(32)  NULL,
    rejection_reason     NVARCHAR(2000) NULL,
    day20_notified_at    DATETIME2     NULL,
    day28_notified_at    DATETIME2     NULL,
    overdue_notified_at  DATETIME2     NULL,
    created_by_user_id   BIGINT        NOT NULL,
    created_at           DATETIME2     NOT NULL CONSTRAINT DF_privacy_dsr_created DEFAULT SYSUTCDATETIME(),
    updated_at           DATETIME2     NOT NULL CONSTRAINT DF_privacy_dsr_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_privacy_dsr_display_id UNIQUE (display_id),
    CONSTRAINT fk_privacy_dsr_assigned FOREIGN KEY (assigned_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_privacy_dsr_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_privacy_dsr_status ON privacy_dsr (status);
CREATE INDEX idx_privacy_dsr_due ON privacy_dsr (due_at);
CREATE INDEX idx_privacy_dsr_assigned ON privacy_dsr (assigned_user_id);

-- DSR PDPL clock notification templates (req §7 rows 90–93 / §18.2).
IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DSR_RECEIVED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DSR_RECEIVED', N'New DSR received: {{displayId}}',
            N'A data subject request ({{requestType}}) for {{subjectName}} has been logged as {{displayId}}. Response is due by {{dueAt}}.',
            1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DSR_DAY_20')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DSR_DAY_20', N'DSR day 20 warning: {{displayId}}',
            N'DSR {{displayId}} ({{requestType}} for {{subjectName}}) has 10 days remaining. Due date: {{dueAt}}.',
            1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DSR_DAY_28')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DSR_DAY_28', N'DSR day 28 urgent: {{displayId}}',
            N'URGENT: DSR {{displayId}} ({{requestType}} for {{subjectName}}) has 2 days remaining. Due date: {{dueAt}}.',
            1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'DSR_OVERDUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'DSR_OVERDUE', N'DSR OVERDUE — PDPL breach risk: {{displayId}}',
            N'CRITICAL: DSR {{displayId}} ({{requestType}} for {{subjectName}}) is past the 30-day UAE PDPL deadline (due {{dueAt}}). Immediate action required.',
            1, 1, 1, 1);
END;
