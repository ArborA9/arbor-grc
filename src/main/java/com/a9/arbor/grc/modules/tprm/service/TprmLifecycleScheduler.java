package com.a9.arbor.grc.modules.tprm.service;

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
public class TprmLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(TprmLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final TprmLifecycleService tprmLifecycleService;

    public TprmLifecycleScheduler(TenantRepository tenantRepository,
                                  TenantDataSourceManager tenantDataSourceManager,
                                  TprmLifecycleService tprmLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.tprmLifecycleService = tprmLifecycleService;
    }

    @Scheduled(cron = "${arbor.tprm.lifecycle-cron:0 45 2 * * *}")
    public void scheduledDaily() {
        Map<String, TprmLifecycleService.LifecycleRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled TPRM lifecycle finished for {} tenants", results.size());
    }

    public Map<String, TprmLifecycleService.LifecycleRunResult> runAllTenants(LocalDate asOf) {
        Map<String, TprmLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, tprmLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("TPRM lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }
}
