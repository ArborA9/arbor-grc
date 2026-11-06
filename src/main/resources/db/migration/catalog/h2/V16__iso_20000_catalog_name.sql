-- Catalog title for ISO 20000. New imports use FrameworkTypeCode; this renames rows already stored.
UPDATE framework SET name = 'ISO 20000' WHERE code = 'ISO_20000';
