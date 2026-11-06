-- TPRM rebuild from client pack. Replaces V31/V43 TPRM tables.

IF OBJECT_ID('fk_tprm_answer_evidence', 'F') IS NOT NULL
    ALTER TABLE tprm_assessment_answer DROP CONSTRAINT fk_tprm_answer_evidence;
IF OBJECT_ID('fk_risk_tprm_vendor', 'F') IS NOT NULL
    ALTER TABLE risk_record DROP CONSTRAINT fk_risk_tprm_vendor;
UPDATE risk_record SET tprm_vendor_id = NULL;

IF OBJECT_ID('tprm_vendor_token', 'U') IS NOT NULL DROP TABLE tprm_vendor_token;
IF OBJECT_ID('tprm_finding', 'U') IS NOT NULL DROP TABLE tprm_finding;
IF OBJECT_ID('tprm_assessment_item', 'U') IS NOT NULL DROP TABLE tprm_assessment_item;
IF OBJECT_ID('tprm_assessment_answer', 'U') IS NOT NULL DROP TABLE tprm_assessment_answer;
IF OBJECT_ID('tprm_assessment', 'U') IS NOT NULL DROP TABLE tprm_assessment;
IF OBJECT_ID('tprm_questionnaire_question', 'U') IS NOT NULL DROP TABLE tprm_questionnaire_question;
IF OBJECT_ID('tprm_questionnaire_template', 'U') IS NOT NULL DROP TABLE tprm_questionnaire_template;
IF OBJECT_ID('tprm_vendor_contact', 'U') IS NOT NULL DROP TABLE tprm_vendor_contact;
IF OBJECT_ID('tprm_vendor_cert', 'U') IS NOT NULL DROP TABLE tprm_vendor_cert;
IF OBJECT_ID('tprm_vendor', 'U') IS NOT NULL DROP TABLE tprm_vendor;

CREATE TABLE tprm_control (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    control_code        NVARCHAR(16)   NOT NULL,
    domain              NVARCHAR(80)   NOT NULL,
    requirement         NVARCHAR(500)  NOT NULL,
    description         NVARCHAR(4000) NOT NULL,
    required_evidence   NVARCHAR(2000) NOT NULL,
    weight              INT            NOT NULL,
    weight_label        NVARCHAR(32)   NOT NULL,
    mandatory           BIT            NOT NULL CONSTRAINT DF_tprm_control_mand DEFAULT 0,
    asked_from_tier     INT            NOT NULL,
    iso27001            NVARCHAR(120)  NULL,
    uae_ia              NVARCHAR(120)  NULL,
    other_standard      NVARCHAR(120)  NULL,
    module              NVARCHAR(16)   NOT NULL,
    sort_order          INT            NOT NULL,
    CONSTRAINT uq_tprm_control_code UNIQUE (control_code)
);

CREATE TABLE tprm_vendor (
    id                         BIGINT IDENTITY(1,1) PRIMARY KEY,
    name                       NVARCHAR(200) NOT NULL,
    vendor_type                NVARCHAR(64)  NULL,
    website                    NVARCHAR(500) NULL,
    services_in_scope          NVARCHAR(1000) NULL,
    project_description        NVARCHAR(2000) NULL,
    org_owner                  NVARCHAR(200) NULL,
    contract_code              NVARCHAR(60)  NULL,
    contract_start             DATE          NULL,
    contract_end               DATE          NULL,
    f1                         NVARCHAR(200) NULL,
    f2                         NVARCHAR(200) NULL,
    f3                         NVARCHAR(200) NULL,
    f4                         NVARCHAR(200) NULL,
    f5                         NVARCHAR(200) NULL,
    f6                         NVARCHAR(200) NULL,
    f1_score                   INT           NULL,
    f2_score                   INT           NULL,
    f3_score                   INT           NULL,
    f4_score                   INT           NULL,
    f5_score                   INT           NULL,
    f6_score                   INT           NULL,
    inherent_score             INT           NULL,
    assurance_credit           INT           NULL,
    adjusted_score             INT           NULL,
    tier                       NVARCHAR(16)  NULL,
    criticality                NVARCHAR(32)  NULL,
    posture_target             DECIMAL(4, 2) NULL,
    reassessment_months        INT           NULL,
    override_tier              NVARCHAR(16)  NULL,
    override_justification     NVARCHAR(2000) NULL,
    elevation_flag             BIT           NOT NULL CONSTRAINT DF_tprm_vendor_elev DEFAULT 0,
    lifecycle_state            NVARCHAR(32)  NOT NULL CONSTRAINT DF_tprm_vendor_state DEFAULT N'PROFILE_INCOMPLETE',
    compliance_status          NVARCHAR(32)  NOT NULL CONSTRAINT DF_tprm_vendor_comp DEFAULT N'UNKNOWN',
    posture_percent            DECIMAL(6, 3) NULL,
    next_review_date           DATE          NULL,
    review_due_notified_at     DATETIME2     NULL,
    review_overdue_notified_at DATETIME2     NULL,
    contract_expiry_notified_at DATETIME2    NULL,
    linked_erm_risk_id         BIGINT        NULL,
    current_assessment_id      BIGINT        NULL,
    status                     NVARCHAR(32)  NOT NULL CONSTRAINT DF_tprm_vendor_status DEFAULT N'ACTIVE',
    created_by_user_id         BIGINT        NOT NULL,
    created_at                 DATETIME2     NOT NULL CONSTRAINT DF_tprm_vendor_created DEFAULT SYSUTCDATETIME(),
    updated_at                 DATETIME2     NOT NULL CONSTRAINT DF_tprm_vendor_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_tprm_vendor_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_tprm_vendor_erm_risk FOREIGN KEY (linked_erm_risk_id) REFERENCES risk_record (id)
);

