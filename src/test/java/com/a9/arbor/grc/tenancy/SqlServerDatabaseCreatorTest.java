package com.a9.arbor.grc.tenancy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlServerDatabaseCreatorTest {

    @Test
    void plainCreateWithoutPoolOrRedundancy() {
        assertEquals("CREATE DATABASE [arbor_tenant_a]",
                SqlServerDatabaseCreator.createStatement("arbor_tenant_a", null, null));
    }

    @Test
    void poolWithoutRedundancyKeepsAzureDefault() {
        assertEquals("CREATE DATABASE [arbor_tenant_a] ( SERVICE_OBJECTIVE = ELASTIC_POOL ( name = [arbor-sqlpool-prod] ) )",
                SqlServerDatabaseCreator.createStatement("arbor_tenant_a", "arbor-sqlpool-prod", ""));
    }

    @Test
    void poolWithLocalBackupStorage() {
        assertEquals("CREATE DATABASE [arbor_tenant_a] ( SERVICE_OBJECTIVE = ELASTIC_POOL ( name = [arbor-sqlpool-prod] ) )"
                        + " WITH BACKUP_STORAGE_REDUNDANCY = 'LOCAL'",
                SqlServerDatabaseCreator.createStatement("arbor_tenant_a", "arbor-sqlpool-prod", " local "));
    }

    @Test
    void rejectsUnknownRedundancy() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlServerDatabaseCreator.createStatement("arbor_tenant_a", "pool", "LOCAL'; DROP DATABASE x--"));
    }

    @Test
    void rejectsUnsafeDatabaseName() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlServerDatabaseCreator.createStatement("a]; DROP DATABASE x--", null, "LOCAL"));
    }
}
