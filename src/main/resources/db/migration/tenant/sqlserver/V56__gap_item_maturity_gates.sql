-- SAMA Gap: evidence-gate answers (M–Q) that drive maturity 0–5.
ALTER TABLE gap_item ADD maturity_gates_json NVARCHAR(MAX) NULL;
ALTER TABLE gap_item_submission ADD maturity_gates_json NVARCHAR(MAX) NULL;
