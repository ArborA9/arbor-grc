-- Twin of H2 V75: IS seed progress on the project row.
IF COL_LENGTH('risk_project', 'seed_total') IS NULL
    ALTER TABLE risk_project ADD seed_total INT NOT NULL CONSTRAINT df_rp_seed_total DEFAULT 0;
IF COL_LENGTH('risk_project', 'seed_status') IS NULL
    ALTER TABLE risk_project ADD seed_status VARCHAR(16) NOT NULL CONSTRAINT df_rp_seed_status DEFAULT 'DONE';
