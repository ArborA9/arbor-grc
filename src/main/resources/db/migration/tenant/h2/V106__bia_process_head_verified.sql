-- Business Continuity Head verifies each process before Approve.
-- Separate from Admin specialist review.

ALTER TABLE bia_process ADD COLUMN IF NOT EXISTS head_verified BOOLEAN NOT NULL DEFAULT FALSE;
