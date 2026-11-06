package com.a9.arbor.grc.modules.dashboard.service;

import com.a9.arbor.grc.modules.dashboard.web.dto.ContributorDashboardResponse;
import com.a9.arbor.grc.modules.dashboard.web.dto.ContributorDashboardResponse.AgingBucket;
import com.a9.arbor.grc.modules.dashboard.web.dto.ContributorDashboardResponse.ModuleCount;
import com.a9.arbor.grc.modules.dashboard.web.dto.ContributorDashboardResponse.QueueItem;
import com.a9.arbor.grc.modules.dashboard.web.dto.ContributorDashboardResponse.WeekPoint;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapItemReviewStatus;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.risk.domain.AcceptanceFormStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskAcceptanceForm;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.repo.RiskAcceptanceFormRepository;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import com.a9.arbor.grc.modules.task.web.dto.WorkTaskSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Personal workload dashboard for Contributor-only users. Aggregates {@code my-tasks}
 * (assigned-to-me). Never uses Admin org-posture widgets.
 */
@Service
public class ContributorDashboardService {

    private static final int QUEUE_CAP = 200;
    private static final String EMPTY_DASH = "-";
    /** Register outcomes that are done. They are not waiting on Admin. */
    private static final Set<String> FINISHED_RISK = Set.of(
            RiskStatus.AVOIDED.name(),
            RiskStatus.TRANSFERRED.name(),
            RiskStatus.TOLERATED.name(),
            RiskStatus.ACCEPTED.name(),
            RiskStatus.CLOSED.name(),
            RiskStatus.AUTO_CLOSED.name(),
            RiskStatus.CANCELLED.name());

    private final MyTasksService myTasksService;
    private final RiskAcceptanceFormRepository acceptanceFormRepository;
    private final GapItemRepository gapItemRepository;

    public ContributorDashboardService(MyTasksService myTasksService,
                                       RiskAcceptanceFormRepository acceptanceFormRepository,
                                       GapItemRepository gapItemRepository) {
        this.myTasksService = myTasksService;
        this.acceptanceFormRepository = acceptanceFormRepository;
        this.gapItemRepository = gapItemRepository;
    }

    @Transactional(readOnly = true)
    public ContributorDashboardResponse summary(User user) {
        if (!RoleAccess.isContributorOnly(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contributor dashboard is for Contributor users only");
        }
        LocalDate today = LocalDate.now();
        List<WorkTaskSummaryResponse> tasks = myTasksService.listMine(user);
        Map<Long, RiskAcceptanceForm> formsByRisk = loadAcceptanceForms(tasks);
        Map<Long, GapItemReviewStatus> gapReviewByItem = loadGapReviews(tasks);

        long open = 0;
        long overdue = 0;
        long dueThisWeek = 0;
        long returned = 0;
        long submitted = 0;
        Map<String, ModuleAcc> modules = new LinkedHashMap<>();
        long[] aging = new long[5];
        List<QueueItem> queue = new ArrayList<>();
        Map<LocalDate, Long> submittedByWeek = new LinkedHashMap<>();
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 5; i >= 0; i--) {
            submittedByWeek.put(thisMonday.minusWeeks(i), 0L);
        }

        for (WorkTaskSummaryResponse t : tasks) {
            boolean gapTask = isGapItemTask(t);
            GapItemReviewStatus gapReview = gapTask && t.sourceRecordId() != null
                    ? gapReviewByItem.get(t.sourceRecordId()) : null;
            boolean gapFinal = gapReview == GapItemReviewStatus.FINALIZED;
            boolean taskOpen = t.status() == WorkTaskStatus.PENDING || t.status() == WorkTaskStatus.IN_PROGRESS;
            boolean isSubmitted = t.status() == WorkTaskStatus.SUBMITTED;
            // Final means the admin has locked the control. That is Closed, even when the
            // contributor task is still Pending because they never submitted.
            boolean finished = gapTask ? gapFinal : isSubmitted && isFinishedRisk(t.riskStatus());
            // Waiting is only a submitted control the admin has not finalized or sent back.
            boolean waitingOnAdmin = gapTask
                    ? isSubmitted && gapReview == GapItemReviewStatus.SUBMITTED
                    : isSubmitted && !finished;
            boolean isOpen = !finished && !waitingOnAdmin && (taskOpen || (gapTask && isSubmitted));
            boolean isOverdue = isOpen && t.dueDate() != null && t.dueDate().isBefore(today);
            boolean isDueWeek = isOpen && t.dueDate() != null
                    && !t.dueDate().isBefore(today)
                    && !t.dueDate().isAfter(today.plusDays(6));
            boolean isReturned = isOpen && t.returned();

            if (isOpen) {
                open++;
            }
            if (isOverdue) {
                overdue++;
            }
            if (isDueWeek) {
                dueThisWeek++;
            }
            if (isReturned) {
                returned++;
            }
            if (waitingOnAdmin) {
                submitted++;
            }

            ModuleSpec spec = moduleSpec(t);
            ModuleAcc acc = modules.computeIfAbsent(spec.key(), k -> new ModuleAcc(spec));
            if (isOpen) {
                acc.open++;
            }
            if (isOverdue) {
                acc.overdue++;
            }

            if (isOpen) {
                aging[agingIndex(today, t.dueDate())]++;
            }

            String waitingOn = waitingOnAdmin ? waitingOnLabel(t, formsByRisk) : null;

            if (isOpen || waitingOnAdmin || finished) {
                queue.add(toQueueItem(t, spec, isOverdue, isDueWeek, waitingOnAdmin, finished, waitingOn));
            }

            if (isSubmitted && t.submittedAt() != null) {
                LocalDate submittedDay = t.submittedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate week = submittedDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                submittedByWeek.computeIfPresent(week, (k, v) -> v + 1);
            }
        }

        queue.sort(queueOrder());
        if (queue.size() > QUEUE_CAP) {
            queue = new ArrayList<>(queue.subList(0, QUEUE_CAP));
        }

        List<ModuleCount> byModule = modules.values().stream()
                .filter(m -> m.open > 0)
                .map(m -> new ModuleCount(m.spec.moduleCode(), m.spec.label(), m.spec.navTarget(), m.open, m.overdue))
                .toList();

        List<AgingBucket> agingBuckets = List.of(
                new AgingBucket("NOT_DUE", "Not due (8d+)", aging[0]),
                new AgingBucket("DUE_4_7", "Due in 4-7d", aging[1]),
                new AgingBucket("DUE_0_3", "Due in 0-3d", aging[2]),
                new AgingBucket("OVERDUE_1_7", "Overdue 1-7d", aging[3]),
                new AgingBucket("OVERDUE_8_PLUS", "Overdue 8d+", aging[4]));

        List<WeekPoint> weeks = submittedByWeek.entrySet().stream()
                .map(e -> new WeekPoint(e.getKey().toString(), e.getValue()))
                .toList();

        return new ContributorDashboardResponse(
                Instant.now(),
                open,
                overdue,
                dueThisWeek,
                returned,
                submitted,
                byModule,
                agingBuckets,
                queue,
                weeks);
    }

