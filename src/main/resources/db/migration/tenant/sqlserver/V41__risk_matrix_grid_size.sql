ALTER TABLE risk_matrix_config ADD grid_size INT NOT NULL CONSTRAINT DF_risk_matrix_config_grid_size DEFAULT 5;

ALTER TABLE risk_record ADD matrix_grid_size INT NULL;
