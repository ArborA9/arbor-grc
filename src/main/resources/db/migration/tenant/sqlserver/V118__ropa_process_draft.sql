-- A Privacy Champion can save a processing activity as a draft at any point
-- and finish it later. Drafts cannot be submitted for DPO review.

IF COL_LENGTH('privacy_ropa', 'draft') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD draft BIT NULL;
END
GO
