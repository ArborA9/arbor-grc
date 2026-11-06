CREATE TABLE app_user (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    email         NVARCHAR(255) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    full_name     NVARCHAR(255) NOT NULL,
    status        VARCHAR(32)   NOT NULL,
    created_at    DATETIME2     NOT NULL CONSTRAINT DF_app_user_created_at DEFAULT SYSUTCDATETIME()
);

CREATE TABLE user_role (
    user_id BIGINT      NOT NULL,
    role    VARCHAR(40) NOT NULL,
    CONSTRAINT PK_user_role PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);
