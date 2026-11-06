package com.a9.arbor.grc.catalog.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Super Admin: set or extend licence expiry for one, several, or all enabled modules of a tenant.
 */
public record ExtendModulesExpiryRequest(
        @NotNull LocalDate expiresOn,
        /** Null or empty = every currently licensed module for the tenant. */
        List<String> codes
) {
}
