package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditStandard;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditStatus;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Audit date-based triggers (#66, #71, #72). */
@Service
public class AuditLifecycleService {

    private static final Set<GrcAuditStatus> OPEN_STATUSES = Set.of(
            GrcAuditStatus.DRAFT,
            GrcAuditStatus.ACCESS_GRANTED,
            GrcAuditStatus.IN_PROGRESS);

    private final GrcAuditRepository auditRepository;
    private final NotificationService notificationService;

    public AuditLifecycleService(GrcAuditRepository auditRepository,
                                NotificationService notificationService) {
        this.auditRepository = auditRepository;
        this.notificationService = notificationService;
    }

    public record LifecycleRunResult(int startDate, int overdue, int reportPending) {
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        int startDate = 0;
        int overdue = 0;
        int reportPending = 0;

        for (GrcAudit audit : auditRepository.findAllByOrderByUpdatedAtDesc()) {
            if (audit.isLocked()) {
                continue;
            }
            if (audit.getStartDate() != null
                    && audit.getStartDate().equals(day)
                    && audit.getStartNotifiedAt() == null) {
                notificationService.trigger("AUDIT_START_DATE", String.valueOf(audit.getId()),
                        auditRecipients(audit), auditData(audit));
                audit.setStartNotifiedAt(Instant.now());
                auditRepository.save(audit);
                startDate++;
            }
            if (audit.getEndDate() != null
                    && audit.getEndDate().isBefore(day)
                    && OPEN_STATUSES.contains(audit.getStatus())
                    && audit.getOverdueNotifiedAt() == null) {
                notificationService.trigger("AUDIT_OVERDUE", String.valueOf(audit.getId()),
                        auditRecipients(audit), auditData(audit));
                audit.setOverdueNotifiedAt(Instant.now());
                auditRepository.save(audit);
                overdue++;
            }
            if (audit.getStatus() == GrcAuditStatus.REPORT_ISSUED
                    && audit.getReportPendingNotifiedAt() == null) {
                notificationService.trigger("AUDIT_REPORT_PENDING", String.valueOf(audit.getId()),
                        List.of("ADMIN", "OWNER"), auditData(audit));
                audit.setReportPendingNotifiedAt(Instant.now());
                auditRepository.save(audit);
                reportPending++;
            }
        }
        return new LifecycleRunResult(startDate, overdue, reportPending);
    }

    private static List<String> auditRecipients(GrcAudit audit) {
        List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
        if (audit.getLeadAuditorUserId() != null) {
            recipients.add(audit.getLeadAuditorUserId().toString());
        }
        return recipients;
    }

    static Map<String, String> auditData(GrcAudit audit) {
        Map<String, String> data = new HashMap<>();
        String name = audit.getName() == null ? "" : audit.getName();
        data.put("name", name);
        data.put("auditName", name);
        String framework = audit.getAuditStandard() == AuditStandard.MULTIPLE
                ? "Multiple"
                : audit.getFrameworkName();
        if (framework == null || framework.isBlank() || "-".equals(framework)) {
            framework = audit.getFrameworkCode() != null ? audit.getFrameworkCode() : "-";
        }
        data.put("frameworkCode", framework);
        data.put("startDate", audit.getStartDate() != null ? audit.getStartDate().toString() : "-");
        data.put("endDate", audit.getEndDate() != null ? audit.getEndDate().toString() : "-");
        data.put("status", audit.getStatus().name());
        return data;
    }
}
