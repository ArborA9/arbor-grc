-- Security P1 — invalidate JWTs on disable / role change / password reset
ALTER TABLE app_user ADD COLUMN token_version INT NOT NULL DEFAULT 0;
