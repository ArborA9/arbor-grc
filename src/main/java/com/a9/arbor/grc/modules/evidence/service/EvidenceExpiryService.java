package com.a9.arbor.grc.modules.evidence.service;

import com.a9.arbor.grc.config.ArborEvidenceProperties;
import com.a9.arbor.grc.modules.evidence.domain.Evidence;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceStatus;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceRepository;
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
 * Marks expired evidence and notifies before expiry (EVIDENCE_EXPIRING, req trigger 87).
 */
@Service
public class EvidenceExpiryService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceExpiryService.class);

    private final EvidenceRepository evidenceRepository;
    private final NotificationService notificationService;
    private final ArborEvidenceProperties properties;

    public EvidenceExpiryService(EvidenceRepository evidenceRepository,
                                 NotificationService notificationService,
                                 ArborEvidenceProperties properties) {
        this.evidenceRepository = evidenceRepository;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    public record ExpiryRunResult(int markedExpired, int expiringNotified) {
    }

    @Transactional
    public ExpiryRunResult processCurrentTenant(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        int expired = markExpired(day);
        int notified = notifyExpiring(day);
        log.info("Evidence expiry asOf={}: markedExpired={}, expiringNotified={}",
                day, expired, notified);
        return new ExpiryRunResult(expired, notified);
    }

    private int markExpired(LocalDate asOf) {
        int count = 0;
        for (Evidence e : evidenceRepository.findActivePastExpiry(asOf)) {
            e.setStatus(EvidenceStatus.EXPIRED);
            if (e.getExpiredNotifiedAt() == null) {
                List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
                if (e.getCreatedByUserId() != null) {
                    recipients.add(e.getCreatedByUserId().toString());
                }
                notificationService.trigger(
                        "EVIDENCE_EXPIRED",
                        String.valueOf(e.getId()),
                        recipients,
                        Map.of(
                                "title", e.getTitle(),
                                "evidenceId", String.valueOf(e.getId()),
                                "expiresOn", e.getExpiresOn() != null ? e.getExpiresOn().toString() : "-"));
                e.setExpiredNotifiedAt(Instant.now());
            }
            evidenceRepository.save(e);
            count++;
        }
        return count;
    }

    private int notifyExpiring(LocalDate asOf) {
        int warnDays = Math.max(1, properties.getExpiryWarnDays());
        LocalDate windowEnd = asOf.plusDays(warnDays);
        int count = 0;
        for (Evidence e : evidenceRepository.findExpiringForNotify(asOf, windowEnd)) {
            List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
            if (e.getCreatedByUserId() != null) {
                recipients.add(e.getCreatedByUserId().toString());
            }
            notificationService.trigger(
                    "EVIDENCE_EXPIRING",
                    String.valueOf(e.getId()),
                    recipients,
                    Map.of(
                            "title", e.getTitle(),
                            "evidenceId", String.valueOf(e.getId()),
                            "expiresOn", e.getExpiresOn().toString()));
            e.setExpiryNotifiedAt(Instant.now());
            evidenceRepository.save(e);
            count++;
        }
        return count;
    }
}
