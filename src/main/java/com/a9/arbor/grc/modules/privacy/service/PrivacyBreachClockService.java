package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.privacy.domain.BreachStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyBreach;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyBreachRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/** UAE PDPL 72-hour breach notification clock (req §18 future / F4). */
@Service
public class PrivacyBreachClockService {

    private static final Logger log = LoggerFactory.getLogger(PrivacyBreachClockService.class);

    private static final EnumSet<BreachStatus> OPEN_CLOCK = EnumSet.of(
            BreachStatus.OPEN, BreachStatus.INVESTIGATING, BreachStatus.OVERDUE);

    private final PrivacyBreachRepository breachRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    public PrivacyBreachClockService(PrivacyBreachRepository breachRepository,
                                     NotificationService notificationService,
                                     AuditTrailService auditTrailService) {
        this.breachRepository = breachRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    public record ClockRunResult(int hour48, int overdue) {
    }

    @Transactional
    public ClockRunResult processCurrentTenant(Instant asOf) {
        Instant now = asOf != null ? asOf : Instant.now();
        int hour48 = 0;
        int overdue = 0;

        List<PrivacyBreach> open = breachRepository.findByStatusInOrderByReportDeadlineAtAsc(OPEN_CLOCK);
        for (PrivacyBreach row : open) {
            Instant warnAt = row.getDiscoveredAt().plusSeconds(48 * 3600L);

            if (row.getHour48NotifiedAt() == null && !now.isBefore(warnAt)) {
                notificationService.trigger(
                        "BREACH_48H_WARNING",
                        String.valueOf(row.getId()),
                        PrivacyBreachService.recipientsAdminOwner(),
                        PrivacyBreachService.templateData(row));
                row.setHour48NotifiedAt(Instant.now());
                hour48++;
            }

            if (row.getOverdueNotifiedAt() == null && !now.isBefore(row.getReportDeadlineAt())) {
                String previous = row.getStatus().name();
                if (row.getStatus() != BreachStatus.REPORTED && row.getStatus() != BreachStatus.CLOSED) {
                    row.setStatus(BreachStatus.OVERDUE);
                }
                notificationService.trigger(
                        "BREACH_72H_OVERDUE",
                        String.valueOf(row.getId()),
                        PrivacyBreachService.recipientsAdminOwner(),
                        PrivacyBreachService.templateData(row));
                row.setOverdueNotifiedAt(Instant.now());
                if (!previous.equals(row.getStatus().name())) {
                    auditTrailService.record(
                            AuditActionType.UPDATED, "PRIVACY_BREACH", null, "SYSTEM",
                            "PRIVACY_BREACH", String.valueOf(row.getId()), row.getDisplayId(),
                            "status", previous, BreachStatus.OVERDUE.name(), null, null);
                }
                overdue++;
            }

            breachRepository.save(row);
        }

        log.info("Privacy breach clock asOf={}: hour48={}, overdue={}", now, hour48, overdue);
        return new ClockRunResult(hour48, overdue);
    }
}
