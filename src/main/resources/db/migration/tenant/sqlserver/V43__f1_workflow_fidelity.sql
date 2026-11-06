-- F1 workflow fidelity: TPRM answer evidence, vendor review notifications, gap custom questions.

ALTER TABLE tprm_assessment_answer ADD evidence_id BIGINT NULL;
GO
ALTER TABLE tprm_assessment_answer ADD CONSTRAINT fk_tprm_answer_evidence
    FOREIGN KEY (evidence_id) REFERENCES evidence (id);

ALTER TABLE tprm_vendor ADD review_due_notified_at DATETIME2 NULL;
ALTER TABLE tprm_vendor ADD review_overdue_notified_at DATETIME2 NULL;

CREATE TABLE gap_item_custom_question (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    gap_item_id         BIGINT        NOT NULL,
    question_text       NVARCHAR(2000) NOT NULL,
    sort_order          INT           NOT NULL DEFAULT 0,
    created_by_user_id  BIGINT        NOT NULL,
    created_at          DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_gicq_item FOREIGN KEY (gap_item_id) REFERENCES gap_item (id),
    CONSTRAINT fk_gicq_user FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_gicq_item ON gap_item_custom_question (gap_item_id);

ALTER TABLE gap_item_answer ALTER COLUMN framework_question_id BIGINT NULL;
ALTER TABLE gap_item_answer ADD custom_question_id BIGINT NULL;
GO
ALTER TABLE gap_item_answer ADD CONSTRAINT fk_gia_custom_q
    FOREIGN KEY (custom_question_id) REFERENCES gap_item_custom_question (id);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'VENDOR_REVIEW_DUE',
       N'[Reminder] Vendor review due — {{vendorName}}',
       N'Vendor {{vendorName}} is due for review on {{reviewDate}}. Residual risk: {{residualRisk}}.',
       1, 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'VENDOR_REVIEW_DUE' AND version = 1);

INSERT INTO notification_template (trigger_type, subject_template, body_template, channel_email, channel_in_app, version, is_active)
SELECT 'VENDOR_REVIEW_OVERDUE',
       N'[Overdue] Vendor review past due — {{vendorName}}',
       N'Vendor {{vendorName}} review was due on {{reviewDate}}. Please complete the review.',
       1, 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE trigger_type = 'VENDOR_REVIEW_OVERDUE' AND version = 1);
