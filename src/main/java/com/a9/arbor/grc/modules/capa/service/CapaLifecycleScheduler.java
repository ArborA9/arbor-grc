package com.a9.arbor.grc.modules.capa.service;

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

/** Runs CAPA lifecycle across all active tenants (daily). Manual run available via API. */
@Component
public class CapaLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(CapaLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final CapaLifecycleService capaLifecycleService;

    public CapaLifecycleScheduler(TenantRepository tenantRepository,
                                  TenantDataSourceManager tenantDataSourceManager,
                                  CapaLifecycleService capaLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.capaLifecycleService = capaLifecycleService;
    }

    /** Daily 02:20 server time (after risk lifecycle). */
    @Scheduled(cron = "${arbor.capa.lifecycle-cron:0 20 2 * * *}")
    public void scheduledDaily() {
        Map<String, CapaLifecycleService.LifecycleRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled CAPA lifecycle finished for {} tenants", results.size());
    }

    public Map<String, CapaLifecycleService.LifecycleRunResult> runAllTenants(LocalDate asOf) {
        Map<String, CapaLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, capaLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("CAPA lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public CapaLifecycleService.LifecycleRunResult runCurrentTenant(LocalDate asOf) {
        return capaLifecycleService.processCurrentTenant(asOf);
    }
}
