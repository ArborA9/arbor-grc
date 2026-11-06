-- What it means and evidence guidance on a gap control (copied from the framework, editable by tenant admin).

IF COL_LENGTH('gap_item', 'what_it_means') IS NULL
BEGIN
    ALTER TABLE gap_item ADD what_it_means NVARCHAR(MAX) NULL;
END
GO

IF COL_LENGTH('gap_item', 'evidence_guidance') IS NULL
BEGIN
    ALTER TABLE gap_item ADD evidence_guidance NVARCHAR(MAX) NULL;
END
GO
