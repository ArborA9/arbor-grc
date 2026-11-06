package com.a9.arbor.grc.modules.kri.service;

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
public class KriLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(KriLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final KriLifecycleService kriLifecycleService;

    public KriLifecycleScheduler(TenantRepository tenantRepository,
                                 TenantDataSourceManager tenantDataSourceManager,
                                 KriLifecycleService kriLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.kriLifecycleService = kriLifecycleService;
    }

    @Scheduled(cron = "${arbor.kri.lifecycle-cron:0 30 2 * * *}")
    public void scheduledDaily() {
        Map<String, KriLifecycleService.LifecycleRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled KRI lifecycle finished for {} tenants", results.size());
    }

    public Map<String, KriLifecycleService.LifecycleRunResult> runAllTenants(LocalDate asOf) {
        Map<String, KriLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, kriLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("KRI lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public KriLifecycleService.LifecycleRunResult runCurrentTenant(LocalDate asOf) {
        return kriLifecycleService.processCurrentTenant(asOf);
    }
}
