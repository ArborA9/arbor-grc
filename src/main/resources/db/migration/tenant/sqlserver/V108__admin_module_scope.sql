IF COL_LENGTH('app_user', 'admin_modules_set') IS NULL
BEGIN
    ALTER TABLE app_user ADD admin_modules_set BIT NOT NULL
        CONSTRAINT df_app_user_admin_modules_set DEFAULT 0;
END
GO

IF OBJECT_ID('user_admin_module', 'U') IS NULL
BEGIN
    CREATE TABLE user_admin_module (
        user_id     BIGINT        NOT NULL,
        module_code NVARCHAR(32)  NOT NULL,
        CONSTRAINT pk_user_admin_module PRIMARY KEY (user_id, module_code),
        CONSTRAINT fk_user_admin_module_user FOREIGN KEY (user_id) REFERENCES app_user (id)
    );
END
GO
