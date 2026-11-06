-- Arbor DPO review: four DPIA-trigger criteria; DPIA required is derived (any Yes).

ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS dpia_crit_large_sensitive BOOLEAN;
ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS dpia_crit_systematic_assessment BOOLEAN;
ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS dpia_crit_automated_profiling BOOLEAN;
ALTER TABLE privacy_ropa ADD COLUMN IF NOT EXISTS dpia_crit_legal_serious_impact BOOLEAN;
