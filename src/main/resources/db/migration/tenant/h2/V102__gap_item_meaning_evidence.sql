-- What it means and evidence guidance on a gap control (copied from the framework, editable by tenant admin).

ALTER TABLE gap_item ADD COLUMN what_it_means CLOB;
ALTER TABLE gap_item ADD COLUMN evidence_guidance CLOB;
