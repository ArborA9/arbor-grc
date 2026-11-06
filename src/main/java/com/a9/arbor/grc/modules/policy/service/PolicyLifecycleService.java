package com.a9.arbor.grc.modules.policy.service;

import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.policy.domain.Policy;
import com.a9.arbor.grc.modules.policy.domain.PolicyStatus;
import com.a9.arbor.grc.modules.policy.repo.PolicyAcknowledgementRepository;
import com.a9.arbor.grc.modules.policy.repo.PolicyRepository;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PolicyLifecycleService {

    public record LifecycleRunResult(int reviewDue, int expired, int ackDeadline, int ackOverdue) {
    }

    private final PolicyRepository policyRepository;
    private final PolicyAcknowledgementRepository acknowledgementRepository;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationService notificationService;

    public PolicyLifecycleService(PolicyRepository policyRepository,
                                  PolicyAcknowledgementRepository acknowledgementRepository,
                                  WorkTaskRepository workTaskRepository,
                                  NotificationService notificationService) {
        this.policyRepository = policyRepository;
        this.acknowledgementRepository = acknowledgementRepository;
        this.workTaskRepository = workTaskRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(LocalDate asOf) {
        int reviewDue = 0;
        int expired = 0;
        int ackDeadline = 0;
        int ackOverdue = 0;

        LocalDate warnFrom = asOf;
        LocalDate warnTo = asOf.plusDays(30);

        for (Policy p : policyRepository.findByStatusAndReviewDateBetween(
                PolicyStatus.APPROVED, warnFrom, warnTo)) {
            if (p.getReviewDueNotifiedAt() != null) {
                continue;
            }
            if (p.getReviewDate() == null) {
                continue;
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(asOf, p.getReviewDate());
            if (days > 30 || days < 0) {
                continue;
            }
            Map<String, String> data = data(p);
            notificationService.trigger("POLICY_REVIEW_DUE", String.valueOf(p.getId()),
                    List.of("OWNER", "ADMIN"), data);
            p.setReviewDueNotifiedAt(Instant.now());
            policyRepository.save(p);
            reviewDue++;
        }

        for (Policy p : policyRepository.findByStatusAndReviewDateLessThanEqual(
                PolicyStatus.APPROVED, asOf.minusDays(1))) {
            if (p.getExpiredNotifiedAt() != null && p.getStatus() == PolicyStatus.DUE_FOR_REVIEW) {
                continue;
            }
            p.setStatus(PolicyStatus.DUE_FOR_REVIEW);
            p.setExpiredNotifiedAt(Instant.now());
            policyRepository.save(p);
            notificationService.trigger("POLICY_EXPIRED", String.valueOf(p.getId()),
                    List.of("OWNER", "ADMIN"), data(p));
            expired++;
        }

        for (Policy p : policyRepository.findByStatusAndAckRequiredTrueAndAckDeadlineNotNull(
                PolicyStatus.APPROVED)) {
            if (p.getAckDeadline() == null || p.getCurrentVersionId() == null) {
                continue;
            }
            long daysTo = java.time.temporal.ChronoUnit.DAYS.between(asOf, p.getAckDeadline());
            if (daysTo == 3 && p.getAckDeadlineNotifiedAt() == null) {
                List<String> pending = pendingAckUserIds(p);
                if (!pending.isEmpty()) {
                    notificationService.trigger("POLICY_ACK_DEADLINE", String.valueOf(p.getId()),
                            pending, data(p));
                }
                p.setAckDeadlineNotifiedAt(Instant.now());
                policyRepository.save(p);
                ackDeadline++;
            }
            if (!p.getAckDeadline().isAfter(asOf) && p.getAckOverdueNotifiedAt() == null) {
                List<String> pending = pendingAckUserIds(p);
                if (!pending.isEmpty()) {
                    notificationService.trigger("POLICY_ACK_OVERDUE", String.valueOf(p.getId()),
                            List.of("OWNER", "ADMIN"), data(p));
                }
                p.setAckOverdueNotifiedAt(Instant.now());
                policyRepository.save(p);
                ackOverdue++;
            }
        }

        return new LifecycleRunResult(reviewDue, expired, ackDeadline, ackOverdue);
    }

    private List<String> pendingAckUserIds(Policy p) {
        return workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndStatusNot(
                        MyTasksService.MODULE_POLICY, MyTasksService.TYPE_POLICY_ACK,
                        p.getId(), WorkTaskStatus.CANCELLED)
                .stream()
                .filter(t -> t.getStatus() != WorkTaskStatus.SUBMITTED)
                .filter(t -> !acknowledgementRepository.existsByPolicyVersionIdAndUserId(
                        p.getCurrentVersionId(), t.getAssignedToUserId()))
                .map(WorkTask::getAssignedToUserId)
                .map(String::valueOf)
                .distinct()
                .toList();
    }

    private Map<String, String> data(Policy p) {
        Map<String, String> data = new HashMap<>();
        data.put("title", p.getTitle());
        data.put("reviewDate", p.getReviewDate() != null ? p.getReviewDate().toString() : "-");
        data.put("ackDeadline", p.getAckDeadline() != null ? p.getAckDeadline().toString() : "-");
        data.put("version", "v" + p.getMajorVersion() + "." + p.getMinorVersion());
        return data;
    }
}
