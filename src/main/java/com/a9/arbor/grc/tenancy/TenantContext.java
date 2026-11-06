package com.a9.arbor.grc.tenancy;

/**
 * Holds the current request's tenant id in a thread-local so the routing datasource can select
 * the correct per-tenant database. Set by {@link TenantResolverFilter} and cleared after each request.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static boolean hasTenant() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
