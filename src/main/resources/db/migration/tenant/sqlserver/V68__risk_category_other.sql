-- Doc 03 D3-01: free-text when risk category is OTHER.

ALTER TABLE risk_record ADD category_other VARCHAR(128) NULL;
