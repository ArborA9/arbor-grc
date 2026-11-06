-- DPIA journey from scratch (DPIA Journey.docx + Questionnaire v3 + DPO Assessment).

CREATE TABLE dpia_questionnaire (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name                NVARCHAR(255) NOT NULL,
    description         NVARCHAR(2000) NULL,
    system_default      BIT NOT NULL CONSTRAINT DF_dpia_q_system_default DEFAULT 0,
    active              BIT NOT NULL CONSTRAINT DF_dpia_q_active DEFAULT 1,
    created_by_user_id  BIGINT NOT NULL,
    created_at          DATETIME2 NOT NULL CONSTRAINT DF_dpia_q_created DEFAULT SYSUTCDATETIME(),
    updated_at          DATETIME2 NOT NULL CONSTRAINT DF_dpia_q_updated DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE dpia_questionnaire_question (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    questionnaire_id    BIGINT NOT NULL,
    code                NVARCHAR(16) NOT NULL,
    section_name        NVARCHAR(255) NOT NULL,
    principle           NVARCHAR(255) NOT NULL,
    question_text       NVARCHAR(MAX) NOT NULL,
    guidance            NVARCHAR(MAX) NULL,
    response_type       NVARCHAR(64) NOT NULL,
    options_json        NVARCHAR(MAX) NULL,
    sort_order          INT NOT NULL CONSTRAINT DF_dpia_qq_sort DEFAULT 0,
    required_flag       BIT NOT NULL CONSTRAINT DF_dpia_qq_req DEFAULT 1
);
GO

CREATE INDEX idx_dpia_q_questionnaire ON dpia_questionnaire_question (questionnaire_id);
GO

CREATE TABLE dpia_project (
    id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name                    NVARCHAR(255) NOT NULL,
    assessment_year         INT NULL,
    status                  NVARCHAR(32) NOT NULL CONSTRAINT DF_dpia_proj_status DEFAULT N'DRAFT',
    linked_ropa_project_id  BIGINT NULL,
    created_by_user_id      BIGINT NOT NULL,
    created_at              DATETIME2 NOT NULL CONSTRAINT DF_dpia_proj_created DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2 NOT NULL CONSTRAINT DF_dpia_proj_updated DEFAULT SYSUTCDATETIME()
);
GO

CREATE TABLE dpia_department (
    id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    dpia_project_id         BIGINT NOT NULL,
    org_unit_id             BIGINT NOT NULL,
    reason_description      NVARCHAR(4000) NULL,
    questionnaire_id        BIGINT NULL,
    champion_user_id        BIGINT NULL,
    department_head_user_id BIGINT NULL,
    due_date                DATE NULL,
    status                  NVARCHAR(32) NOT NULL CONSTRAINT DF_dpia_dept_status DEFAULT N'NOT_STARTED',
    return_from             NVARCHAR(16) NULL,
    return_reason           NVARCHAR(4000) NULL,
    work_task_id            BIGINT NULL,
    head_work_task_id       BIGINT NULL,
    head_notes              NVARCHAR(4000) NULL,
    head_decided_at         DATETIME2 NULL,
    head_outcome            NVARCHAR(32) NULL CONSTRAINT DF_dpia_dept_head_out DEFAULT N'PENDING',
    dpo_accepted_at         DATETIME2 NULL,
    dpo_accepted_by_user_id BIGINT NULL,
    assessment_complete     BIT NOT NULL CONSTRAINT DF_dpia_dept_assess DEFAULT 0,
    closed_at               DATETIME2 NULL,
    closed_by_user_id       BIGINT NULL,
    created_by_user_id      BIGINT NOT NULL,
    created_at              DATETIME2 NOT NULL CONSTRAINT DF_dpia_dept_created DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2 NOT NULL CONSTRAINT DF_dpia_dept_updated DEFAULT SYSUTCDATETIME()
);
GO

CREATE INDEX idx_dpia_dept_project ON dpia_department (dpia_project_id);
GO
CREATE INDEX idx_dpia_dept_champion ON dpia_department (champion_user_id);
GO
CREATE INDEX idx_dpia_dept_head ON dpia_department (department_head_user_id);
GO

CREATE TABLE dpia_instance_question (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    dpia_department_id  BIGINT NOT NULL,
    code                NVARCHAR(16) NOT NULL,
    section_name        NVARCHAR(255) NOT NULL,
    principle           NVARCHAR(255) NOT NULL,
    question_text       NVARCHAR(MAX) NOT NULL,
    guidance            NVARCHAR(MAX) NULL,
    response_type       NVARCHAR(64) NOT NULL,
    options_json        NVARCHAR(MAX) NULL,
    sort_order          INT NOT NULL CONSTRAINT DF_dpia_iq_sort DEFAULT 0,
    required_flag       BIT NOT NULL CONSTRAINT DF_dpia_iq_req DEFAULT 1,
    answer_text         NVARCHAR(MAX) NULL,
    answer_json         NVARCHAR(MAX) NULL,
    details_evidence    NVARCHAR(MAX) NULL
);
GO

CREATE INDEX idx_dpia_inst_dept ON dpia_instance_question (dpia_department_id);
GO

CREATE TABLE dpia_dpo_assessment_row (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    dpia_department_id  BIGINT NOT NULL,
    sort_order          INT NOT NULL,
    principle           NVARCHAR(255) NOT NULL,
    description_text    NVARCHAR(MAX) NOT NULL,
    compliance_status   NVARCHAR(64) NULL,
    remarks             NVARCHAR(MAX) NULL,
    recommended_measures NVARCHAR(MAX) NULL,
    risk_title          NVARCHAR(500) NULL
);
GO

CREATE INDEX idx_dpia_dpo_dept ON dpia_dpo_assessment_row (dpia_department_id);
GO
