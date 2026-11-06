-- Organisation structure + guided-setup profile (Platform always-on).
-- Level 0 = tenant (catalog name). Levels 1–4 are renameable unit tiers.

CREATE TABLE org_level_config (
    level_num   INT           NOT NULL PRIMARY KEY,
    label       NVARCHAR(64)  NOT NULL,
    updated_at  DATETIME2     NOT NULL CONSTRAINT DF_olc_updated DEFAULT SYSUTCDATETIME()
);

INSERT INTO org_level_config (level_num, label) VALUES
    (1, N'Sector'),
    (2, N'Division / Department'),
    (3, N'Section'),
    (4, N'Team / Sub-section');

CREATE TABLE org_profile (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    display_name         NVARCHAR(200) NULL,
    industry             NVARCHAR(100) NULL,
    size_band            NVARCHAR(64)  NULL,
    timezone             NVARCHAR(64)  NOT NULL CONSTRAINT DF_op_tz DEFAULT N'Asia/Dubai',
    date_format          NVARCHAR(32)  NOT NULL CONSTRAINT DF_op_df DEFAULT N'DD/MM/YYYY',
    wizard_completed_at  DATETIME2     NULL,
    updated_at           DATETIME2     NOT NULL CONSTRAINT DF_op_updated DEFAULT SYSUTCDATETIME()
);

INSERT INTO org_profile (display_name, industry, size_band, timezone, date_format)
VALUES (NULL, NULL, NULL, N'Asia/Dubai', N'DD/MM/YYYY');

CREATE TABLE org_unit (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    name          NVARCHAR(200) NOT NULL,
    level_num     INT           NOT NULL,
    parent_id     BIGINT        NULL,
    head_user_id  BIGINT        NULL,
    active        BIT           NOT NULL CONSTRAINT DF_ou_active DEFAULT 1,
    created_at    DATETIME2     NOT NULL CONSTRAINT DF_ou_created DEFAULT SYSUTCDATETIME(),
    updated_at    DATETIME2     NOT NULL CONSTRAINT DF_ou_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_org_unit_level CHECK (level_num BETWEEN 1 AND 4),
    CONSTRAINT fk_org_unit_parent FOREIGN KEY (parent_id) REFERENCES org_unit (id),
    CONSTRAINT fk_org_unit_head FOREIGN KEY (head_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_org_unit_parent ON org_unit (parent_id);
CREATE INDEX idx_org_unit_level ON org_unit (level_num);
CREATE INDEX idx_org_unit_active ON org_unit (active);
