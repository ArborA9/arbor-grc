package com.a9.arbor.grc.tenancy;

/** Thrown when a request references a tenant id that is not present (or not active) in the catalog. */
public class UnknownTenantException extends RuntimeException {

    public UnknownTenantException(String tenantId) {
        super("Unknown or inactive tenant: " + tenantId);
    }
}
