package com.a9.arbor.grc.modules.capa.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.domain.CapaStatus;
import com.a9.arbor.grc.modules.capa.repo.CapaRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Daily CAPA lifecycle: D−3 due-soon, overdue on target date, D+7 escalation (req §17.1).
 */
@Service
public class CapaLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(CapaLifecycleService.class);

    private final CapaRepository capaRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    public CapaLifecycleService(CapaRepository capaRepository,
                                NotificationService notificationService,
                                AuditTrailService auditTrailService) {
        this.capaRepository = capaRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    public record LifecycleRunResult(int dueSoonNotified, int overdueMarked, int escalations) {
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        int dueSoon = processDueSoon(day);
        int overdue = processOverdue(day);
        int escalations = processEscalations(day);
        log.info("CAPA lifecycle asOf={}: dueSoon={}, overdue={}, escalations={}",
                day, dueSoon, overdue, escalations);
        return new LifecycleRunResult(dueSoon, overdue, escalations);
    }

    private int processDueSoon(LocalDate asOf) {
        LocalDate dueSoonDate = asOf.plusDays(3);
        int count = 0;
        for (Capa c : capaRepository.findDueSoonNotNotified(dueSoonDate)) {
            c.setDueSoonNotifiedAt(Instant.now());
            capaRepository.save(c);
            notificationService.trigger(
                    "CAPA_DUE_SOON",
                    String.valueOf(c.getId()),
                    ownerRecipients(c),
                    notifyData(c));
            count++;
        }
        return count;
    }

    private int processOverdue(LocalDate asOf) {
        int count = 0;
        for (Capa c : capaRepository.findPastTargetNotOverdueNotified(asOf)) {
            if (c.getStatus() != CapaStatus.OVERDUE) {
                String prior = c.getStatus().name();
                c.setStatus(CapaStatus.OVERDUE);
                auditTrailService.record(
                        AuditActionType.UPDATED, "CAPA", null, "SYSTEM",
                        "CAPA", String.valueOf(c.getId()), c.getTitle(),
                        "status", prior, "OVERDUE", null, null);
            }
            c.setOverdueNotifiedAt(Instant.now());
            capaRepository.save(c);
            List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
            if (c.getOwnerUserId() != null) {
                recipients.add(c.getOwnerUserId().toString());
            }
            notificationService.trigger(
                    "CAPA_OVERDUE",
                    String.valueOf(c.getId()),
                    recipients,
                    notifyData(c));
            count++;
        }
        return count;
    }

    private int processEscalations(LocalDate asOf) {
        LocalDate escalationCutoff = asOf.minusDays(7);
        int count = 0;
        for (Capa c : capaRepository.findEscalationDue(escalationCutoff)) {
            c.setEscalationNotifiedAt(Instant.now());
            capaRepository.save(c);
            notificationService.trigger(
                    "CAPA_OVERDUE_ESCALATION",
                    String.valueOf(c.getId()),
                    List.of("ADMIN", "OWNER"),
                    notifyData(c));
            count++;
        }
        return count;
    }

    private static List<String> ownerRecipients(Capa c) {
        if (c.getOwnerUserId() != null) {
            return List.of(c.getOwnerUserId().toString());
        }
        return List.of("ADMIN", "OWNER");
    }

    private static Map<String, String> notifyData(Capa c) {
        return Map.of(
                "title", c.getTitle(),
                "controlRef", c.getControlRef() != null ? c.getControlRef() : "-",
                "targetDate", c.getTargetDate() != null ? c.getTargetDate().toString() : "-");
    }
}
