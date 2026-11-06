-- Control-level comments when the Contributor has no questions assigned.

ALTER TABLE gap_item ADD COLUMN contributor_comments CLOB;
ALTER TABLE gap_item_submission ADD COLUMN contributor_comments CLOB;
