-- Doc 03 D3-12: UAE IA Gap create can seed L3 controls or L4 sub-controls.

ALTER TABLE gap_assessment ADD COLUMN assessment_depth INT NOT NULL DEFAULT 4;
