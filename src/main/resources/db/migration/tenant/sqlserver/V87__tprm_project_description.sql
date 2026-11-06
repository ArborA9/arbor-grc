-- Optional TPRM project description (create form, same row as name).

IF COL_LENGTH('tprm_assessment', 'description') IS NULL
    ALTER TABLE tprm_assessment ADD description NVARCHAR(2000) NULL;
GO
