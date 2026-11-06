package com.a9.arbor.grc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the multi-tenant data layer.
 *
 * <p>All tenant databases share the same server/credentials in the current setup; per-tenant
 * credentials (via Key Vault) can be introduced later without changing the routing model.
 */
@Data
@ConfigurationProperties(prefix = "arbor.tenancy")
public class ArborTenancyProperties {

    /** Datasource vendor: {@code h2} (local) or {@code sqlserver} (Azure). Drives migration scripts. */
    private String vendor = "h2";

    /** Whether to physically CREATE DATABASE before migrating (SQL Server); false for H2. */
    private boolean createDatabase = false;

    /**
     * Azure SQL elastic pool name. When set with {@link #createDatabase}, tenant (and catalog)
     * databases are created into that pool. Blank for local SQL Server / non-pooled Azure.
     */
    private String elasticPoolName = "";

    /**
     * Azure SQL backup storage for databases this app creates: {@code LOCAL}, {@code ZONE},
     * {@code GEO}, or {@code GEOZONE}. Blank omits the option (local SQL Server rejects it).
     */
    private String backupStorageRedundancy = "";

    /** JDBC URL pointing at the server's admin/master database, used to create tenant databases. */
    private String masterUrl = "";

    /** JDBC URL template; {@code %s} is replaced with the physical database name. */
    private String urlTemplate;

    private String username;
    private String password;

    /** Name of the shared catalog database (tenant registry). */
    private String catalogDatabase = "arbor_catalog";

    /** Prefix applied to a tenant id to form its physical database name. */
    private String tenantDbPrefix = "arbor_tenant_";

    public String catalogJdbcUrl() {
        return String.format(urlTemplate, catalogDatabase);
    }

    public String tenantDatabaseName(String tenantId) {
        return tenantDbPrefix + tenantId;
    }

    public String tenantJdbcUrl(String tenantId) {
        return String.format(urlTemplate, tenantDatabaseName(tenantId));
    }
}
