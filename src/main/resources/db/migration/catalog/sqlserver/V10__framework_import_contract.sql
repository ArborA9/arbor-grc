-- F1 of the framework Excel redesign: typed routing flags, free-form node attributes and import
-- staging. Behaviour-neutral — the flags are backfilled to the heuristic
-- FrameworkLibraryService.SoaGapSupport.isControlNode still uses (definition text present OR level >= 3),
-- and no consumer reads them until F6-F8.

ALTER TABLE framework_node ADD
    record_type        VARCHAR(32)   NULL,
    official_ref       VARCHAR(128)  NULL,
    in_soa             BIT NOT NULL CONSTRAINT DF_framework_node_in_soa DEFAULT 0,
    in_gap_assessment  BIT NOT NULL CONSTRAINT DF_framework_node_in_gap DEFAULT 0,
    in_audit_checklist BIT NOT NULL CONSTRAINT DF_framework_node_in_audit DEFAULT 0,
    in_risk_register   BIT NOT NULL CONSTRAINT DF_framework_node_in_risk DEFAULT 0;
GO

UPDATE framework_node
SET in_soa = 1,
    in_gap_assessment = 1,
    in_audit_checklist = 1,
    in_risk_register = 1
WHERE level >= 3
   OR (definition_text IS NOT NULL AND LTRIM(RTRIM(definition_text)) <> '');
GO

CREATE INDEX idx_framework_node_soa ON framework_node (framework_id, in_soa);
CREATE INDEX idx_framework_node_gap ON framework_node (framework_id, in_gap_assessment);
CREATE INDEX idx_framework_node_audit ON framework_node (framework_id, in_audit_checklist);
CREATE INDEX idx_framework_node_risk ON framework_node (framework_id, in_risk_register);
CREATE INDEX idx_framework_node_record_type ON framework_node (framework_id, record_type);
GO

-- Open key/value bag so reference-pack columns (section_type, is_mandatory, default_applicability,
-- what_it_means, evidence_examples, baseline, related_controls, testing_procedure) land without a migration.
CREATE TABLE framework_node_attribute (
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    node_id           BIGINT        NOT NULL,
    attr_key          VARCHAR(64)   NOT NULL,
    attr_value        NVARCHAR(MAX) NULL,
    CONSTRAINT fk_fna_node FOREIGN KEY (node_id) REFERENCES framework_node (id) ON DELETE CASCADE,
    CONSTRAINT uq_fna_node_key UNIQUE (node_id, attr_key)
);

CREATE INDEX idx_fna_node ON framework_node_attribute (node_id);
GO

-- Raw staging so an import can be dry-run, diffed and re-reported without touching the live catalog.
CREATE TABLE framework_import_batch (
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    source_filename   NVARCHAR(512) NOT NULL,
    source_format     VARCHAR(32)   NOT NULL,
    status            VARCHAR(32)   NOT NULL,
    dry_run           BIT           NOT NULL CONSTRAINT DF_fib_dry_run DEFAULT 1,
    framework_code    VARCHAR(64)   NULL,
    framework_version VARCHAR(64)   NULL,
    framework_id      BIGINT        NULL,
    row_count         INT           NOT NULL CONSTRAINT DF_fib_row_count DEFAULT 0,
    error_count       INT           NOT NULL CONSTRAINT DF_fib_error_count DEFAULT 0,
    report            NVARCHAR(MAX) NULL,
    started_by        NVARCHAR(255) NULL,
    started_at        DATETIME2     NOT NULL CONSTRAINT DF_fib_started DEFAULT SYSUTCDATETIME(),
    finished_at       DATETIME2     NULL,
    CONSTRAINT fk_fib_framework FOREIGN KEY (framework_id) REFERENCES framework (id)
);

CREATE INDEX idx_fib_status ON framework_import_batch (status);
CREATE INDEX idx_fib_framework ON framework_import_batch (framework_id);
GO

CREATE TABLE framework_import_row (
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    batch_id          BIGINT        NOT NULL,
    sheet_name        NVARCHAR(255) NULL,
    row_index         INT           NOT NULL,
    row_kind          VARCHAR(32)   NULL,
    raw_json          NVARCHAR(MAX) NULL,
    status            VARCHAR(32)   NOT NULL,
    message           NVARCHAR(1024) NULL,
    CONSTRAINT fk_fir_batch FOREIGN KEY (batch_id) REFERENCES framework_import_batch (id) ON DELETE CASCADE
);

CREATE INDEX idx_fir_batch ON framework_import_row (batch_id);
CREATE INDEX idx_fir_batch_status ON framework_import_row (batch_id, status);
GO

ALTER TABLE framework_question ADD
    control_code    VARCHAR(64)   NULL,
    question_number VARCHAR(64)   NULL,
    answer_options  NVARCHAR(255) NULL,
    is_mandatory    BIT NOT NULL CONSTRAINT DF_fq_is_mandatory DEFAULT 0;
GO

-- Legacy rows carry a framework-unique question_code; splitting it into (control_code, question_number)
-- this way preserves the old uniqueness guarantee exactly, so the key move below cannot reject data.
UPDATE q
SET q.control_code = n.node_code,
    q.question_number = q.question_code
FROM framework_question q
JOIN framework_node n ON n.id = q.framework_node_id;
GO

ALTER TABLE framework_question ALTER COLUMN control_code VARCHAR(64) NOT NULL;
GO
ALTER TABLE framework_question ALTER COLUMN question_number VARCHAR(64) NOT NULL;
GO

-- Q1..Qn repeats per control in the new packs, so question_code alone can no longer be the key.
ALTER TABLE framework_question DROP CONSTRAINT uq_fq_code_ver;
GO
ALTER TABLE framework_question
    ADD CONSTRAINT uq_fq_control_question UNIQUE (framework_id, control_code, question_number, version_number);
GO

CREATE INDEX idx_fq_control_code ON framework_question (framework_id, control_code);
GO
