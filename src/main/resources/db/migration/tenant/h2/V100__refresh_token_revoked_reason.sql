-- Why a refresh token was revoked. ROTATED is replayed as theft after a short grace.
-- LOGOUT and admin revoke are not theft.

ALTER TABLE refresh_token ADD COLUMN revoked_reason VARCHAR(16);
