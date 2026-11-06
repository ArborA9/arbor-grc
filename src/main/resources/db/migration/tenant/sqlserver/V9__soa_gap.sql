-- Statement of Applicability + Gap Assessment (ISMS). Catalog framework/node ids stored by value.

CREATE TABLE soa (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    framework_id         BIGINT         NOT NULL,
    framework_code       VARCHAR(64)    NOT NULL,
    framework_name       NVARCHAR(255)  NOT NULL,
    framework_version    VARCHAR(64)    NOT NULL,
    status               VARCHAR(32)    NOT NULL CONSTRAINT DF_soa_status DEFAULT 'DRAFT',
    completed_at         DATETIME2      NULL,
    completed_by_user_id BIGINT         NULL,
    created_by_user_id   BIGINT         NOT NULL,
    created_at           DATETIME2      NOT NULL CONSTRAINT DF_soa_created DEFAULT SYSUTCDATETIME(),
    updated_at           DATETIME2      NOT NULL CONSTRAINT DF_soa_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_soa_framework UNIQUE (framework_id),
    CONSTRAINT fk_soa_completed_by FOREIGN KEY (completed_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_soa_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_soa_status ON soa (status);

CREATE TABLE soa_item (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    soa_id             BIGINT         NOT NULL,
    framework_node_id  BIGINT         NOT NULL,
    node_code          VARCHAR(64)    NOT NULL,
    title              NVARCHAR(512)  NOT NULL,
    applicability      VARCHAR(32)    NULL,
    justification      NVARCHAR(MAX)  NULL,
    remarks            NVARCHAR(MAX)  NULL,
    updated_by_user_id BIGINT         NULL,
    updated_at         DATETIME2      NOT NULL CONSTRAINT DF_soa_item_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_soa_item_soa FOREIGN KEY (soa_id) REFERENCES soa (id) ON DELETE CASCADE,
    CONSTRAINT fk_soa_item_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_soa_item_node UNIQUE (soa_id, framework_node_id)
);

CREATE INDEX idx_soa_item_soa ON soa_item (soa_id);

CREATE TABLE gap_assessment (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    framework_id         BIGINT         NOT NULL,
    framework_code       VARCHAR(64)    NOT NULL,
    framework_name       NVARCHAR(255)  NOT NULL,
    framework_version    VARCHAR(64)    NOT NULL,
    soa_id               BIGINT         NULL,
    name                 NVARCHAR(255)  NOT NULL,
    assessment_type      VARCHAR(32)    NOT NULL,
    status               VARCHAR(32)    NOT NULL CONSTRAINT DF_gap_status DEFAULT 'DRAFT',
    target_date          DATE           NULL,
    overall_score        DECIMAL(6, 2)  NULL,
    rag_status           VARCHAR(32)    NULL,
    applicable_count     INT            NOT NULL CONSTRAINT DF_gap_app_cnt DEFAULT 0,
    scored_count         INT            NOT NULL CONSTRAINT DF_gap_scored DEFAULT 0,
    created_by_user_id   BIGINT         NOT NULL,
    completed_at         DATETIME2      NULL,
    completed_by_user_id BIGINT         NULL,
    created_at           DATETIME2      NOT NULL CONSTRAINT DF_gap_created DEFAULT SYSUTCDATETIME(),
    updated_at           DATETIME2      NOT NULL CONSTRAINT DF_gap_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_gap_soa FOREIGN KEY (soa_id) REFERENCES soa (id),
    CONSTRAINT fk_gap_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_gap_completed_by FOREIGN KEY (completed_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_gap_framework ON gap_assessment (framework_id);
CREATE INDEX idx_gap_status ON gap_assessment (status);

CREATE TABLE gap_item (
    id                        BIGINT IDENTITY(1,1) PRIMARY KEY,
    gap_assessment_id         BIGINT         NOT NULL,
    framework_node_id         BIGINT         NOT NULL,
    node_code                 VARCHAR(64)    NOT NULL,
    title                     NVARCHAR(512)  NOT NULL,
    applicability             VARCHAR(32)    NOT NULL,
    applicability_override    BIT            NOT NULL CONSTRAINT DF_gap_item_ovr DEFAULT 0,
    override_justification    NVARCHAR(MAX)  NULL,
    implementation_status     VARCHAR(32)    NULL,
    observations              NVARCHAR(MAX)  NULL,
    gap_description           NVARCHAR(MAX)  NULL,
    score                     INT            NULL,
    updated_by_user_id        BIGINT         NULL,
    updated_at                DATETIME2      NOT NULL CONSTRAINT DF_gap_item_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_gap_item_assessment FOREIGN KEY (gap_assessment_id) REFERENCES gap_assessment (id) ON DELETE CASCADE,
    CONSTRAINT fk_gap_item_updated_by FOREIGN KEY (updated_by_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_gap_item_node UNIQUE (gap_assessment_id, framework_node_id)
);

CREATE INDEX idx_gap_item_assessment ON gap_item (gap_assessment_id);
