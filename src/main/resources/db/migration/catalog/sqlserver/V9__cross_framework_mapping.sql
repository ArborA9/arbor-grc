-- A3 cross-framework control mappings (Type A catalog).

CREATE TABLE cross_framework_mapping (
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    source_node_id    BIGINT        NOT NULL,
    target_node_id    BIGINT        NOT NULL,
    mapping_type      VARCHAR(32)   NOT NULL,
    justification     NVARCHAR(MAX) NULL,
    loaded_at         DATETIME2     NOT NULL CONSTRAINT DF_cfm_loaded DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_cfm_source FOREIGN KEY (source_node_id) REFERENCES framework_node (id) ON DELETE CASCADE,
    CONSTRAINT fk_cfm_target FOREIGN KEY (target_node_id) REFERENCES framework_node (id),
    CONSTRAINT uq_cfm_pair UNIQUE (source_node_id, target_node_id)
);

CREATE INDEX idx_cfm_source ON cross_framework_mapping (source_node_id);
CREATE INDEX idx_cfm_target ON cross_framework_mapping (target_node_id);
