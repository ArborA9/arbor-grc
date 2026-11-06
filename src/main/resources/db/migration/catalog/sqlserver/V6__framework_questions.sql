-- A2 question bank (Type A catalog).

CREATE TABLE framework_question (
    id                 BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    framework_id       BIGINT        NOT NULL,
    framework_node_id  BIGINT        NOT NULL,
    question_code      VARCHAR(64)   NOT NULL,
    question_type      VARCHAR(64)   NULL,
    question_text      NVARCHAR(MAX) NOT NULL,
    evidence_guidance  NVARCHAR(MAX) NULL,
    display_order      INT           NOT NULL CONSTRAINT DF_fq_order DEFAULT 0,
    version_number     INT           NOT NULL,
    active             BIT           NOT NULL CONSTRAINT DF_fq_active DEFAULT 1,
    created_at         DATETIME2     NOT NULL CONSTRAINT DF_fq_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_fq_framework FOREIGN KEY (framework_id) REFERENCES framework (id) ON DELETE CASCADE,
    CONSTRAINT fk_fq_node FOREIGN KEY (framework_node_id) REFERENCES framework_node (id),
    CONSTRAINT uq_fq_code_ver UNIQUE (framework_id, question_code, version_number)
);

CREATE INDEX idx_fq_framework ON framework_question (framework_id);
CREATE INDEX idx_fq_node ON framework_question (framework_node_id);
CREATE INDEX idx_fq_active_ver ON framework_question (framework_id, version_number, active);
