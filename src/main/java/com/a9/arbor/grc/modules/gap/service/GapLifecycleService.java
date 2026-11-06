package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Gap overdue contributor chase (#60 GAP_NOT_RESPONDED). */
@Service
public class GapLifecycleService {

    private final WorkTaskRepository workTaskRepository;
    private final GapItemRepository gapItemRepository;
    private final NotificationService notificationService;

    public GapLifecycleService(WorkTaskRepository workTaskRepository,
                               GapItemRepository gapItemRepository,
                               NotificationService notificationService) {
        this.workTaskRepository = workTaskRepository;
        this.gapItemRepository = gapItemRepository;
        this.notificationService = notificationService;
    }

    public record LifecycleRunResult(int notResponded) {
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        int count = 0;
        for (WorkTask task : workTaskRepository.findGapOverdueNotNotified(day)) {
            GapItem item = gapItemRepository.findById(task.getSourceRecordId()).orElse(null);
            if (item == null) {
                continue;
            }
            List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
            recipients.add(task.getAssignedToUserId().toString());
            Map<String, String> data = new HashMap<>();
            data.put("nodeCode", item.getNodeCode());
            data.put("title", item.getTitle());
            data.put("dueDate", task.getDueDate() != null ? task.getDueDate().toString() : "-");
            notificationService.trigger("GAP_NOT_RESPONDED", String.valueOf(item.getId()),
                    recipients, data);
            task.setNotRespondedNotifiedAt(Instant.now());
            workTaskRepository.save(task);
            count++;
        }
        return new LifecycleRunResult(count);
    }
}
