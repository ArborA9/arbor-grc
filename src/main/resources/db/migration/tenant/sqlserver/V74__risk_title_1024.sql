-- Twin of H2 V74: IS seed "{node code} — {control title}" can exceed 512.
ALTER TABLE risk_record ALTER COLUMN title NVARCHAR(1024) NOT NULL;
ALTER TABLE work_task ALTER COLUMN title NVARCHAR(1100) NOT NULL;
