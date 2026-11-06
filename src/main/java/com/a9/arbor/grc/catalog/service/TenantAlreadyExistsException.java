package com.a9.arbor.grc.catalog.service;

/** Thrown when attempting to provision a tenant id that is already registered. */
public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String tenantId) {
        super("Tenant already exists: " + tenantId);
    }
}
