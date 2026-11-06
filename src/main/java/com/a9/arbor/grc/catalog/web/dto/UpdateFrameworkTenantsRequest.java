package com.a9.arbor.grc.catalog.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateFrameworkTenantsRequest(
        @NotEmpty List<TenantToggle> tenants
) {
    /**
     * @param licenseModule when assigning and the module is off: if true, license it first;
     *                      if false/null, refuse (module must already be licensed).
     */
    public record TenantToggle(
            @NotNull String tenantId,
            @NotNull Boolean assigned,
            Boolean licenseModule,
            /** When licensing the module in this same request: commercial expiry (yyyy-MM-dd). */
            java.time.LocalDate expiresOn
    ) {
        public TenantToggle(String tenantId, Boolean assigned, Boolean licenseModule) {
            this(tenantId, assigned, licenseModule, null);
        }
    }
}
