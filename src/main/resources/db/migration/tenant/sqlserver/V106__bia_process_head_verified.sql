-- Business Continuity Head verifies each process before Approve.
-- Separate from Admin specialist review.
-- Azure SQL: GO after ADD before any later use of the column (error 207).

IF COL_LENGTH('bia_process', 'head_verified') IS NULL
BEGIN
    ALTER TABLE bia_process ADD head_verified BIT NOT NULL CONSTRAINT df_bia_process_head_verified DEFAULT 0;
END
GO