CREATE INDEX idx_tprm_vendor_name ON tprm_vendor (name);
CREATE INDEX idx_tprm_vendor_state ON tprm_vendor (lifecycle_state);

CREATE TABLE tprm_vendor_contact (
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    vendor_id  BIGINT        NOT NULL,
    role       NVARCHAR(32)  NOT NULL,
    name       NVARCHAR(120) NULL,
    email      NVARCHAR(255) NOT NULL,
    mobile     NVARCHAR(40)  NULL,
    CONSTRAINT fk_tprm_contact_vendor FOREIGN KEY (vendor_id) REFERENCES tprm_vendor (id) ON DELETE CASCADE
);

CREATE INDEX idx_tprm_contact_vendor ON tprm_vendor_contact (vendor_id);

CREATE TABLE tprm_vendor_cert (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    vendor_id   BIGINT       NOT NULL,
    cert_type   NVARCHAR(64) NOT NULL,
    expiry_date DATE         NULL,
    CONSTRAINT fk_tprm_cert_vendor FOREIGN KEY (vendor_id) REFERENCES tprm_vendor (id) ON DELETE CASCADE
);

CREATE TABLE tprm_assessment (
    id                     BIGINT IDENTITY(1,1) PRIMARY KEY,
    vendor_id              BIGINT        NOT NULL,
    display_id             NVARCHAR(32)  NOT NULL,
    title                  NVARCHAR(255) NULL,
    status                 NVARCHAR(32)  NOT NULL CONSTRAINT DF_tprm_assessment_status DEFAULT N'DRAFT',
    snapshot_vendor_type   NVARCHAR(64)  NULL,
    snapshot_tier          NVARCHAR(16)  NULL,
    snapshot_criticality   NVARCHAR(32)  NULL,
    snapshot_target        DECIMAL(4, 2) NULL,
    inherent_score         INT           NULL,
    assurance_credit       INT           NULL,
    adjusted_score         INT           NULL,
    posture_percent        DECIMAL(6, 3) NULL,
    compliant              BIT           NULL,
    due_date               DATE          NULL,
    expires_at             DATETIME2     NULL,
    sent_at                DATETIME2     NULL,
    submitted_at           DATETIME2     NULL,
    reviewed_at            DATETIME2     NULL,
    raise_erm_prompted     BIT           NOT NULL CONSTRAINT DF_tprm_assessment_raise DEFAULT 0,
    linked_erm_risk_id     BIGINT        NULL,
    created_by_user_id     BIGINT        NOT NULL,
    reviewed_by_user_id    BIGINT        NULL,
    created_at             DATETIME2     NOT NULL CONSTRAINT DF_tprm_assessment_created DEFAULT SYSUTCDATETIME(),
    updated_at             DATETIME2     NOT NULL CONSTRAINT DF_tprm_assessment_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_tprm_assessment_display_id UNIQUE (display_id),
    CONSTRAINT fk_tprm_assessment_vendor FOREIGN KEY (vendor_id) REFERENCES tprm_vendor (id),
    CONSTRAINT fk_tprm_assessment_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_tprm_assessment_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_tprm_assessment_erm_risk FOREIGN KEY (linked_erm_risk_id) REFERENCES risk_record (id)
);

CREATE INDEX idx_tprm_assessment_vendor ON tprm_assessment (vendor_id);
CREATE INDEX idx_tprm_assessment_status ON tprm_assessment (status);

