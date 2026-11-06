-- Control-level comments when the Contributor has no questions assigned.

ALTER TABLE gap_item ADD contributor_comments NVARCHAR(MAX) NULL;
ALTER TABLE gap_item_submission ADD contributor_comments NVARCHAR(MAX) NULL;
