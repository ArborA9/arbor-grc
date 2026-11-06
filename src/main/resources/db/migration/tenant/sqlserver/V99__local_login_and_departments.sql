-- Local login flag (SSO and password can share one user) and department source.
-- GO between ADD and later UPDATE (Azure SQL error 207).

IF COL_LENGTH('app_user', 'local_login_enabled') IS NULL
BEGIN
    ALTER TABLE app_user ADD local_login_enabled BIT NOT NULL
        CONSTRAINT DF_app_user_local_login_enabled DEFAULT 0;
END
GO

UPDATE app_user SET local_login_enabled = 1
WHERE auth_type = N'LOCAL' AND password_hash IS NOT NULL;
GO

IF COL_LENGTH('refresh_token', 'auth_method') IS NULL
BEGIN
    ALTER TABLE refresh_token ADD auth_method NVARCHAR(16) NULL;
END
GO

IF COL_LENGTH('org_unit', 'source') IS NULL
BEGIN
    ALTER TABLE org_unit ADD source NVARCHAR(16) NOT NULL
        CONSTRAINT DF_org_unit_source DEFAULT N'MANUAL';
END
GO

UPDATE org_level_config SET label = N'Department' WHERE level_num = 1 AND label = N'Sector';
GO
UPDATE org_level_config SET label = N'Division' WHERE level_num = 2 AND label = N'Division / Department';
GO
