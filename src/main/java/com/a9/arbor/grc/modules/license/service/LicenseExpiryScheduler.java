package com.a9.arbor.grc.modules.license.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.service.LicenseExpiryNoticeService;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Daily licence expiry reminders (3 months, 2 months, 1 month, 2 weeks, 1 week, day before, expiry day).
 */
@Component
public class LicenseExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(LicenseExpiryScheduler.class);

    private final LicenseExpiryNoticeService noticeService;
    private final LicenseExpiryMailService mailService;
    private final TenantDataSourceManager tenantDataSourceManager;

    public LicenseExpiryScheduler(LicenseExpiryNoticeService noticeService,
                                  LicenseExpiryMailService mailService,
                                  TenantDataSourceManager tenantDataSourceManager) {
        this.noticeService = noticeService;
        this.mailService = mailService;
        this.tenantDataSourceManager = tenantDataSourceManager;
    }

    @Scheduled(cron = "${arbor.license.expiry-cron:0 5 2 * * *}")
    public void scheduledDaily() {
        Map<String, Integer> results = runAllTenants(LocalDate.now());
        log.info("Scheduled licence expiry notices finished for {} tenants", results.size());
    }

    public Map<String, Integer> runAllTenants(LocalDate asOf) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Tenant tenant : noticeService.activeTenants()) {
            out.put(tenant.getTenantId(), runTenant(tenant, asOf));
        }
        return out;
    }

    public int runTenant(String tenantId, LocalDate asOf) {
        Tenant tenant = noticeService.activeTenants().stream()
                .filter(t -> t.getTenantId().equalsIgnoreCase(tenantId))
                .findFirst()
                .orElse(null);
        if (tenant == null) {
            return 0;
        }
        return runTenant(tenant, asOf);
    }

    private int runTenant(Tenant tenant, LocalDate asOf) {
        String tid = tenant.getTenantId();
        List<LicenseExpiryNoticeService.DueBatch> batches = noticeService.dueBatches(tid, asOf);
        if (batches.isEmpty()) {
            return 0;
        }
        int sent = 0;
        try {
            tenantDataSourceManager.ensureRegistered(tid);
            TenantContext.setTenantId(tid);
            for (LicenseExpiryNoticeService.DueBatch batch : batches) {
                try {
                    sent += mailService.send(tenant, batch);
                    noticeService.markSent(tid, batch);
                } catch (Exception e) {
                    log.error("Licence expiry mail failed for tenant {} milestone {}: {}",
                            tid, batch.milestone(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Licence expiry scan failed for tenant {}: {}", tid, e.getMessage(), e);
        } finally {
            TenantContext.clear();
        }
        return sent;
    }
}
