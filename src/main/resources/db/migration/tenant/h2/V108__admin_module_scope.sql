ALTER TABLE app_user ADD COLUMN admin_modules_set BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE user_admin_module (
    user_id     BIGINT       NOT NULL,
    module_code VARCHAR(32)  NOT NULL,
    PRIMARY KEY (user_id, module_code),
    CONSTRAINT fk_user_admin_module_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);
