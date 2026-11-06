-- Department Head per-process decision on each ROPA processing activity
-- (approve / return individual processes, BIA-style head verification).

ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS head_outcome VARCHAR(32) DEFAULT 'PENDING';
ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS head_comments CLOB;
ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS head_decided_by_user_id BIGINT;
ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS head_decided_at TIMESTAMP;
