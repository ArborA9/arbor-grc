package com.a9.arbor.grc.modules.incident.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.domain.TenantStatus;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class IncidentLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(IncidentLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final IncidentLifecycleService incidentLifecycleService;

    public IncidentLifecycleScheduler(TenantRepository tenantRepository,
                                      TenantDataSourceManager tenantDataSourceManager,
                                      IncidentLifecycleService incidentLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.incidentLifecycleService = incidentLifecycleService;
    }

    /** Hourly: P1/P2 response windows are hours, not days. */
    @Scheduled(cron = "${arbor.incident.lifecycle-cron:0 15 * * * *}")
    public void scheduledHourly() {
        Map<String, IncidentLifecycleService.LifecycleRunResult> results = runAllTenants(Instant.now());
        log.info("Scheduled incident lifecycle finished for {} tenants", results.size());
    }

    public Map<String, IncidentLifecycleService.LifecycleRunResult> runAllTenants(Instant asOf) {
        Map<String, IncidentLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, incidentLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("Incident lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public IncidentLifecycleService.LifecycleRunResult runCurrentTenant(Instant asOf) {
        return incidentLifecycleService.processCurrentTenant(asOf);
    }
}
