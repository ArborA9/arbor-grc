-- Asset register (ISMS). Dispose only — never hard-delete.

CREATE TABLE asset (
    id                   BIGINT IDENTITY(1,1) PRIMARY KEY,
    name                 NVARCHAR(255) NOT NULL,
    asset_type           VARCHAR(64)   NOT NULL,
    status               VARCHAR(32)   NOT NULL CONSTRAINT DF_asset_status DEFAULT 'ACTIVE',
    description          NVARCHAR(MAX) NULL,
    owner_user_id        BIGINT        NULL,
    org_unit_id          BIGINT        NULL,
    parent_id            BIGINT        NULL,
    confidentiality      INT           NOT NULL,
    integrity_rating     INT           NOT NULL,
    availability         INT           NOT NULL,
    cia_score            DECIMAL(4, 2) NOT NULL,
    criticality          VARCHAR(32)   NOT NULL,
    classification       VARCHAR(32)   NOT NULL CONSTRAINT DF_asset_class DEFAULT 'CONFIDENTIAL',
    created_by_user_id   BIGINT        NOT NULL,
    disposed_at          DATETIME2     NULL,
    disposed_by_user_id  BIGINT        NULL,
    created_at           DATETIME2     NOT NULL CONSTRAINT DF_asset_created DEFAULT SYSUTCDATETIME(),
    updated_at           DATETIME2     NOT NULL CONSTRAINT DF_asset_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_asset_cia_c CHECK (confidentiality BETWEEN 1 AND 4),
    CONSTRAINT ck_asset_cia_i CHECK (integrity_rating BETWEEN 1 AND 4),
    CONSTRAINT ck_asset_cia_a CHECK (availability BETWEEN 1 AND 4),
    CONSTRAINT fk_asset_owner FOREIGN KEY (owner_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_asset_org FOREIGN KEY (org_unit_id) REFERENCES org_unit (id),
    CONSTRAINT fk_asset_parent FOREIGN KEY (parent_id) REFERENCES asset (id),
    CONSTRAINT fk_asset_created_by FOREIGN KEY (created_by_user_id) REFERENCES app_user (id),
    CONSTRAINT fk_asset_disposed_by FOREIGN KEY (disposed_by_user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_asset_status ON asset (status);
CREATE INDEX idx_asset_org ON asset (org_unit_id);
CREATE INDEX idx_asset_owner ON asset (owner_user_id);
CREATE INDEX idx_asset_type ON asset (asset_type);
