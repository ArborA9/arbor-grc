package com.a9.arbor.grc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fails fast on the {@code azure} profile when bootstrap secrets were not overridden.
 */
@Component
@Profile("azure")
public class ProductionSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);

    private static final String DEFAULT_JWT_MARKER = "dev-only-secret-change-me";
    private static final String DEFAULT_SA_PASSWORD = "ChangeMe_SuperAdmin_123";

    private final ArborSecurityProperties properties;

    public ProductionSecurityValidator(ArborSecurityProperties properties) {
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        String jwtSecret = properties.getJwt().getSecret();
        if (jwtSecret == null || jwtSecret.contains(DEFAULT_JWT_MARKER)) {
            throw new IllegalStateException(
                    "Production startup blocked: set ARBOR_JWT_SECRET to a unique Key Vault value "
                            + "(default dev secret detected).");
        }
        String saPassword = properties.getSuperAdmin().getPassword();
        if (DEFAULT_SA_PASSWORD.equals(saPassword)) {
            throw new IllegalStateException(
                    "Production startup blocked: set ARBOR_SUPERADMIN_PASSWORD "
                            + "(default bootstrap password detected).");
        }
        List<String> origins = properties.getCors().effectiveOrigins();
        List<String> patterns = properties.getCors().effectiveOriginPatterns();
        if (origins.isEmpty() && patterns.isEmpty()) {
            throw new IllegalStateException(
                    "Production startup blocked: set ARBOR_CORS_ALLOWED_ORIGINS "
                            + "or ARBOR_CORS_ALLOWED_ORIGIN_PATTERNS.");
        }
        log.info("Production security validator passed (non-default secrets, CORS allowlist present).");
    }
}
