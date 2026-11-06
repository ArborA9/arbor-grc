-- IS project seed titles are "{node code} — {control title}". Control titles can already
-- be 512 chars; the prefix overflows VARCHAR(512). Widen so UAE IA (and similar) seed intact.
ALTER TABLE risk_record ALTER COLUMN title SET DATA TYPE VARCHAR(1024);
-- Assign / acceptance tasks copy "{displayId}: {risk title}" (displayId ≤32).
ALTER TABLE work_task ALTER COLUMN title SET DATA TYPE VARCHAR(1100);
