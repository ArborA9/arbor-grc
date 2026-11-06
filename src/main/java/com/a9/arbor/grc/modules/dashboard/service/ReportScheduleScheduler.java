package com.a9.arbor.grc.modules.dashboard.service;

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

@Component
public class ReportScheduleScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduleScheduler.class);

    private final TenantRepository tenantRepository;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final ReportScheduleService reportScheduleService;

    public ReportScheduleScheduler(TenantRepository tenantRepository,
                                   TenantDataSourceManager tenantDataSourceManager,
                                   ReportScheduleService reportScheduleService) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.reportScheduleService = reportScheduleService;
    }

    @Scheduled(cron = "${arbor.reports.schedule-cron:0 0 3 * * *}")
    public void scheduledDaily() {
        Instant now = Instant.now();
        int total = 0;
        for (Tenant tenant : tenantRepository.findByStatus(TenantStatus.ACTIVE)) {
            String tid = tenant.getTenantId();
            try {
                tenantDataSourceManager.ensureRegistered(tid);
                TenantContext.setTenantId(tid);
                total += reportScheduleService.processDueSchedules(now);
            } catch (Exception e) {
                log.error("Report schedule run failed for tenant {}: {}", tid, e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
        log.info("Scheduled report delivery finished: {} schedule(s) processed", total);
    }
}
