package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.service.TenantProvisioningService.ProvisionResult;

/**
 * Response to a provisioning request. {@code initialAdminPassword} is present only when mail is not
 * live (local dev and E2E). It is not stored in retrievable form and cannot be shown again.
 */
public record ProvisionTenantResponse(
        TenantResponse tenant,
        String initialAdminEmail,
        String initialAdminPassword,
        boolean passwordEmailed,
        boolean ssoConfigured
) {
    public static ProvisionTenantResponse from(ProvisionResult result) {
        return new ProvisionTenantResponse(
                TenantResponse.from(result.tenant()),
                result.adminEmail(),
                result.adminPassword(),
                result.passwordEmailed(),
                result.ssoConfigured());
    }
}
