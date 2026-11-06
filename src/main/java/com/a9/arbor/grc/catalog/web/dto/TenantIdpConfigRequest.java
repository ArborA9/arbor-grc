package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.JitDefaultRole;
import jakarta.validation.constraints.Size;

/**
 * Super Admin request to configure a tenant's SSO / AD (OIDC) integration. When {@code ssoEnabled}
 * is true, the issuer and client id are required (validated in the service). {@code jitEnabled} /
 * {@code jitDefaultRole} control JIT provisioning (Path C B1): only meaningful when SSO is on.
 */
public record TenantIdpConfigRequest(
        boolean ssoEnabled,
        @Size(max = 1000) String oidcIssuer,
        @Size(max = 255) String oidcClientId,
        @Size(max = 500) String oidcClientSecret,
        /** Null = leave existing tenant value unchanged. */
        Boolean jitEnabled,
        /** Null = leave existing tenant value unchanged (or BLOCK when first enabling via default). */
        JitDefaultRole jitDefaultRole
) {
}
