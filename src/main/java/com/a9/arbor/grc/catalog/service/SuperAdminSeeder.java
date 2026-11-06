package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.config.ArborSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Seeds the bootstrap Super Admin from configuration once the context is ready, then applies an
 * emergency password reset when {@code arbor.security.super-admin.reset-code} holds a new code.
 */
@Component
public class SuperAdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final SuperAdminService superAdminService;
    private final ArborSecurityProperties properties;

    public SuperAdminSeeder(SuperAdminService superAdminService, ArborSecurityProperties properties) {
        this.superAdminService = superAdminService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        ArborSecurityProperties.SuperAdmin cfg = properties.getSuperAdmin();
        if (cfg.getEmail() == null || cfg.getPassword() == null) {
            log.warn("No bootstrap super-admin configured; skipping seed");
            return;
        }
        superAdminService.seedIfEmpty(cfg.getEmail(), cfg.getPassword(), cfg.getFullName());
        log.info("Super admin bootstrap ensured for {}", cfg.getEmail());

        String resetCode = cfg.getResetCode();
        if (resetCode == null || resetCode.isBlank()) {
            return;
        }
        try {
            SuperAdminService.ResetOutcome outcome = superAdminService.emergencyReset(
                    cfg.getEmail(), cfg.getPassword(), cfg.getFullName(), resetCode);
            if (outcome == SuperAdminService.ResetOutcome.APPLIED) {
                log.warn("AUDIT super_admin_emergency_reset email={} result=APPLIED "
                        + "(password set from configuration; change required at next sign-in)", cfg.getEmail());
            } else {
                log.warn("AUDIT super_admin_emergency_reset email={} result=SKIPPED "
                        + "(reset code already used; remove ARBOR_SUPERADMIN_RESET)", cfg.getEmail());
            }
        } catch (RuntimeException ex) {
            log.error("AUDIT super_admin_emergency_reset email={} result=FAILED reason={}",
                    cfg.getEmail(), ex.getMessage());
        }
    }
}
