package com.a9.arbor.grc.modules.kri.service;

import com.a9.arbor.grc.modules.kri.domain.Kri;
import com.a9.arbor.grc.modules.kri.domain.KriStatus;
import com.a9.arbor.grc.modules.kri.repo.KriRepository;
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

@Service
public class KriLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(KriLifecycleService.class);

    private final KriRepository kriRepository;
    private final KriService kriService;
    private final NotificationService notificationService;

    public KriLifecycleService(KriRepository kriRepository,
                               KriService kriService,
                               NotificationService notificationService) {
        this.kriRepository = kriRepository;
        this.kriService = kriService;
        this.notificationService = notificationService;
    }

    public record LifecycleRunResult(int dueNotified, int overdueNotified) {
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        int due = 0;
        int overdue = 0;
        for (Kri kri : kriRepository.findByStatusOrderByUpdatedAtDesc(KriStatus.ACTIVE)) {
            if (kri.getNextDueDate() == null) {
                continue;
            }
            if (kri.getNextDueDate().isEqual(day) && kri.getDueNotifiedAt() == null) {
                kriService.ensureDueTask(kri);
                kri.setDueNotifiedAt(Instant.now());
                kriRepository.save(kri);
                notificationService.trigger(
                        "KRI_VALUE_DUE",
                        String.valueOf(kri.getId()),
                        List.of(String.valueOf(kri.getOwnerUserId())),
                        Map.of(
                                "displayId", kri.getDisplayId(),
                                "name", kri.getName(),
                                "dueDate", kri.getNextDueDate().toString()));
                due++;
            } else if (kri.getNextDueDate().isBefore(day) && kri.getOverdueNotifiedAt() == null) {
                kriService.ensureDueTask(kri);
                kri.setOverdueNotifiedAt(Instant.now());
                kriRepository.save(kri);
                List<String> recipients = new ArrayList<>();
                recipients.add(String.valueOf(kri.getOwnerUserId()));
                recipients.add("ADMIN");
                notificationService.trigger(
                        "KRI_VALUE_OVERDUE",
                        String.valueOf(kri.getId()),
                        recipients,
                        Map.of(
                                "displayId", kri.getDisplayId(),
                                "name", kri.getName(),
                                "dueDate", kri.getNextDueDate().toString()));
                overdue++;
            }
        }
        log.info("KRI lifecycle asOf={}: due={}, overdue={}", day, due, overdue);
        return new LifecycleRunResult(due, overdue);
    }
}
