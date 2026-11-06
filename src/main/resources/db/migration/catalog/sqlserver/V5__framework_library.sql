-- Type A platform framework control libraries (import → validate → publish).
CREATE TABLE framework (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    code              VARCHAR(64)   NOT NULL,
    name              NVARCHAR(255) NOT NULL,
    version           VARCHAR(64)   NOT NULL,
    module_code       VARCHAR(32)   NOT NULL,
    status            VARCHAR(32)   NOT NULL,
    control_count     INT           NOT NULL CONSTRAINT DF_framework_control_count DEFAULT 0,
    imported_by       NVARCHAR(255) NULL,
    imported_at       DATETIME2     NULL,
    validated_by      NVARCHAR(255) NULL,
    validated_at      DATETIME2     NULL,
    published_by      NVARCHAR(255) NULL,
    published_at      DATETIME2     NULL,
    source_filename   NVARCHAR(512) NULL,
    CONSTRAINT uq_framework_code_version UNIQUE (code, version)
);

CREATE INDEX idx_framework_status ON framework (status);
CREATE INDEX idx_framework_module ON framework (module_code);

CREATE TABLE framework_node (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    framework_id      BIGINT        NOT NULL,
    parent_id         BIGINT        NULL,
    level             INT           NOT NULL,
    node_code         VARCHAR(64)   NOT NULL,
    title             NVARCHAR(512) NOT NULL,
    definition_text   NVARCHAR(MAX) NULL,
    control_type      VARCHAR(64)   NULL,
    priority          VARCHAR(8)    NULL,
    display_order     INT           NOT NULL CONSTRAINT DF_framework_node_order DEFAULT 0,
    CONSTRAINT fk_framework_node_framework FOREIGN KEY (framework_id) REFERENCES framework (id) ON DELETE CASCADE,
    CONSTRAINT fk_framework_node_parent FOREIGN KEY (parent_id) REFERENCES framework_node (id),
    CONSTRAINT uq_framework_node_code UNIQUE (framework_id, node_code)
);

CREATE INDEX idx_framework_node_framework ON framework_node (framework_id);
CREATE INDEX idx_framework_node_parent ON framework_node (parent_id);

CREATE TABLE tenant_framework (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    tenant_id         VARCHAR(64)   NOT NULL,
    framework_id      BIGINT        NOT NULL,
    activated_at      DATETIME2     NOT NULL,
    activated_by      NVARCHAR(255) NULL,
    CONSTRAINT uq_tenant_framework UNIQUE (tenant_id, framework_id),
    CONSTRAINT fk_tenant_framework_fw FOREIGN KEY (framework_id) REFERENCES framework (id)
);

CREATE INDEX idx_tenant_framework_tenant ON tenant_framework (tenant_id);
