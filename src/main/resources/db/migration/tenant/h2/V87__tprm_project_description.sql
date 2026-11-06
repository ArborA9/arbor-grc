-- Optional TPRM project description (create form, same row as name).

ALTER TABLE tprm_assessment ADD COLUMN IF NOT EXISTS description VARCHAR(2000);
