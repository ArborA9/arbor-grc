-- Typed Finding ID for a CAPA whose audit was not conducted in Arbor.

ALTER TABLE capa ADD COLUMN external_finding_code VARCHAR(80);
