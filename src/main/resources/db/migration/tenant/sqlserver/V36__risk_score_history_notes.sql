-- Risk Wave 3: §13.7 scoring history + stage notes — SQL Server twin.

CREATE TABLE risk_score_history (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    risk_id                 BIGINT       NOT NULL,
    probability             NVARCHAR(16) NOT NULL,
    impact                  NVARCHAR(16) NOT NULL,
    control_effectiveness   NVARCHAR(16) NOT NULL,
    inherent_risk           NVARCHAR(16) NOT NULL,
    residual_risk           NVARCHAR(16) NOT NULL,
    reason                  NVARCHAR(1000) NULL,
    changed_by_user_id      BIGINT       NOT NULL,
    recorded_at             DATETIME2    NOT NULL CONSTRAINT df_rsh_recorded DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_rsh_risk FOREIGN KEY (risk_id) REFERENCES risk_record (id)
);

CREATE INDEX idx_rsh_risk_recorded ON risk_score_history (risk_id, recorded_at);

CREATE TABLE risk_stage_note (
    id                      BIGINT IDENTITY(1,1) PRIMARY KEY,
    risk_id                 BIGINT       NOT NULL,
    workflow_stage          NVARCHAR(32) NOT NULL,
    body                    NVARCHAR(MAX) NOT NULL,
    internal_only           BIT          NOT NULL CONSTRAINT df_rsn_internal DEFAULT 0,
    created_by_user_id      BIGINT       NOT NULL,
    created_at              DATETIME2    NOT NULL CONSTRAINT df_rsn_created DEFAULT SYSUTCDATETIME(),
    deleted_at              DATETIME2    NULL,
    deleted_by_user_id      BIGINT       NULL,
    CONSTRAINT fk_rsn_risk FOREIGN KEY (risk_id) REFERENCES risk_record (id)
);

CREATE INDEX idx_rsn_risk_created ON risk_stage_note (risk_id, created_at);
