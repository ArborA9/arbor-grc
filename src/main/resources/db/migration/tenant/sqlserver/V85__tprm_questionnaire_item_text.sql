-- TPRM questionnaires: admin-authored questions (snapshot text, optional library link).
-- SQL Server will not ALTER COLUMN while a unique constraint still sits on it (error 5074).

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'uq_tprm_control_code')
    ALTER TABLE tprm_control DROP CONSTRAINT uq_tprm_control_code;
GO

ALTER TABLE tprm_control ALTER COLUMN control_code VARCHAR(40) NOT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'uq_tprm_control_code')
    ALTER TABLE tprm_control ADD CONSTRAINT uq_tprm_control_code UNIQUE (control_code);
GO

IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'uq_tprm_qitem_questionnaire_control')
    ALTER TABLE tprm_questionnaire_item DROP CONSTRAINT uq_tprm_qitem_questionnaire_control;
GO

ALTER TABLE tprm_questionnaire_item ALTER COLUMN control_id BIGINT NULL;
GO

IF COL_LENGTH('tprm_questionnaire_item', 'control_code') IS NULL
    ALTER TABLE tprm_questionnaire_item ADD control_code VARCHAR(40) NULL;
GO

IF COL_LENGTH('tprm_questionnaire_item', 'domain') IS NULL
    ALTER TABLE tprm_questionnaire_item ADD domain VARCHAR(80) NULL;
GO

IF COL_LENGTH('tprm_questionnaire_item', 'requirement') IS NULL
    ALTER TABLE tprm_questionnaire_item ADD requirement VARCHAR(2000) NULL;
GO

IF COL_LENGTH('tprm_questionnaire_item', 'required_evidence') IS NULL
    ALTER TABLE tprm_questionnaire_item ADD required_evidence VARCHAR(2000) NULL;
GO

UPDATE i
SET i.control_code = c.control_code,
    i.domain = c.domain,
    i.requirement = c.requirement,
    i.required_evidence = c.required_evidence
FROM tprm_questionnaire_item i
INNER JOIN tprm_control c ON c.id = i.control_id
WHERE i.control_code IS NULL AND i.control_id IS NOT NULL;
GO

IF COL_LENGTH('tprm_assessment_item', 'control_code') IS NULL
    ALTER TABLE tprm_assessment_item ADD control_code VARCHAR(40) NULL;
GO

IF COL_LENGTH('tprm_assessment_item', 'domain') IS NULL
    ALTER TABLE tprm_assessment_item ADD domain VARCHAR(80) NULL;
GO

IF COL_LENGTH('tprm_assessment_item', 'requirement') IS NULL
    ALTER TABLE tprm_assessment_item ADD requirement VARCHAR(2000) NULL;
GO

IF COL_LENGTH('tprm_assessment_item', 'required_evidence') IS NULL
    ALTER TABLE tprm_assessment_item ADD required_evidence VARCHAR(2000) NULL;
GO
