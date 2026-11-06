-- The DPO review keeps two comments apart: one for the DPIA Yes/No decision
-- (privacy_ropa.dpia_comments) and one for the Reviewed / Returned action
-- (the existing privacy_ropa.comments).

ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS dpia_comments CLOB;
