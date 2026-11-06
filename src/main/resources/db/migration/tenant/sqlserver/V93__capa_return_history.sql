-- Append-only CAPA return (reject) history for Admin / Contributor review.
IF OBJECT_ID('capa_return_event', 'U') IS NULL
BEGIN
    CREATE TABLE capa_return_event (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        capa_id BIGINT NOT NULL,
        rejected_at DATETIME2 NOT NULL,
        rejected_by_user_id BIGINT NOT NULL,
        reason NVARCHAR(MAX) NOT NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'idx_capa_return_event_capa' AND object_id = OBJECT_ID('capa_return_event'))
BEGIN
    CREATE INDEX idx_capa_return_event_capa ON capa_return_event (capa_id);
END;
