-- Process owner is a tenant user, not an organisation unit.
-- GO: SQL Server cannot FK/INDEX a column added in the same batch.

ALTER TABLE bia_process ADD owner_user_id BIGINT NULL;
GO

ALTER TABLE bia_process ADD CONSTRAINT fk_bia_process_owner_user
    FOREIGN KEY (owner_user_id) REFERENCES app_user (id);
CREATE INDEX idx_bia_process_owner_user ON bia_process (owner_user_id);
