-- Typed Finding ID for a CAPA whose audit was not conducted in Arbor.

IF COL_LENGTH('capa', 'external_finding_code') IS NULL
BEGIN
    ALTER TABLE capa ADD external_finding_code NVARCHAR(80) NULL;
END
GO
