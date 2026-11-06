-- Gap optional answers for A2 questions (tenant).

CREATE TABLE gap_item_answer (
    id                      BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    gap_item_id             BIGINT        NOT NULL,
    framework_question_id   BIGINT        NOT NULL,
    question_version        INT           NOT NULL,
    answer                  VARCHAR(32)   NULL,
    note                    NVARCHAR(MAX) NULL,
    updated_by_user_id      BIGINT        NULL,
    updated_at              DATETIME2     NOT NULL CONSTRAINT DF_gia_upd DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_gia_item FOREIGN KEY (gap_item_id) REFERENCES gap_item (id) ON DELETE CASCADE,
    CONSTRAINT fk_gia_user FOREIGN KEY (updated_by_user_id) REFERENCES app_user (id),
    CONSTRAINT uq_gia_item_q UNIQUE (gap_item_id, framework_question_id)
);

CREATE INDEX idx_gia_item ON gap_item_answer (gap_item_id);
