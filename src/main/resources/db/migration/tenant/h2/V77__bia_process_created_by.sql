-- Track who added a BIA process so Champions cannot delete Admin-created rows.

ALTER TABLE bia_process ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT;
ALTER TABLE bia_process ADD COLUMN IF NOT EXISTS created_by_admin BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_bia_process_created_by ON bia_process (created_by_user_id);
