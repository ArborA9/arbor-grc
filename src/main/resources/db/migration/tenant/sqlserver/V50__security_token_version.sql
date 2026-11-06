-- Security P1 — invalidate JWTs on disable / role change / password reset
ALTER TABLE app_user ADD token_version INT NOT NULL
    CONSTRAINT DF_app_user_token_version DEFAULT 0;
