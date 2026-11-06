-- Identity from AD (SSO) vs local break-glass accounts. Roles are always managed in Arbor.
ALTER TABLE app_user ADD auth_type VARCHAR(16) NOT NULL
    CONSTRAINT DF_app_user_auth_type DEFAULT 'LOCAL';

-- SSO users have no password stored in Arbor.
ALTER TABLE app_user ALTER COLUMN password_hash NVARCHAR(255) NULL;
