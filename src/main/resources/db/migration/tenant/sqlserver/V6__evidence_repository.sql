-- Central evidence repository (req §17.2). Platform always-on. Files on disk/Blob; metadata here.

CREATE TABLE evidence (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    title              NVARCHAR(500) NOT NULL,
    classification     VARCHAR(32)   NOT NULL,
    status             VARCHAR(32)   NOT NULL CONSTRAINT DF_evidence_status DEFAULT 'ACTIVE',
    expires_on         DATE          NULL,
    current_version    INT           NOT NULL CONSTRAINT DF_evidence_curver DEFAULT 1,
    created_by_user_id BIGINT        NOT NULL,
    created_at         DATETIME2     NOT NULL CONSTRAINT DF_evidence_created DEFAULT SYSUTCDATETIME(),
    updated_at         DATETIME2     NOT NULL CONSTRAINT DF_evidence_updated DEFAULT SYSUTCDATETIME(),
    deleted_at         DATETIME2     NULL,
    CONSTRAINT fk_evidence_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_evidence_status ON evidence (status);
CREATE INDEX idx_evidence_classification ON evidence (classification);
CREATE INDEX idx_evidence_expires ON evidence (expires_on);
CREATE INDEX idx_evidence_deleted ON evidence (deleted_at);

CREATE TABLE evidence_version (
    id                  BIGINT IDENTITY(1,1) PRIMARY KEY,
    evidence_id         BIGINT         NOT NULL,
    version_number      INT            NOT NULL,
    original_filename   NVARCHAR(500)  NOT NULL,
    content_type        NVARCHAR(255)  NOT NULL,
    size_bytes          BIGINT         NOT NULL,
    storage_key         NVARCHAR(1000) NOT NULL,
    virus_scan_status   VARCHAR(32)    NOT NULL CONSTRAINT DF_ev_scan DEFAULT 'SKIPPED',
    uploaded_by_user_id BIGINT         NOT NULL,
    uploaded_at         DATETIME2      NOT NULL CONSTRAINT DF_ev_uploaded DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_evidence_version_evidence FOREIGN KEY (evidence_id) REFERENCES evidence (id),
    CONSTRAINT fk_evidence_version_uploader FOREIGN KEY (uploaded_by_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_evidence_version UNIQUE (evidence_id, version_number)
);

CREATE INDEX idx_evidence_version_evidence ON evidence_version (evidence_id);

CREATE TABLE evidence_link (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    evidence_id       BIGINT        NOT NULL,
    module_code       VARCHAR(32)   NOT NULL,
    record_type       VARCHAR(64)   NOT NULL,
    record_id         VARCHAR(64)   NOT NULL,
    workflow_stage    VARCHAR(64)   NULL,
    linked_by_user_id BIGINT        NOT NULL,
    linked_at         DATETIME2     NOT NULL CONSTRAINT DF_el_linked DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_evidence_link_evidence FOREIGN KEY (evidence_id) REFERENCES evidence (id),
    CONSTRAINT fk_evidence_link_user FOREIGN KEY (linked_by_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_evidence_link UNIQUE (evidence_id, module_code, record_type, record_id)
);

CREATE INDEX idx_evidence_link_evidence ON evidence_link (evidence_id);
CREATE INDEX idx_evidence_link_record ON evidence_link (module_code, record_type, record_id);
