package com.a9.arbor.grc.modules.evidence.service;

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

/** Daily evidence expiry across active tenants. Manual run via API. */
@Component
public class EvidenceExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EvidenceExpiryScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final EvidenceExpiryService evidenceExpiryService;

    public EvidenceExpiryScheduler(TenantRepository tenantRepository,
                                   TenantDataSourceManager tenantDataSourceManager,
                                   EvidenceExpiryService evidenceExpiryService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.evidenceExpiryService = evidenceExpiryService;
    }

    /** Daily 02:30 server time (after risk lifecycle). */
    @Scheduled(cron = "${arbor.evidence.expiry-cron:0 30 2 * * *}")
    public void scheduledDaily() {
        Map<String, EvidenceExpiryService.ExpiryRunResult> results = runAllTenants(LocalDate.now());
        log.info("Scheduled evidence expiry finished for {} tenants", results.size());
    }

    public Map<String, EvidenceExpiryService.ExpiryRunResult> runAllTenants(LocalDate asOf) {
        Map<String, EvidenceExpiryService.ExpiryRunResult> out = new LinkedHashMap<>();
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                out.put(tid, evidenceExpiryService.processCurrentTenant(asOf));
            } catch (Exception e) {
                log.error("Evidence expiry failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        return out;
    }

    public EvidenceExpiryService.ExpiryRunResult runCurrentTenant(LocalDate asOf) {
        return evidenceExpiryService.processCurrentTenant(asOf);
    }
}
