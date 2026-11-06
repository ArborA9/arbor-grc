package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.domain.TenantStatus;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the PDPL DSR clock across ACTIVE tenants on a short fixed delay so Day-30
 * fires promptly (req §18.2 / open question 10).
 */
@Component
public class PrivacyDsrClockScheduler {

    private static final Logger log = LoggerFactory.getLogger(PrivacyDsrClockScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final PrivacyDsrClockService privacyDsrClockService;

    public PrivacyDsrClockScheduler(TenantRepository tenantRepository,
                                    TenantDataSourceManager tenantDataSourceManager,
                                    PrivacyDsrClockService privacyDsrClockService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.privacyDsrClockService = privacyDsrClockService;
    }

    @Scheduled(fixedDelayString = "${arbor.privacy.dsr-clock-ms:60000}")
    public void scheduledTick() {
        Map<String, PrivacyDsrClockService.ClockRunResult> results = runAllTenants(LocalDate.now());
        log.debug("Privacy DSR clock tick finished for {} tenants", results.size());
    }

    public Map<String, PrivacyDsrClockService.ClockRunResult> runAllTenants(LocalDate asOf) {
        Map<String, PrivacyDsrClockService.ClockRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, privacyDsrClockService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("Privacy DSR clock failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public PrivacyDsrClockService.ClockRunResult runCurrentTenant(LocalDate asOf) {
        return privacyDsrClockService.processCurrentTenant(asOf);
    }
}
