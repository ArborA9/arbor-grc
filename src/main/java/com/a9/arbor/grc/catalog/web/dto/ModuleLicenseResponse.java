package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.LicenseDates;
import com.a9.arbor.grc.catalog.domain.TenantModule;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ModuleLicenseResponse(
        String code,
        boolean enabled,
        Instant enabledAt,
        Instant expiresAt,
        LocalDate expiresOn,
        boolean expired,
        Integer daysRemaining,
        /** UNLICENSED | ACTIVE | EXPIRING | EXPIRED */
        String status,
        /** Published framework ids entitled for this tenant under the module (empty if module off). */
        List<Long> entitledFrameworkIds
) {
    public static ModuleLicenseResponse from(TenantModule m, List<Long> entitledFrameworkIds, LocalDate today) {
        LocalDate expiresOn = LicenseDates.toExpiryDate(m.getExpiresAt());
        boolean expired = m.isEnabled() && LicenseDates.isExpired(m.getExpiresAt(), today);
        Integer daysRemaining = m.isEnabled() ? LicenseDates.daysRemaining(m.getExpiresAt(), today) : null;
        String status;
        if (!m.isEnabled()) {
            status = "UNLICENSED";
        } else if (expired) {
            status = "EXPIRED";
        } else if (daysRemaining != null && daysRemaining >= 0 && daysRemaining <= 90) {
            status = "EXPIRING";
        } else {
            status = "ACTIVE";
        }
        return new ModuleLicenseResponse(
                m.getModuleCode().name(),
                m.isEnabled(),
                m.getEnabledAt(),
                m.getExpiresAt(),
                expiresOn,
                expired,
                daysRemaining,
                status,
                entitledFrameworkIds == null ? List.of() : List.copyOf(entitledFrameworkIds));
    }
}
