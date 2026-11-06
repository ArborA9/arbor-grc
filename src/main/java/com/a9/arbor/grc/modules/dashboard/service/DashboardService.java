package com.a9.arbor.grc.modules.dashboard.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.FrameworkScoringSupport;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.audit.domain.AuditLog;
import com.a9.arbor.grc.modules.audit.repo.AuditLogRepository;
import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditStatus;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditFindingRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.bcms.domain.BiaReviewOutcome;
import com.a9.arbor.grc.modules.bcms.domain.ExerciseStatus;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanStatus;
import com.a9.arbor.grc.modules.bcms.repo.BcmsExerciseRepository;
import com.a9.arbor.grc.modules.bcms.repo.BiaProcessRepository;
import com.a9.arbor.grc.modules.bcms.repo.RecoveryPlanRepository;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.domain.CapaRegister;
import com.a9.arbor.grc.modules.capa.domain.CapaStatus;
import com.a9.arbor.grc.modules.capa.repo.CapaRepository;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceRepository;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.service.GapLayerScoreJson;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.kri.domain.Kri;
import com.a9.arbor.grc.modules.kri.domain.KriRag;
import com.a9.arbor.grc.modules.kri.domain.KriStatus;
import com.a9.arbor.grc.modules.kri.repo.KriRepository;
import com.a9.arbor.grc.modules.notification.repo.NotificationRepository;
import com.a9.arbor.grc.modules.policy.domain.PolicyStatus;
import com.a9.arbor.grc.modules.policy.repo.PolicyRepository;
import com.a9.arbor.grc.modules.privacy.domain.DsrStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDsr;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyDsrRepository;
import com.a9.arbor.grc.modules.risk.domain.PriorityLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.soa.domain.Soa;
import com.a9.arbor.grc.modules.soa.domain.SoaItem;
import com.a9.arbor.grc.modules.soa.domain.SoaStatus;
import com.a9.arbor.grc.modules.soa.repo.SoaItemRepository;
import com.a9.arbor.grc.modules.soa.repo.SoaRepository;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.tprm.domain.TprmCompliance;
import com.a9.arbor.grc.modules.tprm.domain.TprmCriticality;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.domain.VendorLifecycleState;
import com.a9.arbor.grc.modules.tprm.domain.VendorStatus;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorRepository;
import com.a9.arbor.grc.modules.dashboard.web.dto.DashboardSummaryResponse;
import com.a9.arbor.grc.modules.dashboard.web.dto.DashboardWidget;
import com.a9.arbor.grc.modules.dashboard.web.dto.RecentActivityItem;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the Overview dashboard (req §19.1/19.2). Platform surfaces (my tasks, notifications,
 * recent activity, evidence, policy) are always on; module widgets check {@link ModuleLicensingService}.
 * Every block is isolated in a try/catch so one module failure never blanks the whole dashboard.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    /** Same closed set as the register "Total open" KPI ({@code RiskService.KPI_CLOSED_STATUSES}). */
    private static final Set<RiskStatus> RISK_CLOSED = EnumSet.of(
            RiskStatus.CLOSED, RiskStatus.AUTO_CLOSED, RiskStatus.AVOIDED,
            RiskStatus.TRANSFERRED, RiskStatus.TOLERATED, RiskStatus.ACCEPTED, RiskStatus.CANCELLED);
    private static final Set<WorkTaskStatus> TASK_OPEN = EnumSet.of(WorkTaskStatus.PENDING, WorkTaskStatus.IN_PROGRESS);
    private static final Set<RiskLevel> CRITICAL_RISK = EnumSet.of(RiskLevel.HIGH, RiskLevel.VERY_HIGH);
    private static final Set<DsrStatus> DSR_OPEN = EnumSet.of(DsrStatus.RECEIVED, DsrStatus.IN_PROGRESS, DsrStatus.OVERDUE);

    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;
    private final UserService userService;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final RiskRecordRepository riskRecordRepository;
    private final CapaRepository capaRepository;
    private final SoaRepository soaRepository;
    private final GapAssessmentRepository gapAssessmentRepository;
    private final BiaProcessRepository biaProcessRepository;
    private final RecoveryPlanRepository recoveryPlanRepository;
    private final BcmsExerciseRepository bcmsExerciseRepository;
    private final GrcAuditRepository grcAuditRepository;
    private final GrcAuditFindingRepository grcAuditFindingRepository;
    private final PolicyRepository policyRepository;
    private final EvidenceRepository evidenceRepository;
    private final PrivacyDsrRepository privacyDsrRepository;
    private final TprmVendorRepository tprmVendorRepository;
    private final GapItemRepository gapItemRepository;
    private final SoaItemRepository soaItemRepository;
    private final KriRepository kriRepository;

    public DashboardService(ModuleLicensingService moduleLicensingService,
                            AdminModuleAccessService adminModuleAccessService,
                            UserService userService,
                            WorkTaskRepository workTaskRepository,
                            NotificationRepository notificationRepository,
                            AuditLogRepository auditLogRepository,
                            RiskRecordRepository riskRecordRepository,
                            CapaRepository capaRepository,
                            SoaRepository soaRepository,
                            GapAssessmentRepository gapAssessmentRepository,
                            BiaProcessRepository biaProcessRepository,
                            RecoveryPlanRepository recoveryPlanRepository,
                            BcmsExerciseRepository bcmsExerciseRepository,
                            GrcAuditRepository grcAuditRepository,
                            GrcAuditFindingRepository grcAuditFindingRepository,
                            PolicyRepository policyRepository,
                            EvidenceRepository evidenceRepository,
                            PrivacyDsrRepository privacyDsrRepository,
                            TprmVendorRepository tprmVendorRepository,
                            GapItemRepository gapItemRepository,
                            SoaItemRepository soaItemRepository,
                            KriRepository kriRepository) {
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
        this.userService = userService;
        this.workTaskRepository = workTaskRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.riskRecordRepository = riskRecordRepository;
        this.capaRepository = capaRepository;
        this.soaRepository = soaRepository;
        this.gapAssessmentRepository = gapAssessmentRepository;
        this.biaProcessRepository = biaProcessRepository;
        this.recoveryPlanRepository = recoveryPlanRepository;
        this.bcmsExerciseRepository = bcmsExerciseRepository;
        this.grcAuditRepository = grcAuditRepository;
        this.grcAuditFindingRepository = grcAuditFindingRepository;
        this.policyRepository = policyRepository;
        this.evidenceRepository = evidenceRepository;
        this.privacyDsrRepository = privacyDsrRepository;
        this.tprmVendorRepository = tprmVendorRepository;
        this.gapItemRepository = gapItemRepository;
        this.soaItemRepository = soaItemRepository;
        this.kriRepository = kriRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(User currentUser) {
        String tenantId = TenantContext.getTenantId();
        List<DashboardWidget> widgets = new ArrayList<>();

        safely(() -> addPlatformWidgets(widgets, currentUser));
        if (isEnabled(tenantId, ModuleCode.ISMS)) {
            safely(() -> addIsmsWidgets(widgets));
        }
        if (isEnabled(tenantId, ModuleCode.ERM)) {
            safely(() -> addErmWidgets(widgets));
            safely(() -> addTprmWidgets(widgets));
        }
        if (isEnabled(tenantId, ModuleCode.BCMS)) {
            safely(() -> addBcmsWidgets(widgets));
        }
        if (isEnabled(tenantId, ModuleCode.AUDIT)) {
            safely(() -> addAuditWidgets(widgets));
        }
        if (isEnabled(tenantId, ModuleCode.PRIVACY)) {
            safely(() -> addPrivacyWidgets(widgets));
        }
        safely(() -> addCapaWidgets(widgets));
        safely(() -> addEvidenceWidgets(widgets));
        safely(() -> addPolicyWidgets(widgets));

        List<RecentActivityItem> recentActivity = new ArrayList<>();
        safely(() -> recentActivity.addAll(loadRecentActivity()));

        return new DashboardSummaryResponse(Instant.now(), widgets, recentActivity);
    }

    private boolean isEnabled(String tenantId, ModuleCode code) {
        return tenantId != null && !tenantId.isBlank()
                && moduleLicensingService.isEnabled(tenantId, code)
                && adminModuleAccessService.mayUse(code, false);
    }

    // ---- Platform (always on) ----

    private void addPlatformWidgets(List<DashboardWidget> widgets, User currentUser) {
        List<WorkTask> myTasks = workTaskRepository.findByAssignedToUserIdOrderByUpdatedAtDesc(currentUser.getId());
        long myPending = myTasks.stream().filter(t -> TASK_OPEN.contains(t.getStatus())).count();
        widgets.add(DashboardWidget.of("my_tasks_pending", "My Pending Tasks", "PLATFORM", myPending,
                "Across Gap, Risk, CAPA, BIA, Policy", myPending > 0 ? "warning" : "success",
                "overview", "All good: nothing to action"));

        long unread = notificationRepository.countByUserIdAndReadAtIsNull(currentUser.getId());
        widgets.add(DashboardWidget.of("unread_notifications", "Unread Notifications", "PLATFORM", unread,
                "Since you last opened the bell", unread > 0 ? "info" : "success",
                "overview", "All caught up"));

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
        if (isAdmin) {
            long openTasks = workTaskRepository.findAll().stream()
                    .filter(t -> TASK_OPEN.contains(t.getStatus()))
                    .count();
            widgets.add(DashboardWidget.of("open_work_tasks", "Open Tasks (All Users)", "PLATFORM", openTasks,
                    "Assigned across the workspace", openTasks > 0 ? "warning" : "success",
                    "overview", "All good: nothing to action"));
        }

        long recentCount = auditLogRepository.countByCreatedAtAfter(Instant.now().minus(Duration.ofHours(24)));
        widgets.add(DashboardWidget.of("recent_activity", "Recent Activity", "PLATFORM", recentCount,
                "Events in the last 24 hours", "neutral", "overview", "No recent activity"));
    }

    private List<RecentActivityItem> loadRecentActivity() {
        List<AuditLog> rows = auditLogRepository
                .findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 10))
                .getContent();
        List<RecentActivityItem> out = new ArrayList<>();
        for (AuditLog row : rows) {
            String actorEmail = row.getUserId() != null
                    ? userService.findById(row.getUserId()).map(User::getEmail).orElse(null)
                    : null;
            out.add(new RecentActivityItem(
                    row.getId(),
                    row.getActionType() != null ? row.getActionType().name() : null,
                    row.getModule(),
                    row.getRecordName(),
                    actorEmail,
                    row.getCreatedAt()));
        }
        return out;
    }

    // ---- ISMS ----

    private void addIsmsWidgets(List<DashboardWidget> widgets) {
        List<RiskRecord> isRisks = riskRecordRepository.findByRiskTypeOrderByUpdatedAtDesc(RiskType.IS_RISK);
        long open = isRisks.stream().filter(r -> !RISK_CLOSED.contains(r.getStatus())).count();
        widgets.add(DashboardWidget.of("open_is_risks", "Open IS Risks", "ISMS", open,
                "Not yet closed", open > 0 ? "warning" : "success", "risk", "All good: nothing to action"));

        long overdue = isRisks.stream().filter(r -> r.getStatus() == RiskStatus.OVERDUE).count();
        widgets.add(DashboardWidget.of("overdue_is_risks", "Overdue IS Risks", "ISMS", overdue,
                "Past review or treatment date", overdue > 0 ? "danger" : "success", "risk",
                "All good: nothing to action"));

        List<Capa> gapCapas = capaRepository.findByRegisterCodeOrderByUpdatedAtDesc(CapaRegister.GAP);
        long openGapCapa = gapCapas.stream().filter(c -> c.getStatus() != CapaStatus.CLOSED).count();
        widgets.add(DashboardWidget.of("open_capa_gap_register", "Open CAPA (Gap Register)", "ISMS", openGapCapa,
                "Register 1: from Gap Assessments", openGapCapa > 0 ? "warning" : "success", "audit",
                "All good: nothing to action"));

        List<Soa> soas = soaRepository.findAllByOrderByUpdatedAtDesc();
        long soaActive = soas.stream().filter(s -> s.getStatus() != SoaStatus.COMPLETE).count();
        widgets.add(DashboardWidget.of("soa_active_count", "SoAs in Progress", "ISMS", soaActive,
                "Not yet marked complete", soaActive > 0 ? "info" : "success", "assessments",
                "All Statements of Applicability are complete"));

        List<GapAssessment> gaps = gapAssessmentRepository.findAllByOrderByUpdatedAtDesc();
        long gapInProgress = gaps.stream().filter(g -> g.getStatus() == GapAssessmentStatus.IN_PROGRESS).count();
        widgets.add(DashboardWidget.of("gap_in_progress", "Gap Assessments in Progress", "ISMS", gapInProgress,
                "Being scored / reviewed", gapInProgress > 0 ? "info" : "success", "assessments",
                "No gap assessments currently in progress"));

        addSoaCompletionWidget(widgets, soas);
        addOverallComplianceWidgets(widgets, gaps);
        addControlsByImplementationWidget(widgets, gaps);
    }

    /** §19.2 must-have: % of SoA controls with applicability decided. */
    private void addSoaCompletionWidget(List<DashboardWidget> widgets, List<Soa> soas) {
        if (soas.isEmpty()) {
            widgets.add(new DashboardWidget("soa_completion_pct", "SoA Completion", "ISMS", "-",
                    "No Statements of Applicability yet", "neutral", "soa", "Create a SoA to track completion"));
            return;
        }
        long totalItems = 0;
        long decided = 0;
        for (Soa soa : soas) {
            List<SoaItem> items = soaItemRepository.findBySoaIdOrderByNodeCodeAsc(soa.getId());
            totalItems += items.size();
            decided += items.stream().filter(i -> i.getApplicability() != null).count();
        }
        int pct = totalItems == 0 ? 0 : (int) Math.round(decided * 100.0 / totalItems);
        String tone = pct >= 80 ? "success" : (pct >= 40 ? "info" : "warning");
        widgets.add(new DashboardWidget("soa_completion_pct", "SoA Completion", "ISMS", pct + "%",
                decided + " of " + totalItems + " controls marked applicable / N/A",
                tone, "soa", "All SoA controls decided"));
    }

    /**
     * §19.2 must-haves: overall compliance (mean of scored Gaps) + framework breakdown.
     * Assumption: average of Gap assessments that have overallScore (IN_PROGRESS or COMPLETED).
     */
    private void addOverallComplianceWidgets(List<DashboardWidget> widgets, List<GapAssessment> gaps) {
        List<GapAssessment> scored = gaps.stream()
                .filter(g -> g.getOverallScore() != null)
                .filter(g -> g.getStatus() == GapAssessmentStatus.IN_PROGRESS
                        || g.getStatus() == GapAssessmentStatus.COMPLETED)
                .toList();
        if (scored.isEmpty()) {
            widgets.add(new DashboardWidget("overall_compliance_score", "Overall Compliance Score", "ISMS", "-",
                    "No scored Gap assessments yet", "neutral", "assessments",
                    "Score appears after Gap controls are assessed"));
            widgets.add(new DashboardWidget("framework_compliance_breakdown", "Framework Compliance", "ISMS", "-",
                    "No framework scores yet", "neutral", "assessments",
                    "Breakdown appears after Gap assessments are scored"));
            return;
        }
        BigDecimal sum = scored.stream()
                .map(GapAssessment::getOverallScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(scored.size()), 1, RoundingMode.HALF_UP);
        String ragHint = scored.stream()
                .map(GapAssessment::getRagStatus)
                .filter(r -> r != null && !r.isBlank())
                .findFirst()
                .orElse(null);
        String tone = avg.doubleValue() >= 80 ? "success"
                : (avg.doubleValue() >= 50 ? "info" : "warning");
        if ("RED".equalsIgnoreCase(ragHint) || "CRITICAL".equalsIgnoreCase(ragHint)) {
            tone = "danger";
        } else if ("AMBER".equalsIgnoreCase(ragHint) || "YELLOW".equalsIgnoreCase(ragHint)) {
            tone = "warning";
        }
        widgets.add(new DashboardWidget("overall_compliance_score", "Overall Compliance Score", "ISMS",
                avg.toPlainString() + "%",
                "Average across " + scored.size() + " scored Gap assessment"
                        + (scored.size() == 1 ? "" : "s"),
                tone, "assessments", null));

        Map<String, GapAssessment> latestByFw = new LinkedHashMap<>();
        for (GapAssessment g : scored) {
            String code = g.getFrameworkCode() != null ? g.getFrameworkCode() : "-";
            latestByFw.putIfAbsent(code, g);
        }
        String breakdown = latestByFw.entrySet().stream()
                .map(e -> e.getKey() + " "
                        + e.getValue().getOverallScore().setScale(0, RoundingMode.HALF_UP).toPlainString() + "%")
                .collect(Collectors.joining(" · "));
        if (breakdown.length() > 90) {
            breakdown = breakdown.substring(0, 87) + "…";
        }
        widgets.add(new DashboardWidget("framework_compliance_breakdown", "Framework Compliance", "ISMS",
                String.valueOf(latestByFw.size()),
                breakdown,
                "info", "assessments", null));
    }

    /** §19.2 must-have: applicable Gap controls by effective implementation status. */
    private void addControlsByImplementationWidget(List<DashboardWidget> widgets, List<GapAssessment> gaps) {
        Set<GapAssessmentStatus> active = EnumSet.of(
                GapAssessmentStatus.IN_PROGRESS, GapAssessmentStatus.COMPLETED);
        List<GapAssessment> activeGaps = gaps.stream().filter(g -> active.contains(g.getStatus())).toList();
        EnumMap<GapImplementationStatus, Long> counts = new EnumMap<>(GapImplementationStatus.class);
        for (GapImplementationStatus s : GapImplementationStatus.values()) {
            counts.put(s, 0L);
        }
        long applicable = 0;
        long unscored = 0;
        for (GapAssessment g : activeGaps) {
            for (GapItem item : gapItemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(g.getId())) {
                if (item.getApplicability() == GapApplicability.NOT_APPLICABLE) {
                    continue;
                }
                applicable++;
                GapImplementationStatus effective = item.effectiveImplementationStatus();
                if (effective == null) {
                    unscored++;
                } else {
                    counts.merge(effective, 1L, Long::sum);
                }
            }
        }
        if (applicable == 0) {
            widgets.add(new DashboardWidget("controls_by_implementation", "Controls by Implementation", "ISMS", "-",
                    "No applicable Gap controls yet", "neutral", "assessments",
                    "Counts appear once Gap assessments have controls"));
            return;
        }
        long fully = counts.getOrDefault(GapImplementationStatus.FULLY_IMPLEMENTED, 0L);
        int pctFully = (int) Math.round(fully * 100.0 / applicable);
        String subtitle = "Full " + fully
                + " · Partial " + counts.getOrDefault(GapImplementationStatus.PARTIALLY_IMPLEMENTED, 0L)
                + " · Planned " + counts.getOrDefault(GapImplementationStatus.PLANNED, 0L)
                + " · Not " + counts.getOrDefault(GapImplementationStatus.NOT_IMPLEMENTED, 0L)
                + (unscored > 0 ? " · Open " + unscored : "");
        String tone = pctFully >= 80 ? "success" : (pctFully >= 40 ? "info" : "warning");
        widgets.add(new DashboardWidget("controls_by_implementation", "Controls by Implementation", "ISMS",
                pctFully + "%",
                subtitle,
                tone, "assessments", null));
    }

    // ---- ERM ----

    private void addErmWidgets(List<DashboardWidget> widgets) {
        List<RiskRecord> ermRisks = riskRecordRepository.findByRiskTypeOrderByUpdatedAtDesc(RiskType.ERM);
        List<RiskRecord> isRisks = riskRecordRepository.findByRiskTypeOrderByUpdatedAtDesc(RiskType.IS_RISK);
        long open = ermRisks.stream().filter(r -> !RISK_CLOSED.contains(r.getStatus())).count();
        widgets.add(DashboardWidget.of("open_erm_risks", "Open ERM Risks", "ERM", open,
                "Not yet closed", open > 0 ? "warning" : "success", "erm-risk", "All good: nothing to action"));

        widgets.add(DashboardWidget.of("risk_heat_map", "Risk Heat Map", "ERM", open,
                "Interactive 5×5: click a cell on the dashboard map", open > 0 ? "info" : "success",
                "erm-risk", "All good: nothing to action"));

        long overdueErm = ermRisks.stream().filter(r -> r.getStatus() == RiskStatus.OVERDUE).count();
        widgets.add(DashboardWidget.of("overdue_erm_risks", "Overdue ERM Risks", "ERM", overdueErm,
                "Past review or treatment date", overdueErm > 0 ? "danger" : "success", "erm-risk",
                "All good: nothing to action"));

        long overdueIs = isRisks.stream().filter(r -> r.getStatus() == RiskStatus.OVERDUE).count();
        long overdueTreatments = overdueErm + overdueIs;
        widgets.add(DashboardWidget.of("overdue_risk_treatments", "Overdue Risk Treatments", "ERM", overdueTreatments,
                "IS + ERM overdue (review / treatment)", overdueTreatments > 0 ? "danger" : "success", "erm-risk",
                "All good: nothing to action"));

        long criticalOpen = ermRisks.stream()
                .filter(r -> !RISK_CLOSED.contains(r.getStatus()))
                .filter(r -> r.getResidualRisk() != null && CRITICAL_RISK.contains(r.getResidualRisk()))
                .count();
        widgets.add(DashboardWidget.of("critical_open_risks", "Critical Open ERM Risks", "ERM", criticalOpen,
                "Open with residual High / Very High", criticalOpen > 0 ? "danger" : "success", "erm-risk",
                "All good: nothing to action"));

        addTopCriticalRisksWidget(widgets, isRisks, ermRisks);
        addRisksByPriorityWidget(widgets, ermRisks);
        addKriBreachedWidget(widgets);
    }

    private void addTopCriticalRisksWidget(List<DashboardWidget> widgets,
                                           List<RiskRecord> isRisks,
                                           List<RiskRecord> ermRisks) {
        List<RiskRecord> critical = new ArrayList<>();
        for (RiskRecord r : isRisks) {
            if (!RISK_CLOSED.contains(r.getStatus())
                    && r.getResidualRisk() != null
                    && CRITICAL_RISK.contains(r.getResidualRisk())) {
                critical.add(r);
            }
        }
        for (RiskRecord r : ermRisks) {
            if (!RISK_CLOSED.contains(r.getStatus())
                    && r.getResidualRisk() != null
                    && CRITICAL_RISK.contains(r.getResidualRisk())) {
                critical.add(r);
            }
        }
        critical.sort(Comparator.comparing(RiskRecord::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<RiskRecord> top = critical.stream().limit(5).toList();
        if (top.isEmpty()) {
            widgets.add(DashboardWidget.of("top_critical_risks", "Top Critical Risks", "ERM", 0,
                    "Open residual High / Very High (IS + ERM)", "success", "erm-risk",
                    "All good: nothing to action"));
            return;
        }
        String subtitle = top.stream()
                .map(r -> r.getDisplayId() != null ? r.getDisplayId() : ("#" + r.getId()))
                .collect(Collectors.joining(" · "));
        widgets.add(new DashboardWidget("top_critical_risks", "Top Critical Risks", "ERM",
                String.valueOf(top.size()),
                subtitle + (critical.size() > 5 ? " (+" + (critical.size() - 5) + " more)" : ""),
                "danger", "erm-risk", null));
    }

    private void addRisksByPriorityWidget(List<DashboardWidget> widgets, List<RiskRecord> ermRisks) {
        EnumMap<PriorityLevel, Long> counts = new EnumMap<>(PriorityLevel.class);
        for (PriorityLevel p : PriorityLevel.values()) {
            counts.put(p, 0L);
        }
        long open = 0;
        for (RiskRecord r : ermRisks) {
            if (RISK_CLOSED.contains(r.getStatus()) || r.getResidualRisk() == null) {
                continue;
            }
            open++;
            counts.merge(PriorityLevel.fromResidual(r.getResidualRisk()), 1L, Long::sum);
        }
        long p1 = counts.getOrDefault(PriorityLevel.CRITICAL, 0L);
        String subtitle = "P1 " + p1
                + " · P2 " + counts.getOrDefault(PriorityLevel.HIGH, 0L)
                + " · P3 " + counts.getOrDefault(PriorityLevel.MEDIUM, 0L)
                + " · P4 " + counts.getOrDefault(PriorityLevel.LOW, 0L);
        String tone = p1 > 0 ? "danger" : (open > 0 ? "info" : "success");
        widgets.add(new DashboardWidget("risks_by_priority", "Risks by Priority", "ERM",
                String.valueOf(open),
                subtitle,
                tone, "erm-risk", "All good: nothing to action"));
    }

    private void addKriBreachedWidget(List<DashboardWidget> widgets) {
        List<Kri> kris = kriRepository.findByStatusOrderByUpdatedAtDesc(KriStatus.ACTIVE);
        long breached = kris.stream().filter(k -> k.getRag() == KriRag.RED).count();
        widgets.add(DashboardWidget.of("kri_breached_count", "KRI Breached (Red)", "ERM", breached,
                "Active KRIs on Red RAG", breached > 0 ? "danger" : "success", "kri",
                "All good: nothing to action"));
    }

    // ---- TPRM (under ERM licence) ----

    private void addTprmWidgets(List<DashboardWidget> widgets) {
        List<TprmVendor> vendors = tprmVendorRepository.findAllByOrderByNameAsc();
        long active = vendors.stream().filter(v -> v.getStatus() == VendorStatus.ACTIVE).count();
        widgets.add(DashboardWidget.of("active_vendors", "Active Vendors", "ERM", active,
                "Vendor register: Active status", "info", "tprm-vendors", "No active vendors"));

        long critical = vendors.stream()
                .filter(v -> v.getCriticality() == TprmCriticality.CRITICAL)
                .count();
        widgets.add(DashboardWidget.of("high_risk_vendors", "Critical Vendors", "ERM", critical,
                "Tier 1 / Tier 2", critical > 0 ? "warning" : "success", "tprm-vendors",
                "No critical vendors"));

        long nonCritical = vendors.stream().filter(v -> v.getCriticality() == TprmCriticality.NON_CRITICAL).count();
        long notCompliant = vendors.stream()
                .filter(v -> v.getComplianceStatus() == TprmCompliance.NOT_COMPLIANT)
                .count();
        widgets.add(new DashboardWidget("vendor_risk_distribution", "Vendor Criticality", "ERM",
                String.valueOf(notCompliant),
                "Critical " + critical + " · Non-critical " + nonCritical + " · Not compliant " + notCompliant,
                notCompliant > 0 ? "danger" : "info", "tprm-vendors",
                vendors.isEmpty() ? "No vendors yet" : null));

        long issued = vendors.stream()
                .filter(v -> v.getLifecycleState() == VendorLifecycleState.ISSUED
                        || v.getLifecycleState() == VendorLifecycleState.RETURNED)
                .count();
        widgets.add(DashboardWidget.of("issued_questionnaires", "Issued / Awaiting Vendor", "ERM", issued,
                "Questionnaire outstanding", issued > 0 ? "warning" : "info", "tprm-projects",
                issued == 0 ? "No outstanding questionnaires" : null));

        LocalDate today = LocalDate.now();
        long reviewDue = vendors.stream()
                .filter(v -> v.getNextReviewDate() != null && !v.getNextReviewDate().isAfter(today))
                .count();
        widgets.add(DashboardWidget.of("vendors_review_due", "Reassessment Due", "ERM", reviewDue,
                "Next reassessment today or earlier", reviewDue > 0 ? "warning" : "success", "tprm-vendors",
                "All good: nothing to action"));
    }

    // ---- Privacy ----

    private void addPrivacyWidgets(List<DashboardWidget> widgets) {
        List<PrivacyDsr> dsrs = privacyDsrRepository.findAllByOrderByUpdatedAtDesc();
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(10);

        long open = dsrs.stream().filter(d -> DSR_OPEN.contains(d.getStatus())).count();
        widgets.add(DashboardWidget.of("open_dsrs", "Open DSRs", "PRIVACY", open,
                "Received, in progress, or overdue", open > 0 ? "warning" : "success", "privacy-dsr",
                "All good: nothing to action"));

        long approaching = dsrs.stream()
                .filter(d -> DSR_OPEN.contains(d.getStatus()))
                .filter(d -> d.getDueAt() != null
                        && !d.getDueAt().isBefore(today)
                        && !d.getDueAt().isAfter(windowEnd))
                .count();
        widgets.add(DashboardWidget.of("dsrs_approaching_deadline", "DSRs Approaching Deadline", "PRIVACY", approaching,
                "Due within the next 10 days", approaching > 0 ? "warning" : "success", "privacy-dsr",
                "All good: nothing to action"));

        long overdue = dsrs.stream()
                .filter(d -> d.getStatus() == DsrStatus.OVERDUE
                        || (DSR_OPEN.contains(d.getStatus()) && d.getDueAt() != null && d.getDueAt().isBefore(today)))
                .count();
        widgets.add(DashboardWidget.of("overdue_dsrs", "Overdue DSRs", "PRIVACY", overdue,
                "Past due date or marked overdue", overdue > 0 ? "danger" : "success", "privacy-dsr",
                "All good: nothing to action"));
    }

    // ---- CAPA (cross-module, always on) ----

    private void addCapaWidgets(List<DashboardWidget> widgets) {
        List<Capa> capas = capaRepository.findAllByOrderByUpdatedAtDesc();
        long openCapas = capas.stream().filter(c -> c.getStatus() != CapaStatus.CLOSED).count();
        widgets.add(DashboardWidget.of("open_capa_count", "Open CAPAs", "PLATFORM", openCapas,
                "All registers: not closed", openCapas > 0 ? "warning" : "success", "audit",
                "All good: nothing to action"));

        long overdue = capas.stream()
                .filter(c -> c.getStatus() == CapaStatus.OVERDUE)
                .count();
        widgets.add(DashboardWidget.of("overdue_capas", "Overdue CAPAs", "PLATFORM", overdue,
                "Across all CAPA registers", overdue > 0 ? "danger" : "success", "audit",
                "All good: nothing to action"));
    }

    // ---- BCMS ----

    private void addBcmsWidgets(List<DashboardWidget> widgets) {
        List<BiaProcess> processes = biaProcessRepository.findAllByOrderByUpdatedAtDesc();
        long inScope = processes.stream()
                .filter(b -> b.getReviewOutcome() != BiaReviewOutcome.REJECTED)
                .count();
        long verified = processes.stream()
                .filter(b -> b.getReviewOutcome().specialistDone())
                .count();
        long pct = inScope > 0 ? Math.round(verified * 100.0 / inScope) : 0;
        widgets.add(new DashboardWidget("bia_approved_pct", "BIA Verified", "BCMS", pct + "%",
                verified + " of " + inScope + " verified",
                inScope == 0 ? "neutral" : pct == 100 ? "success" : "warning",
                "bcms-bia", "No BIAs recorded yet"));

        long criticalRto = processes.stream()
                .filter(b -> b.getReviewOutcome() != BiaReviewOutcome.REJECTED)
                .filter(b -> b.getCriticalityTier() != null && b.getCriticalityTier() == 1)
                .count();
        widgets.add(DashboardWidget.of("bia_critical_rto_under_4h", "Critical Processes RTO ≤ 4h", "BCMS", criticalRto,
                "Tier 1 (RTO 3 h / MAO 4 h)", "info", "bcms-bia", "No Tier 1 processes"));

        long plansApproved = recoveryPlanRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(p -> p.getStatus() == RecoveryPlanStatus.APPROVED)
                .count();
        widgets.add(DashboardWidget.of("recovery_plans_approved", "Recovery Plans Approved", "BCMS", plansApproved,
                "Owner-approved plans", "success", "bcms-plans", "No recovery plans approved yet"));

        long exercisesPlanned = bcmsExerciseRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(e -> e.getStatus() == ExerciseStatus.PLANNED)
                .count();
        widgets.add(DashboardWidget.of("exercises_planned", "Exercises Planned", "BCMS", exercisesPlanned,
                "Scheduled tabletop / simulation", "info", "bcms-exercises", "No exercises currently planned"));

        addNcemaWidgets(widgets);
    }

    private void addNcemaWidgets(List<DashboardWidget> widgets) {
        GapAssessment latest = gapAssessmentRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(a -> FrameworkScoringSupport.isNcema(a.getFrameworkCode(), a.getFrameworkName()))
                .filter(a -> a.getOverallScore() != null)
                .findFirst()
                .orElse(null);
        if (latest == null) {
            widgets.add(new DashboardWidget("ncema_compliance_score", "NCEMA Compliance Score", "BCMS", "-",
                    "Latest NCEMA V3 gap assessment", "info", "assessments",
                    "No scored NCEMA assessment yet"));
            widgets.add(new DashboardWidget("ncema_score_by_pillar", "NCEMA Score by Pillar", "BCMS", "-",
                    "Six-pillar breakdown", "info", "assessments",
                    "No NCEMA pillar scores yet"));
            return;
        }
        String score = latest.getOverallScore().toPlainString() + "%";
        String rag = latest.getRagStatus() != null ? latest.getRagStatus() : "-";
        widgets.add(new DashboardWidget("ncema_compliance_score", "NCEMA Compliance Score", "BCMS", score,
                latest.getFrameworkCode() + " · RAG " + rag,
                "GREEN".equalsIgnoreCase(rag) ? "success" : ("RED".equalsIgnoreCase(rag) ? "danger" : "warning"),
                "assessments", null));

        GapLayerScoreSummary layers = GapLayerScoreJson.parse(latest.getLayerScoreJson());
        if (layers == null || layers.domains() == null || layers.domains().isEmpty()) {
            widgets.add(new DashboardWidget("ncema_score_by_pillar", "NCEMA Score by Pillar", "BCMS", "-",
                    "Score applicable controls to populate pillars", "info", "assessments", null));
            return;
        }
        String pillarSubtitle = layers.domains().stream()
                .map(p -> p.code() + " " + (p.score() != null ? p.score().toPlainString() + "%" : "-"))
                .collect(Collectors.joining(" · "));
        widgets.add(new DashboardWidget("ncema_score_by_pillar", "NCEMA Score by Pillar", "BCMS",
                String.valueOf(layers.domains().size()),
                pillarSubtitle, "info", "assessments", null));
    }

    // ---- Audit ----

    private void addAuditWidgets(List<DashboardWidget> widgets) {
        List<GrcAudit> audits = grcAuditRepository.findAllByOrderByUpdatedAtDesc();
        long openFindings = 0;
        for (GrcAudit audit : audits) {
            if (audit.getStatus() != GrcAuditStatus.CLOSED) {
                openFindings += grcAuditFindingRepository.countByAuditId(audit.getId());
            }
        }
        widgets.add(DashboardWidget.of("open_findings", "Open Findings", "AUDIT", openFindings,
                "On audits not yet locked", openFindings > 0 ? "warning" : "success", "audit",
                "All good: nothing to action"));

        long inProgress = audits.stream().filter(a -> a.getStatus() == GrcAuditStatus.IN_PROGRESS).count();
        widgets.add(DashboardWidget.of("audits_in_progress", "Audits in Progress", "AUDIT", inProgress,
                "Checklist / findings underway", "info", "audit", "No audits currently in progress"));

        addUaeIaLatestWidget(widgets);
        addFindingsBySeverityWidget(widgets);
    }

    private void addUaeIaLatestWidget(List<DashboardWidget> widgets) {
        GapAssessment latest = gapAssessmentRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(a -> FrameworkScoringSupport.isUaeIa(a.getFrameworkCode(), a.getFrameworkName()))
                .filter(a -> a.getOverallScore() != null)
                .findFirst()
                .orElse(null);
        if (latest == null) {
            widgets.add(new DashboardWidget("uae_ia_latest_score", "UAE IA Latest Score", "AUDIT", "-",
                    "Latest UAE IA V2.1 gap assessment", "info", "assessments",
                    "No scored UAE IA assessment yet"));
            return;
        }
        String score = latest.getOverallScore().toPlainString() + "%";
        String rag = latest.getRagStatus() != null ? latest.getRagStatus() : "-";
        widgets.add(new DashboardWidget("uae_ia_latest_score", "UAE IA Latest Score", "AUDIT", score,
                latest.getFrameworkCode() + " · RAG " + rag,
                "GREEN".equalsIgnoreCase(rag) ? "success" : ("RED".equalsIgnoreCase(rag) ? "danger" : "warning"),
                "assessments", null));
    }

    private void addFindingsBySeverityWidget(List<DashboardWidget> widgets) {
        EnumMap<FindingSeverity, Long> counts = new EnumMap<>(FindingSeverity.class);
        for (FindingSeverity s : FindingSeverity.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : grcAuditFindingRepository.countOpenFindingsBySeverity()) {
            if (row[0] instanceof FindingSeverity sev && row[1] instanceof Long cnt) {
                counts.put(sev, cnt);
            } else if (row[0] instanceof FindingSeverity sev && row[1] instanceof Number n) {
                counts.put(sev, n.longValue());
            }
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        String subtitle = "Major " + counts.get(FindingSeverity.MAJOR_NC)
                + " · Minor " + counts.get(FindingSeverity.MINOR_NC)
                + " · Obs " + counts.get(FindingSeverity.OBSERVATION)
                + " · OFI " + counts.get(FindingSeverity.OFI);
        widgets.add(new DashboardWidget("open_findings_by_severity", "Open Findings by Severity", "AUDIT",
                String.valueOf(total), subtitle, total > 0 ? "warning" : "success", "audit",
                "All good: nothing to action"));
    }

    // ---- Evidence (platform always-on) ----

    private void addEvidenceWidgets(List<DashboardWidget> widgets) {
        long expiring = evidenceRepository.countActiveExpiringOrExpired(LocalDate.now().plusDays(30));
        widgets.add(DashboardWidget.of("evidence_expiring", "Evidence Expiring / Expired", "PLATFORM", expiring,
                "Within 30 days or already expired", expiring > 0 ? "warning" : "success", "evidence",
                "All good: nothing to action"));
    }

    // ---- Policy (platform always-on) ----

    private void addPolicyWidgets(List<DashboardWidget> widgets) {
        long dueForReview = policyRepository.findByStatusOrderByUpdatedAtDesc(PolicyStatus.DUE_FOR_REVIEW).size();
        widgets.add(DashboardWidget.of("policies_due_for_review", "Policies Due for Review", "PLATFORM", dueForReview,
                "Past their review date", dueForReview > 0 ? "warning" : "success", "policies",
                "All good: nothing to action"));

        long pendingOwner = policyRepository.findByStatusOrderByUpdatedAtDesc(PolicyStatus.PENDING_OWNER).size();
        widgets.add(DashboardWidget.of("policies_pending_owner", "Policies Pending Owner Approval", "PLATFORM", pendingOwner,
                "Awaiting final sign-off", pendingOwner > 0 ? "info" : "success", "policies",
                "All good: nothing to action"));

        long openAckTasks = workTaskRepository.findAll().stream()
                .filter(t -> "POLICY".equals(t.getSourceModule()) && "POLICY_ACK".equals(t.getSourceRecordType())
                        && TASK_OPEN.contains(t.getStatus()))
                .count();
        widgets.add(DashboardWidget.of("open_policy_ack_tasks", "Open Policy Acknowledgements", "PLATFORM", openAckTasks,
                "Pending across the workspace", openAckTasks > 0 ? "info" : "success", "policies",
                "All good: nothing to action"));
    }

    private void safely(Runnable block) {
        try {
            block.run();
        } catch (Exception e) {
            log.error("Dashboard widget block failed (resilient: skipping): {}", e.getMessage(), e);
        }
    }
}
