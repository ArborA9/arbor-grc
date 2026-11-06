package com.a9.arbor.grc.tenancy;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes JPA/JDBC calls to the datasource of the tenant currently held in {@link TenantContext}.
 *
 * <p>Unlike a static {@link AbstractRoutingDataSource}, tenants are registered dynamically at
 * runtime (on provisioning or first access) via {@link #addTenant(String, DataSource)}.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> tenantDataSources = new ConcurrentHashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenantId();
    }

    /** Registers (or replaces) the datasource for a tenant and rebuilds the resolved lookup map. */
    public synchronized void addTenant(String tenantId, DataSource dataSource) {
        tenantDataSources.put(tenantId, dataSource);
        super.setTargetDataSources(new HashMap<>(tenantDataSources));
        super.afterPropertiesSet();
    }

    public boolean contains(String tenantId) {
        return tenantDataSources.containsKey(tenantId);
    }
}
