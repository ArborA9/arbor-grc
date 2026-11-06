package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.Tenant;

import java.time.Instant;

/** Public view of a tenant (never exposes the database password or the OIDC client secret). */
public record TenantResponse(
        Long id,
        String tenantId,
        String name,
        String status,
        String jdbcUrl,
        boolean ssoEnabled,
        String oidcIssuer,
        String oidcClientId,
        boolean jitEnabled,
        String jitDefaultRole,
        Instant createdAt,
        TenantLicenseSummary license
) {
    public static TenantResponse from(Tenant tenant) {
        return from(tenant, null);
    }

    public static TenantResponse from(Tenant tenant, TenantLicenseSummary license) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getTenantId(),
                tenant.getName(),
                tenant.getStatus().name(),
                tenant.getJdbcUrl(),
                tenant.isSsoEnabled(),
                tenant.getOidcIssuer(),
                tenant.getOidcClientId(),
                tenant.isJitEnabled(),
                tenant.getJitDefaultRole().name(),
                tenant.getCreatedAt(),
                license
        );
    }
}