CREATE TABLE tprm_assessment_item (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    assessment_id    BIGINT         NOT NULL,
    control_id       BIGINT         NOT NULL,
    sort_order       INT            NOT NULL CONSTRAINT DF_tprm_item_sort DEFAULT 0,
    vendor_answer    NVARCHAR(16)   NULL,
    justification    NVARCHAR(4000) NULL,
    evidence_id      BIGINT         NULL,
    evidence_ref     NVARCHAR(500)  NULL,
    review_verdict   NVARCHAR(40)   NULL,
    review_comment   NVARCHAR(2000) NULL,
    returned         BIT            NOT NULL CONSTRAINT DF_tprm_item_returned DEFAULT 0,
    CONSTRAINT fk_tprm_item_assessment FOREIGN KEY (assessment_id) REFERENCES tprm_assessment (id) ON DELETE CASCADE,
    CONSTRAINT fk_tprm_item_control FOREIGN KEY (control_id) REFERENCES tprm_control (id),
    CONSTRAINT fk_tprm_item_evidence FOREIGN KEY (evidence_id) REFERENCES evidence (id),
    CONSTRAINT uq_tprm_item_assessment_control UNIQUE (assessment_id, control_id)
);

CREATE TABLE tprm_finding (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    vendor_id       BIGINT        NOT NULL,
    assessment_id   BIGINT        NOT NULL,
    item_id         BIGINT        NULL,
    control_code    NVARCHAR(16)  NOT NULL,
    domain          NVARCHAR(80)  NOT NULL,
    title           NVARCHAR(500) NOT NULL,
    owner_user_id   BIGINT        NULL,
    due_date        DATE          NULL,
    status          NVARCHAR(16)  NOT NULL CONSTRAINT DF_tprm_finding_status DEFAULT N'OPEN',
    created_at      DATETIME2     NOT NULL CONSTRAINT DF_tprm_finding_created DEFAULT SYSUTCDATETIME(),
    closed_at       DATETIME2     NULL,
    CONSTRAINT fk_tprm_finding_vendor FOREIGN KEY (vendor_id) REFERENCES tprm_vendor (id),
    CONSTRAINT fk_tprm_finding_assessment FOREIGN KEY (assessment_id) REFERENCES tprm_assessment (id) ON DELETE CASCADE
);

CREATE INDEX idx_tprm_finding_vendor ON tprm_finding (vendor_id);

CREATE TABLE tprm_vendor_token (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    assessment_id      BIGINT        NOT NULL,
    invitee_email      NVARCHAR(255) NOT NULL,
    token_hash         NVARCHAR(128) NOT NULL,
    otp_hash           NVARCHAR(128) NOT NULL,
    status             NVARCHAR(32)  NOT NULL CONSTRAINT DF_tprm_token_status DEFAULT N'PENDING',
    expires_at         DATETIME2     NOT NULL,
    redeemed_at        DATETIME2     NULL,
    consumed_at        DATETIME2     NULL,
    user_id            BIGINT        NULL,
    created_by_user_id BIGINT        NOT NULL,
    created_at         DATETIME2     NOT NULL CONSTRAINT DF_tprm_token_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_tprm_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_tprm_token_assessment FOREIGN KEY (assessment_id) REFERENCES tprm_assessment (id) ON DELETE CASCADE,
    CONSTRAINT fk_tprm_token_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_tprm_token_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_tprm_token_hash ON tprm_vendor_token (token_hash);

ALTER TABLE risk_record ADD CONSTRAINT fk_risk_tprm_vendor FOREIGN KEY (tprm_vendor_id) REFERENCES tprm_vendor (id);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_RETURNED')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_QUESTIONNAIRE_RETURNED', N'Questionnaire returned: {{displayId}}',
            N'Some controls on “{{assessmentTitle}}” were returned for {{vendorName}}. Please reopen the secure link and update the returned answers.',
            1, 0, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_REVIEW_COMPLETE')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_REVIEW_COMPLETE', N'TPRM review complete: {{vendorName}}',
            N'Review of {{displayId}} for {{vendorName}} is complete. Posture {{posturePercent}}%. Compliance: {{complianceStatus}}.',
            1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_NOT_COMPLIANT')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_NOT_COMPLIANT', N'Vendor not compliant: {{vendorName}}',
            N'Vendor “{{vendorName}}” did not meet the tier posture target on {{displayId}}. Consider raising a linked ERM risk.',
            1, 1, 1, 1);

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_CONTRACT_EXPIRY')
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_CONTRACT_EXPIRY', N'Vendor contract expiry: {{vendorName}}',
            N'Contract {{contractCode}} for {{vendorName}} ends on {{contractEnd}}.',
            1, 1, 1, 1);