    private static QueueItem toQueueItem(
            WorkTaskSummaryResponse t,
            ModuleSpec spec,
            boolean overdue,
            boolean dueThisWeek,
            boolean waitingOnAdmin,
            boolean finished,
            String waitingOn) {
        String project = t.projectName() == null || t.projectName().isBlank() ? EMPTY_DASH : t.projectName();
        String by = t.assignedByName() == null || t.assignedByName().isBlank() ? EMPTY_DASH : t.assignedByName();
        return new QueueItem(
                t.id(),
                t.title(),
                spec.label(),
                spec.navTarget(),
                project,
                by,
                t.dueDate(),
                t.status().name(),
                t.returned(),
                overdue,
                dueThisWeek,
                waitingOnAdmin,
                finished,
                waitingOn,
                agingLabel(LocalDate.now(), t.dueDate(), waitingOn, finished));
    }

    private Map<Long, RiskAcceptanceForm> loadAcceptanceForms(List<WorkTaskSummaryResponse> tasks) {
        Set<Long> riskIds = new HashSet<>();
        for (WorkTaskSummaryResponse t : tasks) {
            if (isAcceptanceRiskTask(t) && t.sourceRecordId() != null) {
                riskIds.add(t.sourceRecordId());
            }
        }
        if (riskIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RiskAcceptanceForm> byRisk = new HashMap<>();
        for (RiskAcceptanceForm form : acceptanceFormRepository.findByRiskIdIn(riskIds)) {
            if (form.getRiskId() != null) {
                byRisk.put(form.getRiskId(), form);
            }
        }
        return byRisk;
    }

    /** Review status for every gap task, including ones the contributor has not submitted. */
    private Map<Long, GapItemReviewStatus> loadGapReviews(List<WorkTaskSummaryResponse> tasks) {
        Set<Long> itemIds = new HashSet<>();
        for (WorkTaskSummaryResponse t : tasks) {
            if (isGapItemTask(t) && t.sourceRecordId() != null) {
                itemIds.add(t.sourceRecordId());
            }
        }
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, GapItemReviewStatus> reviews = new HashMap<>();
        for (GapItem item : gapItemRepository.findAllById(itemIds)) {
            reviews.put(item.getId(), item.getReviewStatus());
        }
        return reviews;
    }

    private static boolean isGapItemTask(WorkTaskSummaryResponse t) {
        return MyTasksService.MODULE_GAP.equals(t.sourceModule())
                && MyTasksService.TYPE_GAP_ITEM.equals(t.sourceRecordType());
    }

    private static boolean isAcceptanceRiskTask(WorkTaskSummaryResponse t) {
        String type = t.sourceRecordType();
        return MyTasksService.TYPE_RISK_RECORD.equals(type)
                || MyTasksService.TYPE_RISK_ACCEPTANCE.equals(type);
    }

    /** Who holds a submitted risk now. Form status names the acceptance turn. */
    private static String waitingOnLabel(WorkTaskSummaryResponse t, Map<Long, RiskAcceptanceForm> formsByRisk) {
        if (!isAcceptanceRiskTask(t) || t.sourceRecordId() == null) {
            return "Admin";
        }
        RiskAcceptanceForm form = formsByRisk.get(t.sourceRecordId());
        AcceptanceFormStatus status = form == null ? null : form.getStatus();
        if (status == AcceptanceFormStatus.PENDING_RISK_OWNER) {
            return "Risk Owner";
        }
        if (status == AcceptanceFormStatus.PENDING_APPROVER_2) {
            return "ERM".equalsIgnoreCase(t.riskType()) ? "ERM Head" : "CISO";
        }
        if (status == AcceptanceFormStatus.PENDING_APPROVER_3) {
            return "Level 3";
        }
        return "Admin";
    }

    private static boolean isFinishedRisk(String riskStatus) {
        return riskStatus != null && FINISHED_RISK.contains(riskStatus);
    }

    private static Comparator<QueueItem> queueOrder() {
        return Comparator
                .comparingInt((QueueItem q) -> q.finished() ? 5
                        : q.waitingOnAdmin() ? 4
                        : q.returned() ? 0
                        : q.overdue() ? 1
                        : q.dueThisWeek() ? 2
                        : 3)
                .thenComparing(QueueItem::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(QueueItem::taskId);
    }

    /** 0 not-due/no-date, 1 due 4-7, 2 due 0-3, 3 overdue 1-7, 4 overdue 8+. */
    private static int agingIndex(LocalDate today, LocalDate due) {
        if (due == null) {
            return 0;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, due);
        if (days <= -8) {
            return 4;
        }
        if (days < 0) {
            return 3;
        }
        if (days <= 3) {
            return 2;
        }
        if (days <= 7) {
            return 1;
        }
        return 0;
    }

    private static String agingLabel(LocalDate today, LocalDate due, String waitingOn, boolean finished) {
        if (finished) {
            return "Closed";
        }
        if (waitingOn != null) {
            return waitingOn;
        }
        if (due == null) {
            return "No due date";
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, due);
        if (days < 0) {
            long overdue = -days;
            return overdue == 1 ? "1d overdue" : overdue + "d overdue";
        }
        if (days == 0) {
            return "Due today";
        }
        if (days == 1) {
            return "Due in 1d";
        }
        return "Due in " + days + "d";
    }

    private static ModuleSpec moduleSpec(WorkTaskSummaryResponse t) {
        String module = t.sourceModule() == null ? "" : t.sourceModule();
        String type = t.sourceRecordType() == null ? "" : t.sourceRecordType();
        if (MyTasksService.MODULE_GAP.equals(module)) {
            return new ModuleSpec("GAP", "Gap", "assessments");
        }
        if (MyTasksService.MODULE_RISK.equals(module)) {
            if ("ERM".equalsIgnoreCase(t.riskType())) {
                return new ModuleSpec("ERM", "Enterprise Risk", "erm-risk");
            }
            return new ModuleSpec("IS_RISK", "IS Risk", "risk");
        }
        if (MyTasksService.MODULE_CAPA.equals(module)) {
            return new ModuleSpec("CAPA", "CAPA", "audit-capa");
        }
        if (MyTasksService.MODULE_BCMS.equals(module)) {
            return new ModuleSpec("BIA", "BIA", "bcms-bia");
        }
        if (MyTasksService.MODULE_POLICY.equals(module)) {
            return new ModuleSpec("POLICY", "Policy", "policies");
        }
        if (MyTasksService.MODULE_KRI.equals(module)) {
            return new ModuleSpec("KRI", "KRI", "kri");
        }
        if (MyTasksService.MODULE_INCIDENT.equals(module)) {
            return new ModuleSpec("INCIDENT", "Incidents", "incidents");
        }
        if (MyTasksService.MODULE_PRIVACY.equals(module) && MyTasksService.TYPE_PRIVACY_DPIA.equals(type)) {
            return new ModuleSpec("DPIA", "DPIA", "privacy-dpia");
        }
        if (MyTasksService.MODULE_PRIVACY.equals(module)) {
            return new ModuleSpec("ROPA", "RoPA", "privacy-ropa");
        }
        return new ModuleSpec(module.isBlank() ? "OTHER" : module, "Assigned work", "contributor-dashboard");
    }

    private record ModuleSpec(String key, String moduleCode, String label, String navTarget) {
        ModuleSpec(String moduleCode, String label, String navTarget) {
            this(moduleCode, moduleCode, label, navTarget);
        }
    }

    private static final class ModuleAcc {
        private final ModuleSpec spec;
        private long open;
        private long overdue;

        private ModuleAcc(ModuleSpec spec) {
            this.spec = spec;
        }
    }
}
