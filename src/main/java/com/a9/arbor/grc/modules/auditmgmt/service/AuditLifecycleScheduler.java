package com.a9.arbor.grc.modules.auditmgmt.service;

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
public class AuditLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditLifecycleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final AuditLifecycleService auditLifecycleService;

    public AuditLifecycleScheduler(TenantRepository tenantRepository,
                                   TenantDataSourceManager tenantDataSourceManager,
                                   AuditLifecycleService auditLifecycleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.auditLifecycleService = auditLifecycleService;
    }

    @Scheduled(cron = "${arbor.audit.lifecycle-cron:0 30 2 * * *}")
    public void scheduledDaily() {
        Map<String, AuditLifecycleService.LifecycleRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled audit lifecycle finished for {} tenants", results.size());
    }

    public Map<String, AuditLifecycleService.LifecycleRunResult> runAllTenants(LocalDate asOf) {
        Map<String, AuditLifecycleService.LifecycleRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, auditLifecycleService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("Audit lifecycle failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public AuditLifecycleService.LifecycleRunResult runCurrentTenant(LocalDate asOf) {
        return auditLifecycleService.processCurrentTenant(asOf);
    }
}
