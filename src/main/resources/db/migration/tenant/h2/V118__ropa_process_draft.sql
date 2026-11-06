-- A Privacy Champion can save a processing activity as a draft at any point
-- and finish it later. Drafts cannot be submitted for DPO review.

ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS draft BOOLEAN DEFAULT FALSE;
