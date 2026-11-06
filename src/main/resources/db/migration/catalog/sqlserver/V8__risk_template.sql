-- A4 curated risk library (Type A catalog).

CREATE TABLE risk_template (
    id                              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    framework_id                    BIGINT        NOT NULL,
    framework_node_id               BIGINT        NULL,
    source_risk_id                  VARCHAR(64)   NOT NULL,
    title                           VARCHAR(512)  NOT NULL,
    description                     NVARCHAR(MAX) NULL,
    category                        VARCHAR(32)   NOT NULL,
    typical_threats                 NVARCHAR(MAX) NULL,
    suggested_probability           VARCHAR(16)   NOT NULL,
    suggested_impact                VARCHAR(16)   NOT NULL,
    suggested_control_effectiveness VARCHAR(16)   NOT NULL,
    treatment_option                VARCHAR(32)   NULL,
    treatment_recommendation        NVARCHAR(MAX) NULL,
    display_order                   INT           NOT NULL CONSTRAINT DF_rt_order DEFAULT 0,
    version_number                  INT           NOT NULL,
    active                          BIT           NOT NULL CONSTRAINT DF_rt_active DEFAULT 1,
    created_at                      DATETIME2     NOT NULL CONSTRAINT DF_rt_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_rt_framework FOREIGN KEY (framework_id) REFERENCES framework (id) ON DELETE CASCADE,
    CONSTRAINT fk_rt_node FOREIGN KEY (framework_node_id) REFERENCES framework_node (id),
    CONSTRAINT uq_rt_source_ver UNIQUE (framework_id, source_risk_id, version_number)
);

CREATE INDEX idx_rt_framework ON risk_template (framework_id);
CREATE INDEX idx_rt_active_ver ON risk_template (framework_id, version_number, active);
