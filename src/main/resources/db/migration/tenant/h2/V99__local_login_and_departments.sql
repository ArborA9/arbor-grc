-- Local login flag (SSO and password can share one user) and department source.

ALTER TABLE app_user ADD COLUMN local_login_enabled BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE app_user SET local_login_enabled = TRUE
WHERE auth_type = 'LOCAL' AND password_hash IS NOT NULL;

ALTER TABLE refresh_token ADD COLUMN auth_method VARCHAR(16);

ALTER TABLE org_unit ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'MANUAL';

UPDATE org_level_config SET label = 'Department' WHERE level_num = 1 AND label = 'Sector';
UPDATE org_level_config SET label = 'Division' WHERE level_num = 2 AND label = 'Division / Department';
