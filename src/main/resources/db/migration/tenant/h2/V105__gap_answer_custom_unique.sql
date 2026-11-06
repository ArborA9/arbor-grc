-- Custom-question answers store a null framework question id. The old unique pair
-- treated those nulls as the same key, so a second custom answer could not be saved.

ALTER TABLE gap_item_answer DROP CONSTRAINT IF EXISTS uq_gia_item_q;

ALTER TABLE gap_item_answer ADD CONSTRAINT uq_gia_item_fw
    UNIQUE NULLS DISTINCT (gap_item_id, framework_question_id);

ALTER TABLE gap_item_answer ADD CONSTRAINT uq_gia_item_custom
    UNIQUE NULLS DISTINCT (gap_item_id, custom_question_id);
