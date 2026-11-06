-- Framework Import V2 Phase 6C: freeze definition on Gap/SoA seed (in-progress isolation).
ALTER TABLE gap_item ADD definition_text NVARCHAR(MAX) NULL;
ALTER TABLE soa_item ADD definition_text NVARCHAR(MAX) NULL;
GO
