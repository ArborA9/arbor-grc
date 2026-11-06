package com.a9.arbor.grc.modules.gap.service;

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

@Component
public class GapLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(GapLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final GapLifecycleService gapLifecycleService;

    public GapLifecycleScheduler(TenantRepository tenantRepository,
                                 TenantDataSourceManager tenantDataSourceManager,
                                 GapLifecycleService gapLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.gapLifecycleService = gapLifecycleService;
    }

    @Scheduled(cron = "${arbor.gap.lifecycle-cron:0 35 2 * * *}")
    public void scheduledDaily() {
        Map<String, GapLifecycleService.LifecycleRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled gap lifecycle finished for {} tenants", results.size());
    }

    public Map<String, GapLifecycleService.LifecycleRunResult> runAllTenants(LocalDate asOf) {
        Map<String, GapLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, gapLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("Gap lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public GapLifecycleService.LifecycleRunResult runCurrentTenant(LocalDate asOf) {
        return gapLifecycleService.processCurrentTenant(asOf);
    }
}
