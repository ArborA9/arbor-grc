package com.a9.arbor.grc.config;

import com.a9.arbor.grc.tenancy.SqlServerDatabaseCreator;

/**
 * Ensures the shared catalog database physically exists before Flyway migrates it.
 * No-op for H2 (in-memory databases are created on first connection).
 */
public class CatalogDatabaseInitializer {

    public CatalogDatabaseInitializer(ArborTenancyProperties properties) {
        if (properties.isCreateDatabase()) {
            SqlServerDatabaseCreator.createIfNotExists(
                    properties.getMasterUrl(),
                    properties.getUsername(),
                    properties.getPassword(),
                    properties.getCatalogDatabase(),
                    properties.getElasticPoolName(),
                    properties.getBackupStorageRedundancy());
        }
    }
}
