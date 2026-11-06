-- Risk Wave 6: Incident Management (§14.2) — SQL Server twin.

CREATE TABLE incident_id_seq (
    year_val   INT NOT NULL,
    next_val   INT NOT NULL,
    PRIMARY KEY (year_val)
);

CREATE TABLE incident_record (
    id                       BIGINT IDENTITY(1,1) PRIMARY KEY,
    display_id               NVARCHAR(32)   NOT NULL,
    title                    NVARCHAR(512)  NOT NULL,
    incident_type            NVARCHAR(32)   NOT NULL,
    description              NVARCHAR(MAX)  NULL,
    occurred_at              DATETIME2      NOT NULL,
    affected_area            NVARCHAR(512)  NULL,
    priority                 NVARCHAR(8)    NOT NULL,
    status                   NVARCHAR(24)   NOT NULL,
    linked_risk_id           BIGINT         NULL,
    org_unit_id              BIGINT         NULL,
    reporter_user_id         BIGINT         NOT NULL,
    owner_user_id            BIGINT         NULL,
    root_cause               NVARCHAR(MAX)  NULL,
    containment_actions      NVARCHAR(MAX)  NULL,
    resolution_notes         NVARCHAR(MAX)  NULL,
    response_due_at          DATETIME2      NOT NULL,
    resolution_due_at        DATETIME2      NULL,
    responded_at             DATETIME2      NULL,
    resolved_at              DATETIME2      NULL,
    resolved_by_user_id      BIGINT         NULL,
    closed_at                DATETIME2      NULL,
    closed_by_user_id        BIGINT         NULL,
    overdue_notified_at      DATETIME2      NULL,
    work_task_id             BIGINT         NULL,
    created_by_user_id       BIGINT         NOT NULL,
    created_at               DATETIME2      NOT NULL CONSTRAINT df_incident_created DEFAULT SYSUTCDATETIME(),
    updated_at               DATETIME2      NOT NULL CONSTRAINT df_incident_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_incident_display UNIQUE (display_id),
    CONSTRAINT fk_incident_risk FOREIGN KEY (linked_risk_id) REFERENCES risk_record (id),
    CONSTRAINT fk_incident_org FOREIGN KEY (org_unit_id) REFERENCES org_unit (id),
    CONSTRAINT fk_incident_reporter FOREIGN KEY (reporter_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_incident_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_incident_created FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_incident_status ON incident_record (status);
CREATE INDEX idx_incident_priority ON incident_record (priority);
CREATE INDEX idx_incident_owner ON incident_record (owner_user_id);
CREATE INDEX idx_incident_response_due ON incident_record (response_due_at);
CREATE INDEX idx_incident_reporter ON incident_record (reporter_user_id);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'INCIDENT_P1_REPORTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'INCIDENT_P1_REPORTED', N'[Critical] - P1 incident reported - Incident',
            N'P1 Critical incident {{displayId}} ({{title}}) was reported. Immediate response required (target {{responseDue}}).', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'INCIDENT_P2_REPORTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'INCIDENT_P2_REPORTED', N'[Action Required] - P2 incident reported - Incident',
            N'P2 High incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'INCIDENT_P3_REPORTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'INCIDENT_P3_REPORTED', N'[Action Required] - P3 incident reported - Incident',
            N'P3 Medium incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'INCIDENT_P4_REPORTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'INCIDENT_P4_REPORTED', N'[FYI] - P4 incident reported - Incident',
            N'P4 Low incident {{displayId}} ({{title}}) was reported. Response target {{responseDue}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'INCIDENT_ASSIGNED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'INCIDENT_ASSIGNED', N'[Action Required] - Incident assigned - Incident',
            N'Incident {{displayId}} ({{title}}) was assigned to you. Priority {{priority}}, response due {{responseDue}}.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'INCIDENT_OVERDUE')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'INCIDENT_OVERDUE', N'[Action Required] - Incident response overdue - Incident',
            N'Incident {{displayId}} ({{title}}) is past its response target ({{responseDue}}).', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'INCIDENT_RESOLVED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'INCIDENT_RESOLVED', N'[FYI] - Incident resolved - Incident',
            N'Incident {{displayId}} ({{title}}) was marked Resolved.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'INCIDENT_CLOSED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'INCIDENT_CLOSED', N'[FYI] - Incident closed - Incident',
            N'Incident {{displayId}} ({{title}}) was closed by Admin.', 1, 1, 1, 1);
END;
