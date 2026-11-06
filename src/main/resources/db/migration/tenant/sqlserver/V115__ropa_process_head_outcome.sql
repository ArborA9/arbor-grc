-- Department Head per-process decision on each ROPA processing activity
-- (approve / return individual processes, BIA-style head verification).

IF COL_LENGTH('privacy_ropa', 'head_outcome') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD head_outcome NVARCHAR(32) NULL;
END
GO
IF COL_LENGTH('privacy_ropa', 'head_comments') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD head_comments NVARCHAR(MAX) NULL;
END
GO
IF COL_LENGTH('privacy_ropa', 'head_decided_by_user_id') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD head_decided_by_user_id BIGINT NULL;
END
GO
IF COL_LENGTH('privacy_ropa', 'head_decided_at') IS NULL
BEGIN
    ALTER TABLE privacy_ropa ADD head_decided_at DATETIME2 NULL;
END
GO
