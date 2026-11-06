ALTER TABLE risk_matrix_config ADD COLUMN grid_size INT NOT NULL DEFAULT 5;

ALTER TABLE risk_record ADD COLUMN matrix_grid_size INT NULL;
