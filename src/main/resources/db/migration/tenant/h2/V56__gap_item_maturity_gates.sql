-- SAMA Gap: evidence-gate answers (M–Q) that drive maturity 0–5.
ALTER TABLE gap_item ADD COLUMN maturity_gates_json CLOB;
ALTER TABLE gap_item_submission ADD COLUMN maturity_gates_json CLOB;
