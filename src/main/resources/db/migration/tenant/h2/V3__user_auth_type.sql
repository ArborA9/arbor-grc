-- Identity from AD (SSO) vs local break-glass accounts. Roles are always managed in Arbor.
ALTER TABLE app_user ADD COLUMN auth_type VARCHAR(16) NOT NULL DEFAULT 'LOCAL';

-- SSO users have no password stored in Arbor.
ALTER TABLE app_user ALTER COLUMN password_hash SET NULL;
