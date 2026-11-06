-- Typed source when a manual CAPA is neither Internal nor External.

IF COL_LENGTH('capa', 'source_other') IS NULL
BEGIN
    ALTER TABLE capa ADD source_other NVARCHAR(120) NULL;
END
GO
