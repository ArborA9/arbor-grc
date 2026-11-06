package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.privacy.domain.DsrStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDsr;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyDsrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

/**
 * UAE PDPL 30-day DSR clock for the current tenant (req §18.2).
 * Day 20 / 28 / overdue notifications are idempotent via notified_at flags.
 */
@Service
public class PrivacyDsrClockService {

    private static final Logger log = LoggerFactory.getLogger(PrivacyDsrClockService.class);

    private static final EnumSet<DsrStatus> OPEN = EnumSet.of(
            DsrStatus.RECEIVED, DsrStatus.IN_PROGRESS, DsrStatus.OVERDUE);

    private final PrivacyDsrRepository dsrRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    public PrivacyDsrClockService(PrivacyDsrRepository dsrRepository,
                                  NotificationService notificationService,
                                  AuditTrailService auditTrailService) {
        this.dsrRepository = dsrRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    public record ClockRunResult(int day20, int day28, int overdue) {
    }

    @Transactional
    public ClockRunResult processCurrentTenant(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        int day20 = 0;
        int day28 = 0;
        int overdue = 0;

        List<PrivacyDsr> open = dsrRepository.findByStatusInOrderByDueAtAsc(OPEN);
        for (PrivacyDsr row : open) {
            LocalDate receivedDate = row.getReceivedAt().atZone(ZoneId.systemDefault()).toLocalDate();

            if (row.getDay20NotifiedAt() == null && !day.isBefore(receivedDate.plusDays(20))) {
                notificationService.trigger(
                        "DSR_DAY_20",
                        String.valueOf(row.getId()),
                        PrivacyDsrService.recipientsAssignedPlusAdmin(row),
                        PrivacyDsrService.dsrTemplateData(row));
                row.setDay20NotifiedAt(Instant.now());
                day20++;
            }

            if (row.getDay28NotifiedAt() == null && !day.isBefore(receivedDate.plusDays(28))) {
                notificationService.trigger(
                        "DSR_DAY_28",
                        String.valueOf(row.getId()),
                        PrivacyDsrService.recipientsAdminOwner(),
                        PrivacyDsrService.dsrTemplateData(row));
                row.setDay28NotifiedAt(Instant.now());
                day28++;
            }

            if (row.getOverdueNotifiedAt() == null && !day.isBefore(row.getDueAt())) {
                String previous = row.getStatus().name();
                if (row.getStatus() != DsrStatus.COMPLETE && row.getStatus() != DsrStatus.REJECTED) {
                    row.setStatus(DsrStatus.OVERDUE);
                }
                notificationService.trigger(
                        "DSR_OVERDUE",
                        String.valueOf(row.getId()),
                        PrivacyDsrService.recipientsAdminOwner(),
                        PrivacyDsrService.dsrTemplateData(row));
                row.setOverdueNotifiedAt(Instant.now());
                if (!previous.equals(row.getStatus().name())) {
                    auditTrailService.record(
                            AuditActionType.UPDATED, "PRIVACY_DSR", null, "SYSTEM",
                            "PRIVACY_DSR", String.valueOf(row.getId()), row.getDisplayId(),
                            "status", previous, DsrStatus.OVERDUE.name(), null, null);
                }
                overdue++;
            }

            dsrRepository.save(row);
        }

        log.info("Privacy DSR clock asOf={}: day20={}, day28={}, overdue={}", day, day20, day28, overdue);
        return new ClockRunResult(day20, day28, overdue);
    }
}
