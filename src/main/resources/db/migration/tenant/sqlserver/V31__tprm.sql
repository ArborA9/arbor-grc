-- TPRM — Third Party Risk Management (req §14.4, Journey 14) — SQL Server twin.

CREATE TABLE tprm_vendor (
    id                     BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name                   NVARCHAR(255) NOT NULL,
    category               NVARCHAR(64)  NOT NULL,
    criticality            NVARCHAR(32)  NOT NULL,
    contact_name           NVARCHAR(255) NULL,
    contact_email          NVARCHAR(255) NOT NULL,
    status                 NVARCHAR(32)  NOT NULL CONSTRAINT DF_tprm_vendor_status DEFAULT N'ACTIVE',
    residual_risk          NVARCHAR(32)  NULL,
    inherent_risk          NVARCHAR(32)  NULL,
    probability            NVARCHAR(32)  NULL,
    impact                 NVARCHAR(32)  NULL,
    control_effectiveness  NVARCHAR(32)  NULL,
    next_review_date       DATE          NULL,
    contract_expiry_date   DATE          NULL,
    notes                  NVARCHAR(2000) NULL,
    linked_erm_risk_id     BIGINT        NULL,
    created_by_user_id     BIGINT        NOT NULL,
    created_at             DATETIME2     NOT NULL CONSTRAINT DF_tprm_vendor_created DEFAULT SYSUTCDATETIME(),
    updated_at             DATETIME2     NOT NULL CONSTRAINT DF_tprm_vendor_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_tprm_vendor_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_tprm_vendor_erm_risk FOREIGN KEY (linked_erm_risk_id) REFERENCES risk_record (id)
);

CREATE INDEX idx_tprm_vendor_name ON tprm_vendor (name);
CREATE INDEX idx_tprm_vendor_email ON tprm_vendor (contact_email);

