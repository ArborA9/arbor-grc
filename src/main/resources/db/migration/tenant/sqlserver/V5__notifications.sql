-- Central notification engine (req §7). Templates + in-app inbox. Email via outbox (ACS later).

CREATE TABLE notification_template (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    trigger_type      VARCHAR(64)    NOT NULL,
    subject_template  NVARCHAR(500)  NOT NULL,
    body_template     NVARCHAR(2000) NOT NULL,
    channel_email     BIT            NOT NULL CONSTRAINT DF_nt_email DEFAULT 1,
    channel_in_app    BIT            NOT NULL CONSTRAINT DF_nt_inapp DEFAULT 1,
    version           INT            NOT NULL CONSTRAINT DF_nt_ver DEFAULT 1,
    is_active         BIT            NOT NULL CONSTRAINT DF_nt_active DEFAULT 1,
    created_at        DATETIME2      NOT NULL CONSTRAINT DF_nt_created DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uq_notification_template_active
    ON notification_template (trigger_type, version);

CREATE TABLE notification (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id        BIGINT         NOT NULL,
    trigger_type   VARCHAR(64)    NOT NULL,
    title          NVARCHAR(500)  NOT NULL,
    body           NVARCHAR(2000) NULL,
    record_type    VARCHAR(64)    NULL,
    record_id      VARCHAR(64)    NULL,
    link           NVARCHAR(500)  NULL,
    read_at        DATETIME2      NULL,
    created_at     DATETIME2      NOT NULL CONSTRAINT DF_notification_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_user_unread ON notification (user_id, read_at);

CREATE TABLE notification_email_outbox (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    trigger_type   VARCHAR(64)    NOT NULL,
    to_email       NVARCHAR(255)  NOT NULL,
    subject        NVARCHAR(500)  NOT NULL,
    body           NVARCHAR(2000) NOT NULL,
    status         VARCHAR(32)    NOT NULL CONSTRAINT DF_email_status DEFAULT 'PENDING',
    created_at     DATETIME2      NOT NULL CONSTRAINT DF_email_created DEFAULT SYSUTCDATETIME(),
    sent_at        DATETIME2      NULL
);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
VALUES
('USER_INVITED',
 N'[FYI] - You have been invited - Users',
 N'You have been invited to Arbor GRC as {{roles}}. Sign in with {{authType}} when ready.',
 1, 1, 1, 1),
('LOCAL_AUTH_USED',
 N'[Action Required] - Local login used - Security',
 N'Local (break-glass) authentication was used by {{email}} on an SSO-enabled workspace.',
 1, 1, 1, 1),
('SSO_NO_ROLE',
 N'[Action Required] - Unmatched SSO login - Security',
 N'SSO login blocked for {{email}}: no invited Arbor account. Invite the user or ignore if unexpected.',
 1, 1, 1, 1);
