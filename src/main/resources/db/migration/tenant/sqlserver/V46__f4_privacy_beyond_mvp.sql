-- F4 Privacy beyond MVP — DPIA, consent register, 72-hour breach clock.

IF COL_LENGTH('privacy_ropa', 'dpia_required') IS NULL
    ALTER TABLE privacy_ropa ADD dpia_required BIT NOT NULL CONSTRAINT DF_privacy_ropa_dpia_required DEFAULT 0;

IF OBJECT_ID('privacy_dpia', 'U') IS NULL
BEGIN
    CREATE TABLE privacy_dpia (
        id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
        display_id              NVARCHAR(32)  NOT NULL,
        title                   NVARCHAR(255) NOT NULL,
        ropa_id                 BIGINT,
        processing_description  NVARCHAR(MAX),
        necessity_assessment    NVARCHAR(MAX),
        risks_identified        NVARCHAR(MAX),
        mitigation_measures     NVARCHAR(MAX),
        residual_risk           NVARCHAR(32),
        status                  NVARCHAR(32)  NOT NULL CONSTRAINT DF_privacy_dpia_status DEFAULT 'DRAFT',
        review_notes            NVARCHAR(2000),
        reviewed_by_user_id     BIGINT,
        reviewed_at             DATETIME2,
        created_by_user_id      BIGINT        NOT NULL,
        created_at              DATETIME2     NOT NULL CONSTRAINT DF_privacy_dpia_created DEFAULT SYSUTCDATETIME(),
        updated_at              DATETIME2     NOT NULL CONSTRAINT DF_privacy_dpia_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uq_privacy_dpia_display_id UNIQUE (display_id),
        CONSTRAINT fk_privacy_dpia_ropa FOREIGN KEY (ropa_id) REFERENCES privacy_ropa (id),
        CONSTRAINT fk_privacy_dpia_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES app_user (id),
        CONSTRAINT fk_privacy_dpia_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
    );
    CREATE INDEX idx_privacy_dpia_status ON privacy_dpia (status);
    CREATE INDEX idx_privacy_dpia_ropa ON privacy_dpia (ropa_id);
END;

IF OBJECT_ID('privacy_consent', 'U') IS NULL
BEGIN
    CREATE TABLE privacy_consent (
        id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
        display_id           NVARCHAR(32)  NOT NULL,
        subject_name         NVARCHAR(255) NOT NULL,
        subject_email        NVARCHAR(255),
        subject_reference    NVARCHAR(255),
        purpose              NVARCHAR(1000) NOT NULL,
        ropa_id              BIGINT,
        consent_method       NVARCHAR(64)  NOT NULL,
        consented_at         DATETIME2     NOT NULL,
        withdrawn_at         DATETIME2,
        status               NVARCHAR(32)  NOT NULL CONSTRAINT DF_privacy_consent_status DEFAULT 'ACTIVE',
        evidence_notes       NVARCHAR(2000),
        created_by_user_id   BIGINT        NOT NULL,
        created_at           DATETIME2     NOT NULL CONSTRAINT DF_privacy_consent_created DEFAULT SYSUTCDATETIME(),
        updated_at           DATETIME2     NOT NULL CONSTRAINT DF_privacy_consent_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uq_privacy_consent_display_id UNIQUE (display_id),
        CONSTRAINT fk_privacy_consent_ropa FOREIGN KEY (ropa_id) REFERENCES privacy_ropa (id),
        CONSTRAINT fk_privacy_consent_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
    );
    CREATE INDEX idx_privacy_consent_status ON privacy_consent (status);
END;

IF OBJECT_ID('privacy_breach', 'U') IS NULL
BEGIN
    CREATE TABLE privacy_breach (
        id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
        display_id              NVARCHAR(32)  NOT NULL,
        title                   NVARCHAR(255) NOT NULL,
        description             NVARCHAR(MAX),
        severity                NVARCHAR(32)  NOT NULL CONSTRAINT DF_privacy_breach_severity DEFAULT 'MEDIUM',
        discovered_at           DATETIME2     NOT NULL,
        report_deadline_at      DATETIME2     NOT NULL,
        reported_at             DATETIME2,
        authority_reference     NVARCHAR(255),
        affected_subjects_estimate NVARCHAR(255),
        status                  NVARCHAR(32)  NOT NULL CONSTRAINT DF_privacy_breach_status DEFAULT 'OPEN',
        hour48_notified_at      DATETIME2,
        overdue_notified_at     DATETIME2,
        closed_at               DATETIME2,
        created_by_user_id      BIGINT        NOT NULL,
        created_at              DATETIME2     NOT NULL CONSTRAINT DF_privacy_breach_created DEFAULT SYSUTCDATETIME(),
        updated_at              DATETIME2     NOT NULL CONSTRAINT DF_privacy_breach_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uq_privacy_breach_display_id UNIQUE (display_id),
        CONSTRAINT fk_privacy_breach_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
    );
    CREATE INDEX idx_privacy_breach_status ON privacy_breach (status);
    CREATE INDEX idx_privacy_breach_deadline ON privacy_breach (report_deadline_at);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'DPIA_REQUIRED' AND version = 1)
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES ('DPIA_REQUIRED', 'DPIA required: {{activityName}}',
            'ROPA activity "{{activityName}}" ({{displayId}}) requires a Data Protection Impact Assessment before approval.',
            1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'BREACH_RECORDED' AND version = 1)
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES ('BREACH_RECORDED', 'Personal data breach logged: {{displayId}}',
            'Breach {{displayId}} — {{title}} was logged. 72-hour authority notification deadline: {{reportDeadlineAt}}.',
            1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'BREACH_48H_WARNING' AND version = 1)
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES ('BREACH_48H_WARNING', 'Breach 48h warning: {{displayId}}',
            'Breach {{displayId}} ({{title}}) — 24 hours remain before the 72-hour PDPL notification deadline ({{reportDeadlineAt}}).',
            1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'BREACH_72H_OVERDUE' AND version = 1)
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES ('BREACH_72H_OVERDUE', 'Breach OVERDUE — 72h clock: {{displayId}}',
            'CRITICAL: Breach {{displayId}} ({{title}}) has passed the 72-hour notification deadline ({{reportDeadlineAt}}) without authority reporting.',
            1, 1, 1, 1);
