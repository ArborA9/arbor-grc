package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.TreatmentPlan;
import com.a9.arbor.grc.modules.risk.domain.TreatmentPlanStatus;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.risk.repo.TreatmentPlanRepository;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Daily risk lifecycle: Treat OVERDUE, assignment/owner chase, overdue escalation,
 * Tolerate review window / return, Excel Next Review Date (AL) 30-day reminders.
 */
@Service
public class RiskLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(RiskLifecycleService.class);

    private final RiskRecordRepository riskRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final RiskAdminRecipients riskAdminRecipients;

    public RiskLifecycleService(RiskRecordRepository riskRepository,
                                TreatmentPlanRepository treatmentPlanRepository,
                                WorkTaskRepository workTaskRepository,
                                NotificationService notificationService,
                                AuditTrailService auditTrailService,
                                RiskAdminRecipients riskAdminRecipients) {
        this.riskAdminRecipients = riskAdminRecipients;
        this.riskRepository = riskRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.workTaskRepository = workTaskRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    public record LifecycleRunResult(
            int overdueMarked,
            int reviewReminders,
            int toleranceReturned,
            int assignmentChases,
            int ownerChases,
            int overdueEscalations
    ) {
    }

    @Transactional
    public LifecycleRunResult processCurrentTenant(LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        int overdue = processOverdue(day);
        int reminders = processToleranceReminders(day) + processNextReviewReminders(day);
        int returned = processToleranceReturns(day);
        int assignmentChases = processAssignmentChases(day);
        int ownerChases = processOwnerChases(day);
        int escalations = processOverdueEscalations(day);
        log.info("Risk lifecycle asOf={}: overdue={}, reminders={}, returned={}, assignChase={}, ownerChase={}, escalate={}",
                day, overdue, reminders, returned, assignmentChases, ownerChases, escalations);
        return new LifecycleRunResult(overdue, reminders, returned, assignmentChases, ownerChases, escalations);
    }

    private int processAssignmentChases(LocalDate asOf) {
        int count = 0;
        Instant cutoff = asOf.minusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC);
        for (RiskRecord risk : riskRepository.findByStatus(RiskStatus.ASSIGNED)) {
            if (risk.getAssignedAt() == null || risk.getAssignmentChaseSentAt() != null) {
                continue;
            }
            if (risk.getAssignedAt().isAfter(cutoff)) {
                continue;
            }
            risk.setAssignmentChaseSentAt(Instant.now());
            riskRepository.save(risk);
            List<String> recipients = new ArrayList<>(riskAdminRecipients.forRisk(risk));
            if (risk.getAssignedContributorId() != null) {
                recipients.add(String.valueOf(risk.getAssignedContributorId()));
            }
            notificationService.trigger(
                    "RISK_NOT_RESPONDED",
                    String.valueOf(risk.getId()),
                    recipients,
                    Map.of("displayId", risk.getDisplayId(), "title", risk.getTitle()));
            count++;
        }
        return count;
    }

    private int processOwnerChases(LocalDate asOf) {
        int count = 0;
        Instant cutoff = asOf.minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC);
        for (RiskRecord risk : riskRepository.findByStatus(RiskStatus.PENDING_ACCEPTANCE)) {
            if (risk.getOwnerChaseSentAt() != null) {
                continue;
            }
            // Use updatedAt as proxy for when form was sent to owner (status entered PENDING_ACCEPTANCE)
            if (risk.getUpdatedAt() == null || risk.getUpdatedAt().isAfter(cutoff)) {
                continue;
            }
            risk.setOwnerChaseSentAt(Instant.now());
            riskRepository.save(risk);
            List<String> recipients = new ArrayList<>(riskAdminRecipients.forRisk(risk));
            if (risk.getRiskOwnerUserId() != null) {
                recipients.add(String.valueOf(risk.getRiskOwnerUserId()));
            }
            notificationService.trigger(
                    "RISK_OWNER_NOT_RESPONDED",
                    String.valueOf(risk.getId()),
                    recipients,
                    Map.of("displayId", risk.getDisplayId(), "title", risk.getTitle()));
            count++;
        }
        return count;
    }

    private int processOverdueEscalations(LocalDate asOf) {
        int count = 0;
        Instant cutoff = asOf.minusDays(7).atStartOfDay().toInstant(ZoneOffset.UTC);
        for (RiskRecord risk : riskRepository.findByStatus(RiskStatus.OVERDUE)) {
            if (risk.getOverdueEscalationSentAt() != null) {
                continue;
            }
            Instant overdueSince = risk.getOverdueNotifiedAt() != null
                    ? risk.getOverdueNotifiedAt()
                    : risk.getUpdatedAt();
            if (overdueSince == null || overdueSince.isAfter(cutoff)) {
                continue;
            }
            risk.setOverdueEscalationSentAt(Instant.now());
            riskRepository.save(risk);
            notificationService.trigger(
                    "RISK_OVERDUE_ESCALATION",
                    String.valueOf(risk.getId()),
                    riskAdminRecipients.forRisk(risk),
                    Map.of("displayId", risk.getDisplayId(), "title", risk.getTitle()));
            count++;
        }
        return count;
    }

    private int processOverdue(LocalDate asOf) {
        int count = 0;
        for (TreatmentPlan plan : treatmentPlanRepository.findPastTargetNotCompleted(asOf)) {
            RiskRecord risk = riskRepository.findById(plan.getRiskId()).orElse(null);
            if (risk == null) {
                continue;
            }
            if (risk.getStatus() != RiskStatus.MITIGATING && risk.getStatus() != RiskStatus.OVERDUE) {
                continue;
            }
            if (risk.getStatus() == RiskStatus.MITIGATING) {
                risk.setStatus(RiskStatus.OVERDUE);
            }
            if (plan.getStatus() != TreatmentPlanStatus.OVERDUE
                    && plan.getStatus() != TreatmentPlanStatus.COMPLETED) {
                plan.setStatus(TreatmentPlanStatus.OVERDUE);
                treatmentPlanRepository.save(plan);
            }
            if (risk.getOverdueNotifiedAt() == null) {
                risk.setOverdueNotifiedAt(Instant.now());
                List<String> recipients = new ArrayList<>(riskAdminRecipients.forRisk(risk));
                if (plan.getActionOwnerId() != null) {
                    recipients.add(plan.getActionOwnerId().toString());
                }
                if (risk.getAssignedContributorId() != null) {
                    recipients.add(risk.getAssignedContributorId().toString());
                }
                notificationService.trigger(
                        "TREATMENT_OVERDUE",
                        String.valueOf(risk.getId()),
                        recipients,
                        Map.of(
                                "displayId", risk.getDisplayId(),
                                "title", risk.getTitle(),
                                "targetDate", plan.getTargetDate().toString()));
                auditTrailService.record(
                        AuditActionType.UPDATED, "RISK", null, "SYSTEM",
                        "RISK_RECORD", String.valueOf(risk.getId()), risk.getDisplayId(),
                        "status", "MITIGATING", "OVERDUE", null, null);
            }
            riskRepository.save(risk);
            count++;
        }
        return count;
    }

    private int processToleranceReminders(LocalDate asOf) {
        int count = 0;
        for (RiskRecord risk : riskRepository.findByStatus(RiskStatus.TOLERATED)) {
            if (risk.getNextReviewDate() == null || risk.getReviewReminderSentAt() != null) {
                continue;
            }
            LocalDate windowStart = risk.getNextReviewDate().minusDays(30);
            if (asOf.isBefore(windowStart) || !asOf.isBefore(risk.getNextReviewDate())) {
                continue;
            }
            risk.setReviewReminderSentAt(Instant.now());
            riskRepository.save(risk);
            List<String> recipients = new ArrayList<>(riskAdminRecipients.forRisk(risk));
            if (risk.getRiskOwnerUserId() != null) {
                recipients.add(risk.getRiskOwnerUserId().toString());
            }
            notificationService.trigger(
                    "RISK_TOLERANCE_REVIEW_DUE",
                    String.valueOf(risk.getId()),
                    recipients,
                    Map.of(
                            "displayId", risk.getDisplayId(),
                            "title", risk.getTitle(),
                            "reviewDate", risk.getNextReviewDate().toString()));
            count++;
        }
        return count;
    }

    /** Excel AL (Next Review Date) — 30-day window. Does not reopen ACCEPTED. */
    private int processNextReviewReminders(LocalDate asOf) {
        int count = 0;
        List<RiskStatus> statuses = List.of(
                RiskStatus.ACCEPTED,
                RiskStatus.MITIGATING,
                RiskStatus.OVERDUE,
                RiskStatus.AUTO_CLOSED,
                RiskStatus.AVOIDED,
                RiskStatus.TRANSFERRED,
                RiskStatus.CLOSED);
        for (RiskStatus status : statuses) {
            for (RiskRecord risk : riskRepository.findByStatus(status)) {
                if (risk.getNextReviewDate() == null || risk.getReviewReminderSentAt() != null) {
                    continue;
                }
                LocalDate windowStart = risk.getNextReviewDate().minusDays(30);
                if (asOf.isBefore(windowStart) || !asOf.isBefore(risk.getNextReviewDate())) {
                    continue;
                }
                risk.setReviewReminderSentAt(Instant.now());
                riskRepository.save(risk);
                List<String> recipients = new ArrayList<>(riskAdminRecipients.forRisk(risk));
                if (risk.getRiskOwnerUserId() != null) {
                    recipients.add(risk.getRiskOwnerUserId().toString());
                }
                if (risk.getAssignedContributorId() != null) {
                    recipients.add(risk.getAssignedContributorId().toString());
                }
                notificationService.trigger(
                        "RISK_REVIEW_DUE",
                        String.valueOf(risk.getId()),
                        recipients,
                        Map.of(
                                "displayId", risk.getDisplayId(),
                                "title", risk.getTitle(),
                                "reviewDate", risk.getNextReviewDate().toString()));
                count++;
            }
        }
        return count;
    }

    private int processToleranceReturns(LocalDate asOf) {
        int count = 0;
        for (RiskRecord risk : riskRepository.findByStatus(RiskStatus.TOLERATED)) {
            if (risk.getNextReviewDate() == null || asOf.isBefore(risk.getNextReviewDate())) {
                continue;
            }
            String reviewDate = risk.getNextReviewDate().toString();
            risk.setStatus(RiskStatus.ASSIGNED);
            risk.setAssignedAt(Instant.now());
            risk.setNextReviewDate(null);
            risk.setReviewReminderSentAt(null);
            risk.setAssignmentChaseSentAt(null);
            risk.setClosedAt(null);
            reopenContributorTask(risk);
            riskRepository.save(risk);

            List<String> recipients = new ArrayList<>(riskAdminRecipients.forRisk(risk));
            if (risk.getAssignedContributorId() != null) {
                recipients.add(risk.getAssignedContributorId().toString());
            }
            if (risk.getRiskOwnerUserId() != null) {
                recipients.add(risk.getRiskOwnerUserId().toString());
            }
            notificationService.trigger(
                    "RISK_TOLERANCE_RETURNED",
                    String.valueOf(risk.getId()),
                    recipients,
                    Map.of(
                            "displayId", risk.getDisplayId(),
                            "title", risk.getTitle(),
                            "reviewDate", reviewDate));
            auditTrailService.record(
                    AuditActionType.UPDATED, "RISK", null, "SYSTEM",
                    "RISK_RECORD", String.valueOf(risk.getId()), risk.getDisplayId(),
                    "status", "TOLERATED", "ASSIGNED", null, null);
            count++;
        }
        return count;
    }

    private void reopenContributorTask(RiskRecord risk) {
        if (risk.getAssignedContributorId() == null) {
            return;
        }
        WorkTask task = risk.getWorkTaskId() != null
                ? workTaskRepository.findById(risk.getWorkTaskId()).orElse(null)
                : workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_RECORD, risk.getId())
                .orElse(null);
        if (task == null) {
            task = new WorkTask();
            task.setSourceModule(MyTasksService.MODULE_RISK);
            task.setSourceRecordType(MyTasksService.TYPE_RISK_RECORD);
            task.setSourceRecordId(risk.getId());
            task.setTitle(risk.getDisplayId() + ": " + risk.getTitle());
            task.setDetail("Tolerance review: re-evaluate");
            task.setAssignedByUserId(risk.getCreatedByUserId());
        }
        task.setAssignedToUserId(risk.getAssignedContributorId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setSubmittedAt(null);
        workTaskRepository.save(task);
        risk.setWorkTaskId(task.getId());
    }
}
