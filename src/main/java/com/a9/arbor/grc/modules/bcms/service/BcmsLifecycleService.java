package com.a9.arbor.grc.modules.bcms.service;

import com.a9.arbor.grc.modules.bcms.domain.BcmsExercise;
import com.a9.arbor.grc.modules.bcms.domain.BiaCycle;
import com.a9.arbor.grc.modules.bcms.domain.BiaStatus;
import com.a9.arbor.grc.modules.bcms.domain.ExerciseStatus;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlan;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanStatus;
import com.a9.arbor.grc.modules.bcms.repo.BcmsExerciseRepository;
import com.a9.arbor.grc.modules.bcms.repo.BiaCycleRepository;
import com.a9.arbor.grc.modules.bcms.repo.RecoveryPlanRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** BCMS date-based notification triggers (#47–57). */
@Service
public class BcmsLifecycleService {

    private static final int BIA_BCP_REVIEW_WARN_DAYS = 30;
    private static final int EXERCISE_DUE_WARN_DAYS = 7;

    private final BiaCycleRepository biaCycleRepository;
    private final RecoveryPlanRepository recoveryPlanRepository;
    private final BcmsExerciseRepository exerciseRepository;
    private final NotificationService notificationService;

    public BcmsLifecycleService(BiaCycleRepository biaCycleRepository,
                                RecoveryPlanRepository recoveryPlanRepository,
                                BcmsExerciseRepository exerciseRepository,
                                NotificationService notificationService) {
        this.biaCycleRepository = biaCycleRepository;
        this.recoveryPlanRepository = recoveryPlanRepository;
        this.exerciseRepository = exerciseRepository;
        this.notificationService = notificationService;
    }

    public record LifecycleRunResult(
            int biaNotSubmitted,
            int biaReviewDue,
            int bcpReviewDue,
            int bcpExpired,
            int exerciseDue,
            int exerciseOverdue) {
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        int biaNotSubmitted = 0;
        int biaReviewDue = 0;
        int bcpReviewDue = 0;
        int bcpExpired = 0;
        int exerciseDue = 0;
        int exerciseOverdue = 0;

        for (BiaCycle bia : biaCycleRepository.findAll()) {
            if ((bia.getStatus() == BiaStatus.ASSIGNED || bia.getStatus() == BiaStatus.IN_PROGRESS)
                    && bia.getDueDate() != null
                    && bia.getDueDate().isBefore(day)
                    && bia.getNotSubmittedNotifiedAt() == null) {
                List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
                if (bia.getAssignedToUserId() != null) {
                    recipients.add(bia.getAssignedToUserId().toString());
                }
                notificationService.trigger("BIA_NOT_SUBMITTED", String.valueOf(bia.getId()),
                        recipients, biaData(bia));
                bia.setNotSubmittedNotifiedAt(Instant.now());
                biaCycleRepository.save(bia);
                biaNotSubmitted++;
            }
            if (bia.getNextReviewDate() != null
                    && bia.getReviewDueNotifiedAt() == null
                    && ChronoUnit.DAYS.between(day, bia.getNextReviewDate()) == BIA_BCP_REVIEW_WARN_DAYS) {
                List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
                notificationService.trigger("BIA_REVIEW_DUE", String.valueOf(bia.getId()),
                        recipients, biaData(bia));
                bia.setReviewDueNotifiedAt(Instant.now());
                biaCycleRepository.save(bia);
                biaReviewDue++;
            }
        }

        for (RecoveryPlan plan : recoveryPlanRepository.findAll()) {
            if (plan.getReviewDate() != null
                    && plan.getReviewDueNotifiedAt() == null
                    && ChronoUnit.DAYS.between(day, plan.getReviewDate()) == BIA_BCP_REVIEW_WARN_DAYS) {
                List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
                if (plan.getOwnerUserId() != null) {
                    recipients.add(plan.getOwnerUserId().toString());
                }
                notificationService.trigger("BCP_REVIEW_DUE", String.valueOf(plan.getId()),
                        recipients, planData(plan));
                plan.setReviewDueNotifiedAt(Instant.now());
                recoveryPlanRepository.save(plan);
                bcpReviewDue++;
            }
            if (plan.getReviewDate() != null
                    && plan.getReviewDate().isBefore(day)
                    && plan.getExpiredNotifiedAt() == null
                    && plan.getStatus() == RecoveryPlanStatus.APPROVED) {
                List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
                if (plan.getOwnerUserId() != null) {
                    recipients.add(plan.getOwnerUserId().toString());
                }
                notificationService.trigger("BCP_EXPIRED", String.valueOf(plan.getId()),
                        recipients, planData(plan));
                plan.setStatus(RecoveryPlanStatus.EXPIRED);
                plan.setExpiredNotifiedAt(Instant.now());
                recoveryPlanRepository.save(plan);
                bcpExpired++;
            }
        }

        for (BcmsExercise ex : exerciseRepository.findAll()) {
            if (ex.getStatus() != ExerciseStatus.PLANNED || ex.getScheduledDate() == null) {
                continue;
            }
            long daysUntil = ChronoUnit.DAYS.between(day, ex.getScheduledDate());
            if (daysUntil == EXERCISE_DUE_WARN_DAYS && ex.getDueNotifiedAt() == null) {
                List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
                if (ex.getFacilitatorUserId() != null) {
                    recipients.add(ex.getFacilitatorUserId().toString());
                }
                notificationService.trigger("EXERCISE_DUE", String.valueOf(ex.getId()),
                        recipients, exerciseData(ex));
                ex.setDueNotifiedAt(Instant.now());
                exerciseRepository.save(ex);
                exerciseDue++;
            }
            if (daysUntil < 0 && ex.getOverdueNotifiedAt() == null) {
                List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
                if (ex.getFacilitatorUserId() != null) {
                    recipients.add(ex.getFacilitatorUserId().toString());
                }
                notificationService.trigger("EXERCISE_OVERDUE", String.valueOf(ex.getId()),
                        recipients, exerciseData(ex));
                ex.setOverdueNotifiedAt(Instant.now());
                exerciseRepository.save(ex);
                exerciseOverdue++;
            }
        }

        return new LifecycleRunResult(biaNotSubmitted, biaReviewDue, bcpReviewDue, bcpExpired, exerciseDue, exerciseOverdue);
    }

    private static Map<String, String> biaData(BiaCycle bia) {
        Map<String, String> data = new HashMap<>();
        data.put("processName", bia.getDisplayId());
        data.put("dueDate", bia.getDueDate() != null ? bia.getDueDate().toString() : "-");
        data.put("reviewDate", bia.getNextReviewDate() != null ? bia.getNextReviewDate().toString() : "-");
        return data;
    }

    private static Map<String, String> planData(RecoveryPlan plan) {
        Map<String, String> data = new HashMap<>();
        data.put("planName", plan.getName());
        data.put("reviewDate", plan.getReviewDate() != null ? plan.getReviewDate().toString() : "-");
        return data;
    }

    private static Map<String, String> exerciseData(BcmsExercise ex) {
        Map<String, String> data = new HashMap<>();
        data.put("exerciseName", ex.getName());
        data.put("scheduledDate", ex.getScheduledDate() != null ? ex.getScheduledDate().toString() : "-");
        return data;
    }
}
