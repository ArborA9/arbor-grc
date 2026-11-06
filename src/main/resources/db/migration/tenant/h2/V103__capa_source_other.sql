-- Typed source when a manual CAPA is neither Internal nor External.

ALTER TABLE capa ADD COLUMN source_other VARCHAR(120);
