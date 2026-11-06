-- BIA 2026 pack rebuild. Replaces typed-RTO bia rows with cycle + process + impact + MOR.

IF OBJECT_ID('fk_rpp_bia', 'F') IS NOT NULL
    ALTER TABLE recovery_plan_procedure DROP CONSTRAINT fk_rpp_bia;
IF OBJECT_ID('fk_rpb_bia', 'F') IS NOT NULL
    ALTER TABLE recovery_plan_bia DROP CONSTRAINT fk_rpb_bia;
DELETE FROM recovery_plan_bia;
UPDATE recovery_plan_procedure SET bia_id = NULL;
IF OBJECT_ID('bia', 'U') IS NOT NULL DROP TABLE bia;

CREATE TABLE bia_cycle (
    id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    org_unit_id             BIGINT       NOT NULL,
    display_id              NVARCHAR(64) NOT NULL,
    issue_date              DATE         NULL,
    next_review_date        DATE         NULL,
    version_label           NVARCHAR(32) NOT NULL CONSTRAINT DF_bia_cycle_ver DEFAULT N'1.0',
    objective               NVARCHAR(4000) NULL,
    scope_text              NVARCHAR(4000) NULL,
    mbco                    NVARCHAR(4000) NULL,
    status                  NVARCHAR(32) NOT NULL CONSTRAINT DF_bia_cycle_status DEFAULT N'DRAFT',
    assigned_to_user_id     BIGINT       NULL,
    due_date                DATE         NULL,
    work_task_id            BIGINT       NULL,
    return_reason           NVARCHAR(4000) NULL,
    review_due_notified_at  DATETIME2    NULL,
    not_submitted_notified_at DATETIME2  NULL,
    approved_at             DATETIME2    NULL,
    approved_by_user_id     BIGINT       NULL,
    created_by_user_id      BIGINT       NOT NULL,
    created_at              DATETIME2    NOT NULL CONSTRAINT DF_bia_cycle_created DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2    NOT NULL CONSTRAINT DF_bia_cycle_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_bia_cycle_display UNIQUE (display_id),
    CONSTRAINT fk_bia_cycle_org FOREIGN KEY (org_unit_id) REFERENCES org_unit (id)
);

CREATE INDEX idx_bia_cycle_status ON bia_cycle (status);
CREATE INDEX idx_bia_cycle_org ON bia_cycle (org_unit_id);
CREATE INDEX idx_bia_cycle_assigned ON bia_cycle (assigned_to_user_id);

CREATE TABLE bia_process (
    id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    cycle_id                BIGINT       NOT NULL,
    sort_order              INT          NOT NULL CONSTRAINT DF_bia_proc_sort DEFAULT 0,
    process_code            NVARCHAR(64) NOT NULL,
    process_name            NVARCHAR(255) NOT NULL,
    description             NVARCHAR(4000) NULL,
    flow_trigger            NVARCHAR(4000) NULL,
    flow_steps              NVARCHAR(4000) NULL,
    flow_output             NVARCHAR(4000) NULL,
    third_party_activities  NVARCHAR(4000) NULL,
    supplier_name           NVARCHAR(255) NULL,
    owner_org_unit_id       BIGINT       NULL,
    frequency               NVARCHAR(64) NULL,
    peak_period             NVARCHAR(255) NULL,
    dep_staff               NVARCHAR(4000) NULL,
    dep_premises            NVARCHAR(4000) NULL,
    dep_it                  NVARCHAR(4000) NULL,
    dep_equipment           NVARCHAR(4000) NULL,
    dep_records             NVARCHAR(4000) NULL,
    dep_third_party         NVARCHAR(4000) NULL,
    related_risk_code       NVARCHAR(64) NULL,
    champion_notes          NVARCHAR(4000) NULL,
    peak_score              INT          NULL,
    criticality_tier        INT          NULL,
    category_label          NVARCHAR(64) NULL,
    rto_hours               INT          NULL,
    mao_hours               INT          NULL,
    recovery_priority       INT          NULL,
    critical_rank           INT          NULL,
    mor_required            BIT          NOT NULL CONSTRAINT DF_bia_proc_mor DEFAULT 0,
    rto_caption             NVARCHAR(500) NULL,
    completeness_ok         BIT          NOT NULL CONSTRAINT DF_bia_proc_comp DEFAULT 0,
    completeness_label      NVARCHAR(64) NOT NULL CONSTRAINT DF_bia_proc_clab DEFAULT N'Incomplete',
    review_outcome          NVARCHAR(32) NOT NULL CONSTRAINT DF_bia_proc_rev DEFAULT N'PENDING_REVIEW',
    submitted_by_user_id    BIGINT       NULL,
    submitted_at            DATETIME2    NULL,
    reviewer_user_id        BIGINT       NULL,
    reviewer_name           NVARCHAR(255) NULL,
    review_date             DATE         NULL,
    review_comment          NVARCHAR(4000) NULL,
    created_at              DATETIME2    NOT NULL CONSTRAINT DF_bia_proc_created DEFAULT SYSUTCDATETIME(),
    updated_at              DATETIME2    NOT NULL CONSTRAINT DF_bia_proc_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_bia_process_code UNIQUE (cycle_id, process_code),
    CONSTRAINT fk_bia_process_cycle FOREIGN KEY (cycle_id) REFERENCES bia_cycle (id) ON DELETE CASCADE,
    CONSTRAINT fk_bia_process_owner FOREIGN KEY (owner_org_unit_id) REFERENCES org_unit (id)
);

CREATE INDEX idx_bia_process_cycle ON bia_process (cycle_id);
CREATE INDEX idx_bia_process_tier ON bia_process (criticality_tier);

CREATE TABLE bia_impact_cell (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    process_id   BIGINT       NOT NULL,
    category     NVARCHAR(32) NOT NULL,
    band         NVARCHAR(16) NOT NULL,
    rating       INT          NULL,
    remarks      NVARCHAR(2000) NULL,
    CONSTRAINT uq_bia_impact UNIQUE (process_id, category, band),
    CONSTRAINT fk_bia_impact_process FOREIGN KEY (process_id) REFERENCES bia_process (id) ON DELETE CASCADE
);

CREATE INDEX idx_bia_impact_process ON bia_impact_cell (process_id);

CREATE TABLE bia_mor (
    id                   BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    process_id           BIGINT NOT NULL,
    capacity_pct         INT    NULL,
    min_staff            INT    NULL,
    people               NVARCHAR(4000) NULL,
    technology           NVARCHAR(4000) NULL,
    premises             NVARCHAR(4000) NULL,
    equipment            NVARCHAR(4000) NULL,
    records_text         NVARCHAR(4000) NULL,
    third_party_support  NVARCHAR(4000) NULL,
    recovery_strategy    NVARCHAR(255) NULL,
    workaround           NVARCHAR(4000) NULL,
    CONSTRAINT uq_bia_mor_process UNIQUE (process_id),
    CONSTRAINT fk_bia_mor_process FOREIGN KEY (process_id) REFERENCES bia_process (id) ON DELETE CASCADE
);

ALTER TABLE recovery_plan_bia
    ADD CONSTRAINT fk_rpb_bia FOREIGN KEY (bia_id) REFERENCES bia_process (id);
ALTER TABLE recovery_plan_procedure
    ADD CONSTRAINT fk_rpp_bia FOREIGN KEY (bia_id) REFERENCES bia_process (id);
