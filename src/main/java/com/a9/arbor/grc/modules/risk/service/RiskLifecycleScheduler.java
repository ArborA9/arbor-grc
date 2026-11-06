package com.a9.arbor.grc.modules.risk.service;

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
 * Runs risk lifecycle across all active tenants (daily). Manual run available via API.
 */
@Component
public class RiskLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(RiskLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final RiskLifecycleService riskLifecycleService;

    public RiskLifecycleScheduler(TenantRepository tenantRepository,
                                  TenantDataSourceManager tenantDataSourceManager,
                                  RiskLifecycleService riskLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.riskLifecycleService = riskLifecycleService;
    }

    /** Daily 02:15 server time. */
    @Scheduled(cron = "${arbor.risk.lifecycle-cron:0 15 2 * * *}")
    public void scheduledDaily() {
        Map<String, RiskLifecycleService.LifecycleRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled risk lifecycle finished for {} tenants", results.size());
    }

    public Map<String, RiskLifecycleService.LifecycleRunResult> runAllTenants(LocalDate asOf) {
        Map<String, RiskLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, riskLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("Risk lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public RiskLifecycleService.LifecycleRunResult runCurrentTenant(LocalDate asOf) {
        return riskLifecycleService.processCurrentTenant(asOf);
    }
}
