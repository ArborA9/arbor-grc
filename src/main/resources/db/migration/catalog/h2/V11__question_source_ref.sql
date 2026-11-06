-- F3 of the framework Excel redesign: per-question provenance for the reference packs.
--
-- A pack row can be authored deeper than the Master hierarchy goes (ISO 22301 "4.3" when the catalog
-- stops at "4", ISO 42001 "A.4.1" when the catalog has no "A.4"). Those questions attach to the
-- nearest existing ancestor, so control_code becomes the ancestor's code and the ref the client
-- actually wrote would otherwise be lost — making the later "create the finer nodes" pass guesswork.
--
-- Null for every Master-imported question, where control_code is already the authored ref.
ALTER TABLE framework_question ADD COLUMN source_ref VARCHAR(64);

CREATE INDEX idx_fq_source_ref ON framework_question (framework_id, source_ref);
