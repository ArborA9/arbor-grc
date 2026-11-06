-- Custom-question answers store a null framework question id. A plain unique pair
-- allows only one such null, so a second custom answer cannot be saved.

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'uq_gia_item_q')
    ALTER TABLE gap_item_answer DROP CONSTRAINT uq_gia_item_q;
GO

IF EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'uq_gia_item_q' AND object_id = OBJECT_ID(N'gap_item_answer')
)
    DROP INDEX uq_gia_item_q ON gap_item_answer;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'uq_gia_item_fw' AND object_id = OBJECT_ID(N'gap_item_answer')
)
    CREATE UNIQUE INDEX uq_gia_item_fw
        ON gap_item_answer (gap_item_id, framework_question_id)
        WHERE framework_question_id IS NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'uq_gia_item_custom' AND object_id = OBJECT_ID(N'gap_item_answer')
)
    CREATE UNIQUE INDEX uq_gia_item_custom
        ON gap_item_answer (gap_item_id, custom_question_id)
        WHERE custom_question_id IS NOT NULL;
GO
