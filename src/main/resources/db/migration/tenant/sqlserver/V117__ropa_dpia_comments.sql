-- The DPO review keeps two comments apart: one for the DPIA Yes/No decision
-- (privacy_ropa.dpia_comments) and one for the Reviewed / Returned action
-- (the existing privacy_ropa.comments).

IF COL_LENGTH('privacy_ropa', 'dpia_comments') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD dpia_comments NVARCHAR(MAX) NULL;
END
GO
