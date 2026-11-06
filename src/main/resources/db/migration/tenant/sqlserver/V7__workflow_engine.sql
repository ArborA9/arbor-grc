-- Central workflow engine (approvals state machine). Platform always-on.
-- Seed: SIMPLE_APPROVAL for demos until SoA/Gap/Risk wire real definitions.

CREATE TABLE workflow_definition (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    code           VARCHAR(64)    NOT NULL,
    name           NVARCHAR(200)  NOT NULL,
    module_code    VARCHAR(32)    NOT NULL,
    record_type    VARCHAR(64)    NOT NULL,
    initial_state  VARCHAR(64)    NOT NULL,
    version        INT            NOT NULL CONSTRAINT DF_wd_ver DEFAULT 1,
    is_active      BIT            NOT NULL CONSTRAINT DF_wd_active DEFAULT 1,
    created_at     DATETIME2      NOT NULL CONSTRAINT DF_wd_created DEFAULT SYSUTCDATETIME()
);

CREATE UNIQUE INDEX uq_workflow_definition_code_ver ON workflow_definition (code, version);

CREATE TABLE workflow_transition_def (
    id                BIGINT IDENTITY(1,1) PRIMARY KEY,
    definition_id     BIGINT         NOT NULL,
    from_state        VARCHAR(64)    NOT NULL,
    to_state          VARCHAR(64)    NOT NULL,
    action_code       VARCHAR(64)    NOT NULL,
    allowed_roles     NVARCHAR(255)  NOT NULL,
    requires_comment  BIT            NOT NULL CONSTRAINT DF_wtd_comment DEFAULT 0,
    CONSTRAINT fk_wtd_definition FOREIGN KEY (definition_id) REFERENCES workflow_definition (id)
);

CREATE INDEX idx_wtd_definition ON workflow_transition_def (definition_id);

CREATE TABLE workflow_instance (
    id                 BIGINT IDENTITY(1,1) PRIMARY KEY,
    definition_id      BIGINT         NOT NULL,
    module_code        VARCHAR(32)    NOT NULL,
    record_type        VARCHAR(64)    NOT NULL,
    record_id          VARCHAR(64)    NOT NULL,
    record_name        NVARCHAR(500)  NULL,
    current_state      VARCHAR(64)    NOT NULL,
    status             VARCHAR(32)    NOT NULL CONSTRAINT DF_wi_status DEFAULT 'OPEN',
    started_by_user_id BIGINT         NOT NULL,
    started_at         DATETIME2      NOT NULL CONSTRAINT DF_wi_started DEFAULT SYSUTCDATETIME(),
    completed_at       DATETIME2      NULL,
    CONSTRAINT fk_wi_definition FOREIGN KEY (definition_id) REFERENCES workflow_definition (id),
    CONSTRAINT fk_wi_started_by FOREIGN KEY (started_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_wi_record ON workflow_instance (module_code, record_type, record_id);
CREATE INDEX idx_wi_status ON workflow_instance (status);
CREATE INDEX idx_wi_state ON workflow_instance (current_state);

CREATE TABLE workflow_transition_log (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    instance_id      BIGINT          NOT NULL,
    from_state       VARCHAR(64)     NOT NULL,
    to_state         VARCHAR(64)     NOT NULL,
    action_code      VARCHAR(64)     NOT NULL,
    actor_user_id    BIGINT          NOT NULL,
    comment_text     NVARCHAR(2000)  NULL,
    created_at       DATETIME2       NOT NULL CONSTRAINT DF_wtl_created DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_wtl_instance FOREIGN KEY (instance_id) REFERENCES workflow_instance (id),
    CONSTRAINT fk_wtl_actor FOREIGN KEY (actor_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_wtl_instance ON workflow_transition_log (instance_id, created_at);

INSERT INTO workflow_definition (code, name, module_code, record_type, initial_state, version, is_active)
VALUES ('SIMPLE_APPROVAL', N'Simple approval', 'PLATFORM', 'DEMO_RECORD', 'DRAFT', 1, 1);

INSERT INTO workflow_transition_def (definition_id, from_state, to_state, action_code, allowed_roles, requires_comment)
SELECT id, 'DRAFT', 'PENDING_APPROVAL', 'submit', N'OWNER,ADMIN,CONTRIBUTOR', 0
FROM workflow_definition WHERE code = 'SIMPLE_APPROVAL' AND version = 1;

INSERT INTO workflow_transition_def (definition_id, from_state, to_state, action_code, allowed_roles, requires_comment)
SELECT id, 'PENDING_APPROVAL', 'APPROVED', 'approve', N'OWNER,ADMIN', 0
FROM workflow_definition WHERE code = 'SIMPLE_APPROVAL' AND version = 1;

INSERT INTO workflow_transition_def (definition_id, from_state, to_state, action_code, allowed_roles, requires_comment)
SELECT id, 'PENDING_APPROVAL', 'DRAFT', 'reject', N'OWNER,ADMIN', 1
FROM workflow_definition WHERE code = 'SIMPLE_APPROVAL' AND version = 1;
