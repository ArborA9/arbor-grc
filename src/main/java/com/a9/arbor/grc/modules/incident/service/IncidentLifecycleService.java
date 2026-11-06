package com.a9.arbor.grc.modules.incident.service;

import com.a9.arbor.grc.modules.incident.domain.IncidentRecord;
import com.a9.arbor.grc.modules.incident.domain.IncidentStatus;
import com.a9.arbor.grc.modules.incident.repo.IncidentRecordRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IncidentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(IncidentLifecycleService.class);

    private final IncidentRecordRepository incidentRepository;
    private final NotificationService notificationService;

    public IncidentLifecycleService(IncidentRecordRepository incidentRepository,
                                    NotificationService notificationService) {
        this.incidentRepository = incidentRepository;
        this.notificationService = notificationService;
    }

    public record LifecycleRunResult(int overdueNotified) {
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(Instant asOf) {
        Instant now = asOf != null ? asOf : Instant.now();
        int overdue = 0;
        List<IncidentStatus> open = List.of(IncidentStatus.REPORTED, IncidentStatus.ASSIGNED);
        for (IncidentRecord inc : incidentRepository.findByStatusInOrderByResponseDueAtAsc(open)) {
            if (inc.getResponseDueAt() == null || !inc.getResponseDueAt().isBefore(now)) {
                continue;
            }
            if (inc.getOverdueNotifiedAt() != null) {
                continue;
            }
            if (inc.getRespondedAt() != null) {
                continue; // response already made
            }
            inc.setOverdueNotifiedAt(now);
            incidentRepository.save(inc);
            List<String> recipients = new ArrayList<>();
            if (inc.getOwnerUserId() != null) {
                recipients.add(String.valueOf(inc.getOwnerUserId()));
            }
            recipients.add("ADMIN");
            notificationService.trigger(
                    "INCIDENT_OVERDUE",
                    String.valueOf(inc.getId()),
                    recipients,
                    Map.of(
                            "displayId", inc.getDisplayId(),
                            "title", inc.getTitle(),
                            "responseDue", inc.getResponseDueAt().toString()));
            overdue++;
        }
        log.info("Incident lifecycle asOf={}: overdue={}", now, overdue);
        return new LifecycleRunResult(overdue);
    }
}
