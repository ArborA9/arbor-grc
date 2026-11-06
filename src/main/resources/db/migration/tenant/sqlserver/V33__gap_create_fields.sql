-- Gap create header fields (§11.5): scope + assessor; target_date already exists. SQL Server twin.

ALTER TABLE gap_assessment ADD scope NVARCHAR(MAX) NULL;
ALTER TABLE gap_assessment ADD assessor_user_id BIGINT NULL;
GO

ALTER TABLE gap_assessment
    ADD CONSTRAINT fk_gap_assessor FOREIGN KEY (assessor_user_id) REFERENCES app_user (id);

CREATE INDEX idx_gap_assessor ON gap_assessment (assessor_user_id);
