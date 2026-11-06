-- Why a refresh token was revoked. ROTATED is replayed as theft after a short grace.
-- LOGOUT and admin revoke are not theft.

IF COL_LENGTH('refresh_token', 'revoked_reason') IS NULL
BEGIN
    ALTER TABLE refresh_token ADD revoked_reason NVARCHAR(16) NULL;
END
GO
