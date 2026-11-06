-- Framework Import V2 Phase 6C: freeze definition on Gap seed (in-progress isolation).
ALTER TABLE gap_item ADD COLUMN definition_text CLOB;
ALTER TABLE soa_item ADD COLUMN definition_text CLOB;
