package com.a9.arbor.grc.modules.bcms.service;

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
public class BcmsLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(BcmsLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final BcmsLifecycleService bcmsLifecycleService;

    public BcmsLifecycleScheduler(TenantRepository tenantRepository,
                                  TenantDataSourceManager tenantDataSourceManager,
                                  BcmsLifecycleService bcmsLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.bcmsLifecycleService = bcmsLifecycleService;
    }

    @Scheduled(cron = "${arbor.bcms.lifecycle-cron:0 25 2 * * *}")
    public void scheduledDaily() {
        Map<String, BcmsLifecycleService.LifecycleRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled BCMS lifecycle finished for {} tenants", results.size());
    }

    public Map<String, BcmsLifecycleService.LifecycleRunResult> runAllTenants(LocalDate asOf) {
        Map<String, BcmsLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, bcmsLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("BCMS lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public BcmsLifecycleService.LifecycleRunResult runCurrentTenant(LocalDate asOf) {
        return bcmsLifecycleService.processCurrentTenant(asOf);
    }
}
