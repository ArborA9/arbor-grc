package com.a9.arbor.grc.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request to provision a new tenant.
 *
 * @param tenantId       short, url/database-safe identifier (letters, digits, underscore; stored lowercase)
 * @param name           human-readable organisation name
 * @param ownerEmail     real Owner email. Required when mail is live or SSO is configured.
 * @param ownerFullName  Owner display name
 * @param ssoEnabled     when true, Entra is stored now and the Owner's first login is SSO
 * @param oidcIssuer     required when SSO is enabled
 * @param oidcClientId   required when SSO is enabled
 * @param oidcClientSecret required when SSO is enabled
 */
public record CreateTenantRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "[A-Za-z0-9_]+", message = "tenantId must be letters, digits or underscore")
        String tenantId,

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 255)
        String ownerEmail,

        @Size(max = 255)
        String ownerFullName,

        Boolean ssoEnabled,

        @Size(max = 1000)
        String oidcIssuer,

        @Size(max = 255)
        String oidcClientId,

        @Size(max = 500)
        String oidcClientSecret
) {
}
