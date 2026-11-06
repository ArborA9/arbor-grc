-- Compliance Project: annual assessment container. SoA/Gap bind to project + framework.

CREATE TABLE compliance_project (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    name               NVARCHAR(255) NOT NULL,
    assessment_year    INT           NOT NULL,
    status             VARCHAR(32)   NOT NULL CONSTRAINT DF_cp_status DEFAULT 'ACTIVE',
    created_by_user_id BIGINT        NOT NULL,
    created_at         DATETIME2     NOT NULL CONSTRAINT DF_cp_created DEFAULT SYSUTCDATETIME(),
    updated_at         DATETIME2     NOT NULL CONSTRAINT DF_cp_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_cp_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_cp_year ON compliance_project (assessment_year);
CREATE INDEX idx_cp_status ON compliance_project (status);

CREATE TABLE compliance_project_framework (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    project_id        BIGINT         NOT NULL,
    framework_id      BIGINT         NOT NULL,
    framework_code    VARCHAR(64)    NOT NULL,
    framework_name    NVARCHAR(255)  NOT NULL,
    framework_version VARCHAR(64)    NOT NULL,
    CONSTRAINT fk_cpf_project FOREIGN KEY (project_id) REFERENCES compliance_project (id) ON DELETE CASCADE,
    CONSTRAINT uq_cpf_project_framework UNIQUE (project_id, framework_id)
);

CREATE INDEX idx_cpf_project ON compliance_project_framework (project_id);

ALTER TABLE soa DROP CONSTRAINT uq_soa_framework;
ALTER TABLE soa ADD project_id BIGINT NULL;
GO
-- Empty at migrate time on fresh tenants; NOT NULL enforced after backfill N/A.
ALTER TABLE soa ALTER COLUMN project_id BIGINT NOT NULL;
ALTER TABLE soa ADD CONSTRAINT fk_soa_project FOREIGN KEY (project_id) REFERENCES compliance_project (id);
ALTER TABLE soa ADD CONSTRAINT uq_soa_project_framework UNIQUE (project_id, framework_id);
CREATE INDEX idx_soa_project ON soa (project_id);
GO

ALTER TABLE gap_assessment ADD project_id BIGINT NULL;
GO
ALTER TABLE gap_assessment ALTER COLUMN project_id BIGINT NOT NULL;
ALTER TABLE gap_assessment ADD CONSTRAINT fk_gap_project FOREIGN KEY (project_id) REFERENCES compliance_project (id);
CREATE INDEX idx_gap_project ON gap_assessment (project_id);