CREATE TABLE tprm_questionnaire_template (
    id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name               NVARCHAR(255) NOT NULL,
    source             NVARCHAR(32)  NOT NULL,
    is_active          BIT           NOT NULL CONSTRAINT DF_tprm_template_active DEFAULT 1,
    created_by_user_id BIGINT        NULL,
    created_at         DATETIME2     NOT NULL CONSTRAINT DF_tprm_template_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_tprm_template_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE TABLE tprm_questionnaire_question (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    template_id    BIGINT         NOT NULL,
    section_name   NVARCHAR(255)  NOT NULL,
    sort_order     INT            NOT NULL CONSTRAINT DF_tprm_question_sort DEFAULT 0,
    question_text  NVARCHAR(1000) NOT NULL,
    answer_type    NVARCHAR(32)   NOT NULL,
    required       BIT            NOT NULL CONSTRAINT DF_tprm_question_required DEFAULT 1,
    CONSTRAINT fk_tprm_question_template FOREIGN KEY (template_id) REFERENCES tprm_questionnaire_template (id) ON DELETE CASCADE
);

CREATE INDEX idx_tprm_question_template ON tprm_questionnaire_question (template_id);

CREATE TABLE tprm_assessment (
    id                     BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    vendor_id              BIGINT        NOT NULL,
    template_id            BIGINT        NOT NULL,
    display_id             NVARCHAR(32)  NOT NULL,
    title                  NVARCHAR(255) NULL,
    status                 NVARCHAR(32)  NOT NULL CONSTRAINT DF_tprm_assessment_status DEFAULT N'DRAFT',
    expires_at             DATETIME2     NULL,
    sent_at                DATETIME2     NULL,
    submitted_at           DATETIME2     NULL,
    scored_at              DATETIME2     NULL,
    probability            NVARCHAR(32)  NULL,
    impact                 NVARCHAR(32)  NULL,
    inherent_risk          NVARCHAR(32)  NULL,
    control_effectiveness  NVARCHAR(32)  NULL,
    residual_risk          NVARCHAR(32)  NULL,
    raise_erm_prompted     BIT           NOT NULL CONSTRAINT DF_tprm_assessment_raise_erm DEFAULT 0,
    linked_erm_risk_id     BIGINT        NULL,
    scored_by_user_id      BIGINT        NULL,
    created_by_user_id     BIGINT        NOT NULL,
    created_at             DATETIME2     NOT NULL CONSTRAINT DF_tprm_assessment_created DEFAULT SYSUTCDATETIME(),
    updated_at             DATETIME2     NOT NULL CONSTRAINT DF_tprm_assessment_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_tprm_assessment_display_id UNIQUE (display_id),
    CONSTRAINT fk_tprm_assessment_vendor FOREIGN KEY (vendor_id) REFERENCES tprm_vendor (id),
    CONSTRAINT fk_tprm_assessment_template FOREIGN KEY (template_id) REFERENCES tprm_questionnaire_template (id),
    CONSTRAINT fk_tprm_assessment_scored_by FOREIGN KEY (scored_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_tprm_assessment_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_tprm_assessment_erm_risk FOREIGN KEY (linked_erm_risk_id) REFERENCES risk_record (id)
);

CREATE INDEX idx_tprm_assessment_vendor ON tprm_assessment (vendor_id);
CREATE INDEX idx_tprm_assessment_status ON tprm_assessment (status);

CREATE TABLE tprm_assessment_answer (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    assessment_id BIGINT         NOT NULL,
    question_id   BIGINT         NOT NULL,
    answer_text   NVARCHAR(4000) NULL,
    notes         NVARCHAR(2000) NULL,
    CONSTRAINT fk_tprm_answer_assessment FOREIGN KEY (assessment_id) REFERENCES tprm_assessment (id) ON DELETE CASCADE,
    CONSTRAINT fk_tprm_answer_question FOREIGN KEY (question_id) REFERENCES tprm_questionnaire_question (id),
    CONSTRAINT uq_tprm_answer_assessment_question UNIQUE (assessment_id, question_id)
);

CREATE TABLE tprm_vendor_token (
    id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
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

ALTER TABLE risk_record ADD tprm_vendor_id BIGINT NULL;
GO
ALTER TABLE risk_record ADD CONSTRAINT fk_risk_tprm_vendor FOREIGN KEY (tprm_vendor_id) REFERENCES tprm_vendor (id);

-- Seed: Arbor default vendor questionnaire (6 sections x 2 questions — Journey 14 samples).
IF NOT EXISTS (SELECT 1 FROM tprm_questionnaire_template WHERE source = N'ARBOR_DEFAULT')
BEGIN
    INSERT INTO tprm_questionnaire_template (name, source, is_active, created_by_user_id)
    VALUES (N'Arbor Vendor Security Assessment', N'ARBOR_DEFAULT', 1, NULL);
END;

IF NOT EXISTS (SELECT 1 FROM tprm_questionnaire_question q
               JOIN tprm_questionnaire_template t ON t.id = q.template_id
               WHERE t.source = N'ARBOR_DEFAULT')
BEGIN
    DECLARE @tpl_id BIGINT = (SELECT TOP 1 id FROM tprm_questionnaire_template WHERE source = N'ARBOR_DEFAULT');

    INSERT INTO tprm_questionnaire_question (template_id, section_name, sort_order, question_text, answer_type, required)
    VALUES
        (@tpl_id, N'Information Security Management', 0, N'Does your organisation hold a current ISO 27001 (or equivalent ISMS) certification?', N'YES_NO', 1),
        (@tpl_id, N'Information Security Management', 1, N'If certified, what is the certificate expiry date?', N'DATE', 0),
        (@tpl_id, N'Access Control', 2, N'Do you enforce multi-factor authentication for privileged/remote access to systems processing our data?', N'YES_NO', 1),
        (@tpl_id, N'Access Control', 3, N'Describe your user access review and de-provisioning process.', N'TEXT', 1),
        (@tpl_id, N'Incident Response', 4, N'Do you maintain a documented and tested incident response plan?', N'YES_NO', 1),
        (@tpl_id, N'Incident Response', 5, N'What is your contractual notification window for reporting a security incident affecting our data?', N'TEXT', 1),
        (@tpl_id, N'Data Residency & Protection', 6, N'Where is our data stored and processed (country/region)?', N'TEXT', 1),
        (@tpl_id, N'Data Residency & Protection', 7, N'Is data encrypted at rest and in transit?', N'YES_NO', 1),
        (@tpl_id, N'Business Continuity', 8, N'Do you maintain a business continuity / disaster recovery plan covering the services provided to us?', N'YES_NO', 1),
        (@tpl_id, N'Business Continuity', 9, N'What is your committed Recovery Time Objective (RTO) for the services provided to us?', N'TEXT', 0),
        (@tpl_id, N'Subcontractors & Fourth Parties', 10, N'Do you use subcontractors or fourth parties to deliver the service to us?', N'YES_NO', 1),
        (@tpl_id, N'Subcontractors & Fourth Parties', 11, N'If yes, describe the due diligence performed on those subcontractors.', N'TEXT', 0);
END;

-- Notification templates (req §7, rows 35, 38, 45; OTP is a distinct trigger from the link).
IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_SENT')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_QUESTIONNAIRE_SENT', N'Security questionnaire request — {{vendorName}}', N'Hello {{vendorName}}, please complete the requested security questionnaire using this secure link: {{accessLink}} (expires {{expiresAt}}). A one-time passcode will follow in a separate email.', 1, 0, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_OTP')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_QUESTIONNAIRE_OTP', N'Your one-time passcode', N'Your one-time passcode for the vendor security questionnaire is: {{otpCode}} (valid until {{expiresAt}}).', 1, 0, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_QUESTIONNAIRE_SUBMITTED')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_QUESTIONNAIRE_SUBMITTED', N'Vendor questionnaire submitted: {{displayId}}', N'The vendor has submitted their responses for “{{assessmentTitle}}” ({{displayId}}). Please review and score the assessment.', 1, 1, 1, 1);
END;

IF NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = N'VENDOR_HIGH_RISK')
BEGIN
    INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
    VALUES (N'VENDOR_HIGH_RISK', N'High-risk vendor detected: {{vendorName}}', N'Vendor “{{vendorName}}” scored a {{residualRisk}} residual risk on assessment “{{assessmentTitle}}”. Consider raising a linked ERM risk.', 1, 1, 1, 1);
END;
