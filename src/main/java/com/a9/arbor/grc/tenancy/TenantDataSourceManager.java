package com.a9.arbor.grc.tenancy;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.domain.TenantStatus;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.config.ArborTenancyProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Owns the lifecycle of per-tenant datasources: builds a connection pool for a tenant, runs that
 * tenant's Flyway migrations, and registers it with the {@link TenantRoutingDataSource}.
 */
@Component
public class TenantDataSourceManager {

    private static final Logger log = LoggerFactory.getLogger(TenantDataSourceManager.class);

    private final TenantRoutingDataSource routingDataSource;
    private final TenantRepository tenantRepository;
    private final ArborTenancyProperties properties;

    public TenantDataSourceManager(TenantRoutingDataSource routingDataSource,
                                   TenantRepository tenantRepository,
                                   ArborTenancyProperties properties) {
        this.routingDataSource = routingDataSource;
        this.tenantRepository = tenantRepository;
        this.properties = properties;
    }

    /** Re-register all active tenants after the context is ready (e.g. after a restart). */
    @EventListener(ApplicationReadyEvent.class)
    public void registerActiveTenantsOnStartup() {
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            try {
                register(tenant);
            } catch (RuntimeException e) {
                log.error("Failed to register tenant {} on startup", tenant.getTenantId(), e);
            }
        }
    }

    /** Ensures a tenant's datasource is live, loading it from the catalog if necessary. */
    public void ensureRegistered(String tenantId) {
        if (routingDataSource.contains(tenantId)) {
            return;
        }
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                .orElseThrow(() -> new UnknownTenantException(tenantId));
        register(tenant);
    }

    /** Builds the pool, migrates the tenant schema, and registers the datasource for routing. */
    public synchronized void register(Tenant tenant) {
        if (routingDataSource.contains(tenant.getTenantId())) {
            return;
        }
        DataSource dataSource = buildDataSource(tenant);
        migrate(tenant.getTenantId(), dataSource);
        routingDataSource.addTenant(tenant.getTenantId(), dataSource);
        log.info("Registered datasource for tenant {}", tenant.getTenantId());
    }

    private DataSource buildDataSource(Tenant tenant) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(tenant.getJdbcUrl());
        dataSource.setUsername(tenant.getDbUsername());
        dataSource.setPassword(tenant.getDbPassword());
        dataSource.setPoolName("tenant-" + tenant.getTenantId());
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);
        return dataSource;
    }

    private void migrate(String tenantId, DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenant/" + properties.getVendor())
                .baselineOnMigrate(true)
                .load()
                .migrate();
        log.debug("Migrated tenant schema for {}", tenantId);
    }
}
