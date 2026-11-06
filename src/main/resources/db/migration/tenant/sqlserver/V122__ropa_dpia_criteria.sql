-- Arbor DPO review: four DPIA-trigger criteria; DPIA required is derived (any Yes).

IF COL_LENGTH('privacy_ropa', 'dpia_crit_large_sensitive') IS NULL
    ALTER TABLE privacy_ropa ADD dpia_crit_large_sensitive BIT NULL;
GO
IF COL_LENGTH('privacy_ropa', 'dpia_crit_systematic_assessment') IS NULL
    ALTER TABLE privacy_ropa ADD dpia_crit_systematic_assessment BIT NULL;
GO
IF COL_LENGTH('privacy_ropa', 'dpia_crit_automated_profiling') IS NULL
    ALTER TABLE privacy_ropa ADD dpia_crit_automated_profiling BIT NULL;
GO
IF COL_LENGTH('privacy_ropa', 'dpia_crit_legal_serious_impact') IS NULL
    ALTER TABLE privacy_ropa ADD dpia_crit_legal_serious_impact BIT NULL;
GO
