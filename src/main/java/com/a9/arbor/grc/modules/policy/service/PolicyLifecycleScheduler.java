package com.a9.arbor.grc.modules.policy.service;

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
public class PolicyLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(PolicyLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final PolicyLifecycleService policyLifecycleService;

    public PolicyLifecycleScheduler(TenantRepository tenantRepository,
                                    TenantDataSourceManager tenantDataSourceManager,
                                    PolicyLifecycleService policyLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.policyLifecycleService = policyLifecycleService;
    }

    @Scheduled(cron = "${arbor.policy.lifecycle-cron:0 40 2 * * *}")
    public void scheduledDaily() {
        Map<String, PolicyLifecycleService.LifecycleRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled policy lifecycle finished for {} tenants", results.size());
    }

    public Map<String, PolicyLifecycleService.LifecycleRunResult> runAllTenants(LocalDate asOf) {
        Map<String, PolicyLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, policyLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("Policy lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }
}
