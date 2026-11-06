package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.FrameworkQueryService;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.asset.domain.AssetStatus;
import com.a9.arbor.grc.modules.asset.repo.AssetRepository;
import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceClassification;
import com.a9.arbor.grc.modules.evidence.service.EvidenceService;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserOrgUnit;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserOrgUnitRepository;
import com.a9.arbor.grc.modules.iam.service.ContributorEligibilityService;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.mail.AppNotificationMailComposer;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.org.repo.OrgProfileRepository;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.risk.domain.AcceptanceDecision;
import com.a9.arbor.grc.modules.risk.domain.EvaluationDecision;
import com.a9.arbor.grc.modules.risk.domain.ExcelRiskScoring;
import com.a9.arbor.grc.modules.risk.domain.FinalRiskStatus;
import com.a9.arbor.grc.modules.risk.domain.AcceptanceFormStatus;
import com.a9.arbor.grc.modules.risk.domain.HeatMapScoreBasis;
import com.a9.arbor.grc.modules.risk.domain.MatrixAxes;
import com.a9.arbor.grc.modules.risk.domain.MatrixGridSize;
import com.a9.arbor.grc.modules.risk.domain.ResidualScoringMode;
import com.a9.arbor.grc.modules.risk.domain.RiskAcceptanceForm;
import com.a9.arbor.grc.modules.risk.domain.RiskCategory;
import com.a9.arbor.grc.modules.risk.domain.RiskFrameworkMapping;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskMatrix;
import com.a9.arbor.grc.modules.risk.domain.RiskProject;
import com.a9.arbor.grc.modules.risk.domain.RiskProjectSeedStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskScoreHistory;
import com.a9.arbor.grc.modules.risk.domain.RiskStageNote;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.domain.TreatmentMilestone;
import com.a9.arbor.grc.modules.risk.domain.TreatmentFormKind;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import com.a9.arbor.grc.modules.risk.domain.TreatmentPlan;
import com.a9.arbor.grc.modules.risk.domain.TreatmentPlanStatus;
import com.a9.arbor.grc.modules.risk.domain.UaeIaPriority;
import com.a9.arbor.grc.modules.risk.repo.RiskAcceptanceFormRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskFrameworkMappingRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskScoreHistoryRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskStageNoteRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskProjectRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.risk.repo.TreatmentMilestoneRepository;
import com.a9.arbor.grc.modules.risk.repo.TreatmentPlanRepository;
import com.a9.arbor.grc.modules.risk.web.dto.AcceptanceAdminDecideRequest;
import com.a9.arbor.grc.modules.risk.web.dto.AcceptanceOwnerDecideRequest;
import com.a9.arbor.grc.modules.risk.web.dto.AssignRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CancelRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.ChangeTreatmentRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CloseRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CompleteMilestoneRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskStageNoteRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CreateMilestoneRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.FinalRiskStatusRequest;
import com.a9.arbor.grc.modules.risk.web.dto.PostTreatmentRequest;
import com.a9.arbor.grc.modules.risk.web.dto.EligibleContributorResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RejectValidationRequest;
import com.a9.arbor.grc.modules.risk.web.dto.ReopenRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskAcceptanceFormResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskFrameworkMappingRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskFrameworkMappingResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskScoreHistoryResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskStageNoteResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskHeatMapCellResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskHeatMapResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskKpisResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse;
import com.a9.arbor.grc.modules.risk.web.dto.SubmitRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.TreatmentPlanDraftRequest;
import com.a9.arbor.grc.modules.risk.web.dto.TreatmentPlanResponse;
import com.a9.arbor.grc.modules.risk.web.dto.UpdateRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.UpsertTreatmentPlanRequest;
import com.a9.arbor.grc.modules.risk.web.dto.ValidateApproveRequest;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RiskService {

    public static final String EVIDENCE_MODULE = "RISK";
    public static final String EVIDENCE_RECORD_TYPE = "RISK_RECORD";
    public static final String EVIDENCE_MILESTONE_RECORD_TYPE = "RISK_MILESTONE";

    private static final Set<RiskStatus> REOPENABLE = EnumSet.of(
            RiskStatus.CLOSED, RiskStatus.AUTO_CLOSED, RiskStatus.AVOIDED,
            RiskStatus.TRANSFERRED, RiskStatus.TOLERATED, RiskStatus.ACCEPTED);

    /** Admin finish picker when Acceptance Required is No. Not the register filter. */
    private static final Set<RiskStatus> ADMIN_FINISH_ACTIONS = EnumSet.of(
            RiskStatus.ACCEPTED, RiskStatus.CLOSED, RiskStatus.AVOIDED, RiskStatus.TRANSFERRED);

    private static final Set<RiskStatus> APPROVE_CLOSES_RECORD = EnumSet.of(
            RiskStatus.AUTO_CLOSED, RiskStatus.CLOSED, RiskStatus.AVOIDED,
            RiskStatus.TRANSFERRED, RiskStatus.ACCEPTED, RiskStatus.TOLERATED);

    private final RiskRecordRepository riskRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final TreatmentMilestoneRepository milestoneRepository;
    private final RiskAcceptanceFormRepository acceptanceFormRepository;
    private final RiskFrameworkMappingRepository frameworkMappingRepository;
    private final RiskScoreHistoryRepository scoreHistoryRepository;
    private final RiskStageNoteRepository stageNoteRepository;
    private final RiskIdGenerator idGenerator;
    private final UserRepository userRepository;
    private final UserOrgUnitRepository userOrgUnitRepository;
    private final ContributorEligibilityService contributorEligibilityService;
    private final OrgUnitRepository orgUnitRepository;
    private final AssetRepository assetRepository;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;
    private final EvidenceService evidenceService;
    private final FrameworkQueryService frameworkQueryService;
    private final GapAssessmentRepository gapAssessmentRepository;
    private final GapItemRepository gapItemRepository;
    private final RiskMatrixService riskMatrixService;
    private final RiskProjectRepository riskProjectRepository;
    private final OrgProfileRepository orgProfileRepository;
    private final com.a9.arbor.grc.modules.capa.service.CapaService capaService;
    private final DefaultIsRiskLibrary defaultIsRiskLibrary;
    private final RiskAdminRecipients riskAdminRecipients;
    private final RiskService self;

    public RiskService(RiskRecordRepository riskRepository,
                       TreatmentPlanRepository treatmentPlanRepository,
                       TreatmentMilestoneRepository milestoneRepository,
                       RiskAcceptanceFormRepository acceptanceFormRepository,
                       RiskFrameworkMappingRepository frameworkMappingRepository,
                       RiskScoreHistoryRepository scoreHistoryRepository,
                       RiskStageNoteRepository stageNoteRepository,
                       RiskIdGenerator idGenerator,
                       UserRepository userRepository,
                       UserOrgUnitRepository userOrgUnitRepository,
                       ContributorEligibilityService contributorEligibilityService,
                       OrgUnitRepository orgUnitRepository,
                       AssetRepository assetRepository,
                       WorkTaskRepository workTaskRepository,
                       NotificationService notificationService,
                       AuditTrailService auditTrailService,
                       ModuleLicensingService moduleLicensingService,
                       AdminModuleAccessService adminModuleAccessService,
                       EvidenceService evidenceService,
                       FrameworkQueryService frameworkQueryService,
                       GapAssessmentRepository gapAssessmentRepository,
                       GapItemRepository gapItemRepository,
                       RiskMatrixService riskMatrixService,
                       RiskProjectRepository riskProjectRepository,
                       OrgProfileRepository orgProfileRepository,
                       com.a9.arbor.grc.modules.capa.service.CapaService capaService,
                       DefaultIsRiskLibrary defaultIsRiskLibrary,
                       RiskAdminRecipients riskAdminRecipients,
                       @Lazy RiskService self) {
        this.riskRepository = riskRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.milestoneRepository = milestoneRepository;
        this.acceptanceFormRepository = acceptanceFormRepository;
        this.frameworkMappingRepository = frameworkMappingRepository;
        this.scoreHistoryRepository = scoreHistoryRepository;
        this.stageNoteRepository = stageNoteRepository;
        this.idGenerator = idGenerator;
        this.userRepository = userRepository;
        this.userOrgUnitRepository = userOrgUnitRepository;
        this.contributorEligibilityService = contributorEligibilityService;
        this.orgUnitRepository = orgUnitRepository;
        this.assetRepository = assetRepository;
        this.workTaskRepository = workTaskRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
        this.evidenceService = evidenceService;
        this.frameworkQueryService = frameworkQueryService;
        this.gapAssessmentRepository = gapAssessmentRepository;
        this.gapItemRepository = gapItemRepository;
        this.riskMatrixService = riskMatrixService;
        this.riskProjectRepository = riskProjectRepository;
        this.orgProfileRepository = orgProfileRepository;
        this.capaService = capaService;
        this.defaultIsRiskLibrary = defaultIsRiskLibrary;
        this.riskAdminRecipients = riskAdminRecipients;
        this.self = self;
    }

    private static final java.util.Set<RiskStatus> CLOSED_STATUSES =
            java.util.EnumSet.of(RiskStatus.CLOSED, RiskStatus.AUTO_CLOSED, RiskStatus.ACCEPTED);

    /** Same closed set as the register KPI cards (frontend isOpenRisk). */
    private static final Set<RiskStatus> KPI_CLOSED_STATUSES = EnumSet.of(
            RiskStatus.CLOSED, RiskStatus.AUTO_CLOSED, RiskStatus.AVOIDED,
            RiskStatus.TRANSFERRED, RiskStatus.TOLERATED, RiskStatus.ACCEPTED, RiskStatus.CANCELLED);

    /** Seeded IS stubs must satisfy TAP-required create. Custodian / Admin replace this. */
    private static final String SEEDED_TREATMENT_ACTION_PLAN =
            "This is a default Treatment Action Plan. Update it before assigning the risk.";

    @Transactional(readOnly = true)
    public List<RiskRecordResponse> list(User user, RiskType riskType) {
        return list(user, riskType, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Register list with optional heat-map drill filters (J8).
     * {@code openOnly=true} excludes CLOSED / AUTO_CLOSED.
     */
    @Transactional(readOnly = true)
    public List<RiskRecordResponse> list(User user,
                                         RiskType riskType,
                                         RiskLevel probability,
                                         RiskLevel impact,
                                         RiskLevel residualRisk,
                                         RiskLevel controlEffectiveness,
                                         RiskLevel inherentRisk,
                                         RiskLevel residualProbability,
                                         RiskLevel residualImpact,
                                         Boolean openOnly,
                                         Long projectId,
                                         RiskStatus status) {
        RiskType type = riskType != null ? riskType : RiskType.IS_RISK;
        requireModuleForType(type);
        List<RiskRecord> rows = loadListRows(user, type, projectId, status);
        return rows.stream()
                .filter(r -> projectId == null || projectId.equals(r.getRiskProjectId()))
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> openOnly == null || !openOnly || !CLOSED_STATUSES.contains(r.getStatus()))
                .filter(r -> probability == null || r.getProbability() == probability)
                .filter(r -> impact == null || r.getImpact() == impact)
                .filter(r -> residualRisk == null || r.getResidualRisk() == residualRisk)
                .filter(r -> {
                    boolean hasResidualPi = residualProbability != null || residualImpact != null;
                    boolean hasCe = controlEffectiveness != null || inherentRisk != null;
                    if (!hasResidualPi && !hasCe) {
                        return true;
                    }
                    boolean manualOk = residualProbability != null
                            && residualImpact != null
                            && r.getResidualProbability() == residualProbability
                            && r.getResidualImpact() == residualImpact;
                    boolean ceOk = controlEffectiveness != null
                            && inherentRisk != null
                            && r.getControlEffectiveness() == controlEffectiveness
                            && r.getInherentRisk() == inherentRisk;
                    if (hasResidualPi && hasCe) {
                        return manualOk || ceOk;
                    }
                    if (hasResidualPi) {
                        return (residualProbability == null || r.getResidualProbability() == residualProbability)
                                && (residualImpact == null || r.getResidualImpact() == residualImpact);
                    }
                    return (controlEffectiveness == null || r.getControlEffectiveness() == controlEffectiveness)
                            && (inherentRisk == null || r.getInherentRisk() == inherentRisk);
                })
                .map(this::toResponse)
                .toList();
    }

    /**
     * Project-picker KPI cards. One query, no per-row dossier hydration.
     * Bands match {@code computeRiskKpis} on the register.
     */
    @Transactional(readOnly = true)
    public RiskKpisResponse kpis(User user, RiskType riskType) {
        RiskType type = riskType != null ? riskType : RiskType.IS_RISK;
        requireModuleForType(type);
        List<RiskRecord> rows = loadListRows(user, type, null, null);
        MatrixGridSize defaultGrid = riskMatrixService.currentGridSize();
        int veryHigh = 0;
        int high = 0;
        int medium = 0;
        int low = 0;
        int veryLow = 0;
        int totalOpen = 0;
        for (RiskRecord r : rows) {
            if (KPI_CLOSED_STATUSES.contains(r.getStatus())) {
                continue;
            }
            totalOpen++;
            switch (kpiResidualBand(r, defaultGrid)) {
                case VERY_HIGH -> veryHigh++;
                case HIGH -> high++;
                case MEDIUM -> medium++;
                case LOW -> low++;
                case VERY_LOW -> veryLow++;
            }
        }
        return new RiskKpisResponse(veryHigh, high, medium, low, veryLow, totalOpen);
    }

    private List<RiskRecord> loadListRows(User user, RiskType type, Long projectId, RiskStatus status) {
        if (projectId != null) {
            RiskProject project = riskProjectRepository.findById(projectId).orElse(null);
            if (project == null || project.getProjectType() != type) {
                return List.of();
            }
            if (RoleAccess.isContributorOnly(user)) {
                return riskRepository.findVisibleToContributor(type, user.getId()).stream()
                        .filter(r -> projectId.equals(r.getRiskProjectId()))
                        .filter(r -> status == null || r.getStatus() == status)
                        .toList();
            }
            if (status != null) {
                return riskRepository.findByRiskProjectIdAndStatusOrderByUpdatedAtDesc(projectId, status);
            }
            return riskRepository.findByRiskProjectIdOrderByUpdatedAtDesc(projectId);
        }
        if (RoleAccess.isContributorOnly(user)) {
            return riskRepository.findVisibleToContributor(type, user.getId()).stream()
                    .filter(r -> status == null || r.getStatus() == status)
                    .toList();
        }
        if (status != null) {
            return riskRepository.findByRiskTypeAndStatusOrderByUpdatedAtDesc(type, status);
        }
        return riskRepository.findByRiskTypeOrderByUpdatedAtDesc(type);
    }

    private RiskLevel kpiResidualBand(RiskRecord risk, MatrixGridSize defaultGrid) {
        MatrixGridSize gs = defaultGrid;
        if (risk.getMatrixGridSize() != null) {
            try {
                gs = MatrixGridSize.fromInt(risk.getMatrixGridSize());
            } catch (Exception ignored) {
                gs = defaultGrid;
            }
        }
        RiskLevel band = risk.getResidualRisk();
        Integer residualScore = risk.getResidualScore();
        if (residualScore != null) {
            try {
                band = ExcelRiskScoring.workflowLevel(gs, residualScore);
            } catch (Exception ignored) {
                // keep stored residual band
            }
        }
        if (band == null) {
            band = RiskLevel.MEDIUM;
        }
        return gs.clampRow(band);
    }

    /** Interactive heat map counts (J8 / §19.2): dimensions follow tenant matrix grid size. */
    @Transactional(readOnly = true)
    public RiskHeatMapResponse heatMap(User user, RiskType riskType, HeatMapScoreBasis scoreBasis, boolean openOnly) {
        RiskType type = riskType != null ? riskType : RiskType.IS_RISK;
        requireModuleForType(type);
        HeatMapScoreBasis basis = scoreBasis != null ? scoreBasis : HeatMapScoreBasis.INHERENT;
        MatrixGridSize gridSize = riskMatrixService.currentGridSize();
        MatrixAxes axes = riskMatrixService.currentAxes();
        RiskLevel[] heatRows = axes.rowLevels();
        RiskLevel[] heatCols = axes.colLevels();

        List<RiskRecord> rows;
        if (RoleAccess.isContributorOnly(user)) {
            rows = riskRepository.findVisibleToContributor(type, user.getId());
        } else {
            rows = riskRepository.findByRiskTypeOrderByUpdatedAtDesc(type);
        }

        long closedExcluded = rows.stream().filter(r -> CLOSED_STATUSES.contains(r.getStatus())).count();
        List<RiskRecord> counted = openOnly
                ? rows.stream().filter(r -> !CLOSED_STATUSES.contains(r.getStatus())).toList()
                : rows;

        long[][] counts = new long[heatRows.length][heatCols.length];
        for (RiskRecord r : counted) {
            RiskLevel rowLevel;
            RiskLevel colLevel;
            if (basis == HeatMapScoreBasis.INHERENT) {
                rowLevel = r.getProbability();
                colLevel = r.getImpact();
            } else if (r.getResidualScoringMode() == ResidualScoringMode.MANUAL_PROBABILITY_IMPACT
                    && r.getResidualProbability() != null
                    && r.getResidualImpact() != null) {
                rowLevel = r.getResidualProbability();
                colLevel = r.getResidualImpact();
            } else {
                // Auto residual: Control effectiveness × Inherent
                rowLevel = r.getControlEffectiveness();
                colLevel = r.getInherentRisk();
            }
            if (rowLevel == null || colLevel == null) {
                continue;
            }
            int ri = axes.rowIndex(axes.clampRow(rowLevel));
            int ci = axes.colIndex(axes.clampCol(colLevel));
            if (ri >= 0 && ci >= 0) {
                counts[ri][ci]++;
            }
        }

        List<RiskHeatMapCellResponse> cells = new java.util.ArrayList<>(heatRows.length * heatCols.length);
        long total = 0;
        for (int ri = 0; ri < heatRows.length; ri++) {
            RiskLevel rowLevel = heatRows[ri];
            for (int ci = 0; ci < heatCols.length; ci++) {
                RiskLevel colLevel = heatCols[ci];
                long count = counts[ri][ci];
                total += count;
                RiskLevel band = basis == HeatMapScoreBasis.INHERENT
                        ? riskMatrixService.inherent(rowLevel, colLevel)
                        : riskMatrixService.residual(rowLevel, colLevel);
                cells.add(new RiskHeatMapCellResponse(rowLevel, colLevel, ri, ci, count, band));
            }
        }

        String rowAxis = basis == HeatMapScoreBasis.INHERENT
                ? "PROBABILITY"
                : "CONTROL_EFFECTIVENESS_OR_RESIDUAL_P";
        String colAxis = basis == HeatMapScoreBasis.INHERENT
                ? "IMPACT"
                : "INHERENT_OR_RESIDUAL_I";
        return new RiskHeatMapResponse(
                type,
                basis,
                openOnly,
                gridSize.size(),
                rowAxis,
                colAxis,
                java.util.Arrays.asList(heatRows),
                java.util.Arrays.asList(heatCols),
                cells,
                total,
                openOnly ? closedExcluded : 0L);
    }

    @Transactional(readOnly = true)
    public RiskRecordResponse get(User user, long id) {
        RiskRecord risk = requireVisible(user, id);
        requireModuleForType(risk.getRiskType());
        return toResponse(risk);
    }

    /** Admin: raise CAPA Register 1 from this risk finding (§14.3). */
    @Transactional
    public com.a9.arbor.grc.modules.capa.web.dto.CapaResponse raiseCapa(User admin, long id) {
        if (!admin.getRoles().contains(Role.ADMIN) && !admin.getRoles().contains(Role.OWNER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        RiskRecord risk = requireVisible(admin, id);
        requireWriteForType(risk.getRiskType());
        return capaService.createFromRisk(admin, risk);
    }

    @Transactional
    public RiskRecordResponse create(User user, CreateRiskRequest req) {
        RiskType type = req.riskType() != null ? req.riskType() : RiskType.IS_RISK;
        requireWriteForType(type);

        Long assetId = type == RiskType.IS_RISK ? req.assetId() : null;
        Long frameworkId = type == RiskType.IS_RISK ? req.frameworkId() : null;
        String frameworkCode = type == RiskType.IS_RISK ? req.frameworkCode() : null;
        Long frameworkNodeId = type == RiskType.IS_RISK ? req.frameworkNodeId() : null;
        String nodeCode = type == RiskType.IS_RISK ? req.nodeCode() : null;
        String observedGap = req.observedGap();
        String strategicObjective = type == RiskType.ERM ? req.strategicObjective() : null;
        java.math.BigDecimal financialImpactAed = type == RiskType.ERM ? req.financialImpactAed() : null;
        Long tprmVendorId = type == RiskType.ERM ? req.tprmVendorId() : null;

        // Org unit required for IS create / Gap raise; TPRM raise-ERM may omit until Admin assigns.
        // Assignment still requires org unit (§13.2 / A15).
        validateRefs(req.orgUnitId(), assetId, req.riskOwnerUserId());
        applyAutoCloseOverrideFlags(req.overrideAutoClose(), req.autoCloseOverrideJustification());
        if (type == RiskType.ERM && req.category() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk Category is required");
        }
        validateCategory(req.category(), req.categoryOther());
        requireProductScoreInRange(
                req.residualScore(),
                "Residual Risk Score",
                inherentProduct(req.probability(), req.impact()));

        RiskRecord risk = new RiskRecord();
        risk.setRiskType(type);
        applyDisplayId(risk, type, req.displayIdOverride(), user);
        applyFields(risk, req.title(), req.description(), req.category(), req.categoryOther(), req.orgUnitId(), assetId,
                frameworkId, frameworkCode, frameworkNodeId, nodeCode,
                observedGap, req.threats(), req.existingControls(), req.riskOwnerUserId(),
                req.adminNotes(), strategicObjective, financialImpactAed, tprmVendorId);
        risk.setProbability(req.probability());
        risk.setImpact(req.impact());
        risk.setControlEffectiveness(req.controlEffectiveness());
        if (req.riskProjectId() != null) {
            RiskProject project = riskProjectRepository.findById(req.riskProjectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk project not found"));
            if (project.getProjectType() != type) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk type must match the project");
            }
            risk.setRiskProjectId(project.getId());
            if (type == RiskType.IS_RISK && risk.getFrameworkId() == null && project.getFrameworkId() != null) {
                risk.setFrameworkId(project.getFrameworkId());
            }
        }
        risk.setDateIdentified(req.dateIdentified() != null ? req.dateIdentified() : java.time.LocalDate.now());
        if (type == RiskType.ERM) {
            risk.setCause(blankToNull(req.cause()));
        }
        if (req.targetControlEffectiveness() != null) {
            risk.setTargetControlEffectiveness(req.targetControlEffectiveness());
        }
        if (req.nextReviewDate() != null) {
            risk.setNextReviewDate(req.nextReviewDate());
        }
        if (req.thirdPartyName() != null) {
            risk.setThirdPartyName(blankToNull(req.thirdPartyName()));
        }
        if (req.treatmentJustification() != null) {
            risk.setTreatmentChangeJustification(blankToNull(req.treatmentJustification()));
        }
        if (req.compensatingControls() != null) {
            risk.setTransferCompensatingControls(blankToNull(req.compensatingControls()));
        }
        ResidualScoringMode createMode = req.residualScoringMode();
        if (risk.getRiskProjectId() != null && createMode == null) {
            createMode = ResidualScoringMode.CONTROL_EFFECTIVENESS;
        }
        applyResidualScoringFields(
                risk,
                createMode,
                req.residualProbability(),
                req.residualImpact(),
                true);
        if (req.residualScore() != null
                && (createMode == ResidualScoringMode.MANUAL_PROBABILITY_IMPACT
                    || risk.getResidualScoringMode() == ResidualScoringMode.MANUAL_PROBABILITY_IMPACT)) {
            risk.setResidualScore(req.residualScore());
        }
        risk.setTreatmentOption(req.treatmentOption() != null ? req.treatmentOption() : TreatmentOption.TREAT);
        risk.setTreatmentPlan(blankToNull(req.treatmentPlan()));
        risk.setSourceGapAssessmentId(req.sourceGapAssessmentId());
        risk.setSourceGapItemId(req.sourceGapItemId());
        risk.setSourceDpiaId(req.sourceDpiaId());
        if (req.sourceDpiaAssessmentRowId() != null) {
            if (riskRepository.existsBySourceDpiaAssessmentRowId(req.sourceDpiaAssessmentRowId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A Privacy Risk was already created from this DPIA assessment risk title");
            }
            risk.setSourceDpiaAssessmentRowId(req.sourceDpiaAssessmentRowId());
        }
        risk.setSourceRopaId(req.sourceRopaId());
        if (Boolean.TRUE.equals(req.overrideAutoClose())) {
            risk.setAutoCloseOverride(true);
            risk.setAutoCloseOverrideJustification(req.autoCloseOverrideJustification().trim());
        }
        // Doc 02 R-04: InfoSec posture fields are IS-only (not ERM).
        if (type == RiskType.IS_RISK) {
            risk.setInfosecStatusInitial(blankToNull(req.infosecStatusInitial()));
        }
        requireFilledIdentification(risk);
        recomputeScores(risk);
        resolveUaeIaPriority(risk, req.uaeIaPriority());
        risk.setCreatedByUserId(user.getId());
        requireUniqueTitle(risk.getRiskProjectId(), req.title(), null);
        applyInitialStatus(risk);
        riskRepository.save(risk);
        notifyRiskOwnerNamed(risk, null);

        replaceAdditionalFrameworks(risk.getId(), req.additionalFrameworks());
        applyTreatmentPlanDraft(risk, req.treatmentPlanDetail(), req.treatmentPlan());
        recordScoreHistory(risk, user, "Initial identification");
        if (req.assignedContributorId() != null
                && risk.getStatus() == RiskStatus.DRAFT
                && risk.getOrgUnitId() != null) {
            return assign(user, risk.getId(), new AssignRiskRequest(
                    req.assignedContributorId(), null, risk.getTreatmentOption(), risk.getTreatmentPlan()));
        }

        if (risk.getStatus() == RiskStatus.AUTO_CLOSED) {
            notifyAutoClosed(risk);
        }

        audit(user, AuditActionType.CREATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                risk.isDisplayIdOverridden() ? "displayId" : null,
                risk.isDisplayIdOverridden() ? risk.getDisplayId() : null);
        return toResponse(risk);
    }

    /**
     * Seed IS Risk register from A4 curated templates when a Compliance Project is created (§9.5 / J9).
     * Org unit left unset: Admin assigns before workflow. Does not invent content.
     */
    @Transactional
    public int seedFromCuratedLibrary(User user,
                                      long complianceProjectId,
                                      long frameworkId,
                                      String frameworkCode,
                                      List<com.a9.arbor.grc.catalog.domain.RiskTemplate> templates) {
        if (templates == null || templates.isEmpty()) {
            return 0;
        }
        requireWriteForType(RiskType.IS_RISK);
        Map<Long, com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse> nodes =
                frameworkQueryService.nodesById(frameworkId);
        int seeded = 0;
        for (com.a9.arbor.grc.catalog.domain.RiskTemplate t : templates) {
            String nodeCode = null;
            if (t.getFrameworkNodeId() != null) {
                var node = nodes.get(t.getFrameworkNodeId());
                if (node != null) {
                    nodeCode = node.code();
                }
            }
            RiskCategory category;
            try {
                category = RiskCategory.valueOf(t.getCategory());
            } catch (Exception e) {
                category = RiskCategory.IT;
            }
            RiskLevel probability = RiskLevel.valueOf(t.getSuggestedProbability());
            RiskLevel impact = RiskLevel.valueOf(t.getSuggestedImpact());
            RiskLevel controlEff = RiskLevel.valueOf(t.getSuggestedControlEffectiveness());
            TreatmentOption treatment = TreatmentOption.TREAT;
            if (t.getTreatmentOption() != null && !t.getTreatmentOption().isBlank()) {
                try {
                    treatment = TreatmentOption.valueOf(t.getTreatmentOption().trim());
                } catch (Exception ignored) {
                    treatment = TreatmentOption.TREAT;
                }
            }
            String treatJust = treatment == TreatmentOption.TREAT
                    ? null
                    : firstNonBlank(t.getTreatmentRecommendation(),
                            "Seeded from curated library (" + t.getSourceRiskId() + ")");
            CreateRiskRequest req = new CreateRiskRequest(
                    RiskType.IS_RISK,
                    null,
                    t.getTitle(),
                    firstNonBlank(t.getDescription(), t.getTitle()),
                    category,
                    null,
                    null,
                    null,
                    frameworkId,
                    frameworkCode,
                    t.getFrameworkNodeId(),
                    nodeCode,
                    null,
                    "Seeded from curated library (" + t.getSourceRiskId() + ") for compliance project "
                            + complianceProjectId,
                    t.getTypicalThreats() != null && !t.getTypicalThreats().isBlank()
                            ? t.getTypicalThreats()
                            : "Operational Failure",
                    probability,
                    impact,
                    null,
                    controlEff,
                    null,
                    null,
                    null,
                    treatment,
                    t.getTreatmentRecommendation(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    treatJust,
                    null, null, null, null);
            RiskRecordResponse created = create(user, req);
            RiskRecord risk = riskRepository.findById(created.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Seeded risk missing"));
            risk.setSourceComplianceProjectId(complianceProjectId);
            risk.setSourceRiskTemplateId(t.getId());
            riskRepository.save(risk);
            seeded++;
        }
        return seeded;
    }

    /**
     * Raise an IS Risk from a Gap control item (pre-populate framework / node / observed gap).
     * Caller must supply scoring + org unit via {@code overrides} (orgUnitId required).
     */
    @Transactional
    public RiskRecordResponse raiseFromGap(User admin, long assessmentId, long itemId,
                                           CreateRiskRequest overrides) {
        GapAssessment assessment = gapAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found"));
        GapItem item = gapItemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        if (overrides == null || overrides.orgUnitId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "orgUnitId is required when raising a risk from Gap");
        }
        String title = overrides.title() != null && !overrides.title().isBlank()
                ? overrides.title()
                : (item.getTitle() != null && !item.getTitle().isBlank()
                ? item.getTitle()
                : "Risk from " + item.getNodeCode());
        String observed = overrides.observedGap() != null && !overrides.observedGap().isBlank()
                ? overrides.observedGap()
                : "Raised from Gap “" + assessment.getName() + "” / control " + item.getNodeCode();
        String description = firstNonBlank(overrides.description(), item.getTitle(), title);
        String threats = firstNonBlank(overrides.threats(), "Operational Failure");
        CreateRiskRequest req = new CreateRiskRequest(
                RiskType.IS_RISK,
                overrides.displayIdOverride(),
                title,
                description,
                overrides.category() != null ? overrides.category() : RiskCategory.COMPLIANCE,
                overrides.categoryOther(),
                overrides.orgUnitId(),
                overrides.assetId(),
                assessment.getFrameworkId(),
                assessment.getFrameworkCode(),
                item.getFrameworkNodeId(),
                item.getNodeCode(),
                overrides.uaeIaPriority(),
                observed,
                threats,
                overrides.probability() != null ? overrides.probability() : RiskLevel.MEDIUM,
                overrides.impact() != null ? overrides.impact() : RiskLevel.MEDIUM,
                overrides.existingControls(),
                overrides.controlEffectiveness() != null ? overrides.controlEffectiveness() : RiskLevel.MEDIUM,
                null,
                null,
                null,
                overrides.treatmentOption(),
                overrides.treatmentPlan(),
                overrides.treatmentPlanDetail(),
                overrides.riskOwnerUserId(),
                overrides.adminNotes(),
                null,
                null,
                null,
                overrides.overrideAutoClose(),
                overrides.autoCloseOverrideJustification(),
                overrides.additionalFrameworks(),
                assessment.getId(),
                item.getId(),
                overrides.infosecStatusInitial(),
                overrides.riskProjectId(),
                overrides.dateIdentified(),
                overrides.cause(),
                overrides.residualScore(),
                overrides.targetControlEffectiveness(),
                overrides.assignedContributorId(),
                overrides.nextReviewDate(),
                overrides.thirdPartyName(),
                overrides.treatmentJustification(),
                overrides.compensatingControls(), null, null, null);
        return create(admin, req);
    }

    @Transactional(readOnly = true)
    public List<EligibleContributorResponse> eligibleContributors(Long orgUnitId) {
        return contributorEligibilityService.listForOrgUnit(orgUnitId);
    }

    @Transactional
    public RiskRecordResponse update(User user, long id, UpdateRiskRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        assertEditable(risk);
        if (req.orgUnitId() != null || req.assetId() != null || req.riskOwnerUserId() != null) {
            validateRefs(
                    req.orgUnitId() != null ? req.orgUnitId() : risk.getOrgUnitId(),
                    req.assetId() != null ? req.assetId() : risk.getAssetId(),
                    req.riskOwnerUserId() != null ? req.riskOwnerUserId() : risk.getRiskOwnerUserId());
        }

        if (req.title() != null && !req.title().isBlank()) {
            String newTitle = req.title().trim();
            String current = risk.getTitle() == null ? "" : risk.getTitle().trim();
            if (!newTitle.equalsIgnoreCase(current)) {
                requireUniqueTitle(risk.getRiskProjectId(), newTitle, risk.getId());
            }
            risk.setTitle(newTitle);
        }
        if (req.description() != null) {
            risk.setDescription(blankToNull(req.description()));
        }
        if (req.category() != null) {
            risk.setCategory(req.category());
        }
        if (req.category() != null || req.categoryOther() != null) {
            RiskCategory cat = req.category() != null ? req.category() : risk.getCategory();
            String other = req.categoryOther() != null ? req.categoryOther() : risk.getCategoryOther();
            validateCategory(cat, other);
            if (cat == RiskCategory.OTHER) {
                risk.setCategoryOther(blankToNull(other));
            } else {
                risk.setCategoryOther(null);
            }
        }
        if (req.orgUnitId() != null) {
            risk.setOrgUnitId(req.orgUnitId());
        }
        if (risk.getRiskType() == RiskType.IS_RISK) {
            if (req.assetId() != null) {
                risk.setAssetId(req.assetId());
            }
            if (req.frameworkId() != null) {
                risk.setFrameworkId(req.frameworkId());
            }
            if (req.frameworkCode() != null) {
                risk.setFrameworkCode(blankToNull(req.frameworkCode()));
            }
            if (req.frameworkNodeId() != null) {
                risk.setFrameworkNodeId(req.frameworkNodeId());
            }
            if (req.nodeCode() != null) {
                risk.setNodeCode(blankToNull(req.nodeCode()));
            }
        }
        if (req.observedGap() != null) {
            risk.setObservedGap(blankToNull(req.observedGap()));
        }
        if (risk.getRiskType() == RiskType.ERM) {
            if (req.strategicObjective() != null) {
                if (req.strategicObjective().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Strategic objective is required for ERM risks");
                }
                risk.setStrategicObjective(req.strategicObjective().trim());
            }
            if (req.financialImpactAed() != null) {
                risk.setFinancialImpactAed(req.financialImpactAed());
            }
        }
        if (req.threats() != null) {
            risk.setThreats(blankToNull(req.threats()));
        }
        if (req.existingControls() != null) {
            risk.setExistingControls(blankToNull(req.existingControls()));
        }
        ScoreSnapshot beforeScores = ScoreSnapshot.of(risk);
        if (req.probability() != null) {
            risk.setProbability(req.probability());
        }
        if (req.impact() != null) {
            risk.setImpact(req.impact());
        }
        if (req.residualScore() != null) {
            requireProductScoreInRange(
                    req.residualScore(),
                    "Residual Risk Score",
                    inherentProduct(risk.getProbability(), risk.getImpact()));
        }
        if (req.controlEffectiveness() != null) {
            risk.setControlEffectiveness(req.controlEffectiveness());
        }
        if (req.dateIdentified() != null) {
            risk.setDateIdentified(req.dateIdentified());
        }
        if (req.cause() != null) {
            risk.setCause(blankToNull(req.cause()));
        }
        if (req.targetControlEffectiveness() != null) {
            risk.setTargetControlEffectiveness(req.targetControlEffectiveness());
        }
        if (req.nextReviewDate() != null) {
            risk.setNextReviewDate(req.nextReviewDate());
        }
        if (req.thirdPartyName() != null) {
            risk.setThirdPartyName(blankToNull(req.thirdPartyName()));
        }
        if (req.treatmentJustification() != null) {
            risk.setTreatmentChangeJustification(blankToNull(req.treatmentJustification()));
        }
        if (req.compensatingControls() != null) {
            risk.setTransferCompensatingControls(blankToNull(req.compensatingControls()));
        }
        if (req.residualScoringMode() != null
                || req.residualProbability() != null
                || req.residualImpact() != null) {
            applyResidualScoringFields(
                    risk,
                    req.residualScoringMode(),
                    req.residualProbability(),
                    req.residualImpact(),
                    false);
        }
        if (req.residualScore() != null) {
            risk.setResidualScore(req.residualScore());
        }
        if (req.treatmentOption() != null) {
            risk.setTreatmentOption(req.treatmentOption());
        }
        if (req.treatmentPlan() != null) {
            risk.setTreatmentPlan(blankToNull(req.treatmentPlan()));
        }
        Long previousOwnerId = risk.getRiskOwnerUserId();
        if (req.riskOwnerUserId() != null) {
            risk.setRiskOwnerUserId(req.riskOwnerUserId());
        }
        if (req.adminNotes() != null) {
            risk.setAdminNotes(blankToNull(req.adminNotes()));
        }
        if (req.overrideAutoClose() != null) {
            if (Boolean.TRUE.equals(req.overrideAutoClose())) {
                applyAutoCloseOverrideFlags(true, req.autoCloseOverrideJustification());
                risk.setAutoCloseOverride(true);
                risk.setAutoCloseOverrideJustification(req.autoCloseOverrideJustification().trim());
            } else {
                risk.setAutoCloseOverride(false);
                risk.setAutoCloseOverrideJustification(null);
            }
        }
        requireFilledIdentification(risk);
        recomputeScores(risk);
        resolveUaeIaPriority(risk, req.uaeIaPriority());
        if (risk.getStatus() == RiskStatus.DRAFT || risk.getStatus() == RiskStatus.RE_OPENED) {
            if (!risk.isAutoCloseOverride() && isExcelAutoClosable(risk)) {
                risk.setStatus(RiskStatus.AUTO_CLOSED);
                risk.setClosedAt(Instant.now());
                riskRepository.save(risk);
                notifyAutoClosed(risk);
            } else if (risk.isAutoCloseOverride() && risk.getStatus() == RiskStatus.AUTO_CLOSED) {
                risk.setStatus(RiskStatus.DRAFT);
                risk.setClosedAt(null);
            }
        }
        riskRepository.save(risk);
        if (req.additionalFrameworks() != null) {
            replaceAdditionalFrameworks(risk.getId(), req.additionalFrameworks());
        }
        if (req.treatmentPlanDetail() != null) {
            applyTreatmentPlanDraft(risk, req.treatmentPlanDetail(), req.treatmentPlan());
        }
        recordScoreHistoryIfChanged(risk, user, beforeScores,
                blankToNull(req.scoreChangeReason()) != null
                        ? req.scoreChangeReason().trim()
                        : "Identification update");

        notifyRiskOwnerNamed(risk, previousOwnerId);
        audit(user, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(), null, null);
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse assign(User admin, long id, AssignRiskRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.DRAFT && risk.getStatus() != RiskStatus.RE_OPENED
                && risk.getStatus() != RiskStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Risk must be DRAFT, RE_OPENED, or ASSIGNED to assign or reassign");
        }
        if (risk.getStatus() == RiskStatus.ASSIGNED && risk.getWorkTaskId() != null) {
            WorkTask existing = workTaskRepository.findById(risk.getWorkTaskId()).orElse(null);
            if (existing != null && existing.getStatus() == WorkTaskStatus.SUBMITTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot reassign while Contributor submission is in review");
            }
        }
        if (isExcelAutoClosable(risk)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Auto-closed risks do not require assignment");
        }
        User assignee = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        if (!assignee.getRoles().contains(Role.CONTRIBUTOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must have CONTRIBUTOR role");
        }
        if (risk.getOrgUnitId() != null) {
            boolean inUnit = userOrgUnitRepository.findByUserId(assignee.getId()).stream()
                    .anyMatch(m -> risk.getOrgUnitId().equals(m.getOrgUnitId()));
            if (!inUnit) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Assignee must belong to the risk's division/department");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Set division/department on the risk before assigning a Contributor");
        }

        if (req.treatmentOption() != null) {
            risk.setTreatmentOption(req.treatmentOption());
        } else if (risk.getTreatmentOption() == null) {
            risk.setTreatmentOption(TreatmentOption.TREAT);
        }
        if (req.treatmentPlan() != null) {
            risk.setTreatmentPlan(blankToNull(req.treatmentPlan()));
        }
        LocalDate dueDate = req.dueDate();
        if (risk.getRiskProjectId() != null) {
            dueDate = riskTargetDate(risk);
            if (dueDate == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Set the Target Date on the risk before assigning a Risk Custodian");
            }
        }

        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_RECORD, risk.getId())
                .orElseGet(WorkTask::new);
        task.setSourceModule(MyTasksService.MODULE_RISK);
        task.setSourceRecordType(MyTasksService.TYPE_RISK_RECORD);
        task.setSourceRecordId(risk.getId());
        task.setTitle(risk.getDisplayId() + ": " + risk.getTitle());
        task.setDetail(risk.getCategory().name() + " · residual " + risk.getResidualRisk().name());
        task.setAssignedToUserId(assignee.getId());
        task.setAssignedByUserId(admin.getId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(dueDate);
        task.setSubmittedAt(null);
        workTaskRepository.save(task);

        risk.setAssignedContributorId(assignee.getId());
        risk.setWorkTaskId(task.getId());
        risk.setStatus(RiskStatus.ASSIGNED);
        risk.setAssignedAt(Instant.now());
        risk.setAssignmentChaseSentAt(null);
        risk.setRejectionReason(null);
        riskRepository.save(risk);

        Map<String, String> mailVars = riskMailVars(risk);
        notificationService.trigger(
                "RISK_ASSIGNED",
                String.valueOf(risk.getId()),
                List.of(assignee.getId().toString()),
                mailVars);
        Long ownerId = risk.getRiskOwnerUserId();
        if (ownerId != null && !ownerId.equals(assignee.getId())) {
            notificationService.triggerEmail(
                    "RISK_OWNER_NAMED",
                    String.valueOf(risk.getId()),
                    List.of(String.valueOf(ownerId)),
                    mailVars);
        }

        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "assignedTo", assignee.getEmail());
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse reassignOwner(User admin, long id, long userId) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() == RiskStatus.CLOSED
                || risk.getStatus() == RiskStatus.AUTO_CLOSED
                || risk.getStatus() == RiskStatus.ACCEPTED
                || risk.getStatus() == RiskStatus.AVOIDED
                || risk.getStatus() == RiskStatus.TRANSFERRED
                || risk.getStatus() == RiskStatus.CANCELLED
                || risk.getStatus() == RiskStatus.TOLERATED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Risk Owner cannot be changed after this risk is finished");
        }
        if (userRepository.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk Owner was not found");
        }
        Long previous = risk.getRiskOwnerUserId();
        risk.setRiskOwnerUserId(userId);
        riskRepository.save(risk);
        acceptanceFormRepository.findByRiskId(risk.getId()).ifPresent(form -> {
            if (form.getStatus() == AcceptanceFormStatus.PENDING_APPROVER_2
                    || form.getStatus() == AcceptanceFormStatus.PENDING_APPROVER_3
                    || form.getStatus() == AcceptanceFormStatus.PENDING_ADMIN
                    || form.getStatus() == AcceptanceFormStatus.ACCEPTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Risk Owner cannot be changed after they have approved");
            }
            form.setApprover1UserId(userId);
            acceptanceFormRepository.save(form);
            if (form.getStatus() == AcceptanceFormStatus.PENDING_RISK_OWNER
                    && previous != null
                    && !previous.equals(userId)) {
                workTaskRepository
                        .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                                MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_ACCEPTANCE, risk.getId(), previous)
                        .ifPresent(task -> {
                            if (task.getStatus() != WorkTaskStatus.SUBMITTED
                                    && task.getStatus() != WorkTaskStatus.CANCELLED) {
                                task.setStatus(WorkTaskStatus.CANCELLED);
                                workTaskRepository.save(task);
                            }
                        });
                assignAcceptanceTask(risk, form, userId, "Risk Owner", admin.getId());
            }
        });
        notifyRiskOwnerNamed(risk, previous);
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "riskOwnerUserId", String.valueOf(userId));
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse cancel(User admin, long id, CancelRiskRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an assigned risk can be cancelled");
        }
        if (risk.getWorkTaskId() != null) {
            workTaskRepository.findById(risk.getWorkTaskId()).ifPresent(task -> {
                task.setStatus(WorkTaskStatus.CANCELLED);
                workTaskRepository.save(task);
            });
        }
        String reason = req != null ? blankToNull(req.reason()) : null;
        if (reason != null) {
            risk.setRejectionReason(reason);
        }
        risk.setStatus(RiskStatus.CANCELLED);
        risk.setClosedAt(Instant.now());
        riskRepository.save(risk);
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "CANCELLED");
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse submit(User contributor, long id, SubmitRiskRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.ASSIGNED && risk.getStatus() != RiskStatus.RE_OPENED
                && risk.getStatus() != RiskStatus.PENDING_ACCEPTANCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Risk is not assigned");
        }
        if (!contributor.getId().equals(risk.getAssignedContributorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this risk");
        }
        boolean resubmitted = risk.getStatus() == RiskStatus.RE_OPENED
                && risk.getRejectionReason() != null && !risk.getRejectionReason().isBlank();

        TreatmentOption proposed = req.treatmentOption() != null ? req.treatmentOption() : risk.getTreatmentOption();
        if (proposed == null) {
            proposed = TreatmentOption.TREAT;
        }
        if (Boolean.FALSE.equals(req.confirmTreatment()) || proposed != risk.getTreatmentOption()) {
            if (req.justification() == null || req.justification().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Justification required when changing treatment option");
            }
            risk.setTreatmentChangeJustification(req.justification().trim());
        }
        risk.setTreatmentOption(proposed);

        if (req.infosecStatusContributor() != null) {
            risk.setInfosecStatusContributor(blankToNull(req.infosecStatusContributor()));
        }

        if (req.comments() != null && !req.comments().isBlank()) {
            if (risk.getRiskProjectId() != null) {
                risk.setCustodianComments(req.comments().trim());
            } else {
                risk.setTreatmentPlan(mergeComments(risk.getTreatmentPlan(), req.comments().trim()));
            }
        }

        if (risk.getRiskProjectId() != null && risk.getStatus() != RiskStatus.PENDING_ACCEPTANCE) {
            String plan = req.actionDescription() != null ? req.actionDescription() : risk.getTreatmentPlan();
            if (plan == null || plan.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Treatment Action Plan Detail is required");
            }
        }

        requireRevisionIfReturned(risk, req);

        if (risk.getRiskProjectId() != null && risk.getStatus() == RiskStatus.PENDING_ACCEPTANCE) {
            startFourTApproval(risk, contributor, req, proposed);
            risk.setSubmittedAt(Instant.now());
            riskRepository.save(risk);
            if (risk.getWorkTaskId() != null) {
                workTaskRepository.findById(risk.getWorkTaskId()).ifPresent(task -> {
                    task.setStatus(WorkTaskStatus.SUBMITTED);
                    task.setSubmittedAt(Instant.now());
                    workTaskRepository.save(task);
                });
            }
            Map<String, String> acceptanceVars = riskMailVars(risk);
            acceptanceVars.put(AppNotificationMailComposer.RISK_MAIL_KIND, "ACCEPTANCE");
            notifyAdmins(risk, "RISK_SUBMITTED", acceptanceVars);
            audit(contributor, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                    "status", "PENDING_ACCEPTANCE");
            return toResponse(risk);
        }

        if (risk.getRiskProjectId() != null) {
            applyTreatOnSubmit(risk, req);
        } else {
            switch (proposed) {
                case TREAT -> applyTreatOnSubmit(risk, req);
                case TOLERATE -> applyTolerateOnSubmit(risk, req);
                case TERMINATE -> applyTerminateOnSubmit(risk, req);
                case TRANSFER -> applyTransferOnSubmit(risk, req);
            }
        }

        risk.setStatus(RiskStatus.SUBMITTED);
        risk.setSubmittedAt(Instant.now());
        risk.setRejectionReason(null);
        riskRepository.save(risk);

        if (risk.getWorkTaskId() != null) {
            workTaskRepository.findById(risk.getWorkTaskId()).ifPresent(task -> {
                task.setStatus(WorkTaskStatus.SUBMITTED);
                task.setSubmittedAt(Instant.now());
                workTaskRepository.save(task);
            });
        }

        Map<String, String> submitVars = riskMailVars(risk);
        submitVars.put(AppNotificationMailComposer.RISK_MAIL_KIND, resubmitted ? "RESUBMITTED" : "SUBMITTED");
        notifyAdmins(risk, "RISK_SUBMITTED", submitVars);

        audit(contributor, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "SUBMITTED");
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse startValidation(User admin, long id) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Risk must be SUBMITTED to validate");
        }
        risk.setStatus(RiskStatus.UNDER_VALIDATION);
        risk.setValidatedAt(Instant.now());
        risk.setValidatedByUserId(admin.getId());
        riskRepository.save(risk);
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "UNDER_VALIDATION");
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse rejectValidation(User admin, long id, RejectValidationRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.SUBMITTED && risk.getStatus() != RiskStatus.UNDER_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Risk must be SUBMITTED or UNDER_VALIDATION to reject");
        }
        returnSendToContributor(risk, req.reason().trim(), admin, "RISK_RETURNED");
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse approveValidation(User admin, long id, ValidateApproveRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.UNDER_VALIDATION && risk.getStatus() != RiskStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Risk must be UNDER_VALIDATION (or SUBMITTED) to approve treatment");
        }
        if (risk.getStatus() == RiskStatus.SUBMITTED) {
            risk.setStatus(RiskStatus.UNDER_VALIDATION);
            risk.setValidatedAt(Instant.now());
            risk.setValidatedByUserId(admin.getId());
        }
        if (req != null && req.validationNotes() != null) {
            risk.setValidationNotes(blankToNull(req.validationNotes()));
        }
        if (req != null && req.infosecStatusFinal() != null) {
            risk.setInfosecStatusFinal(blankToNull(req.infosecStatusFinal()));
        }

        TreatmentOption option = risk.getTreatmentOption();
        if (option == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Treatment option is required");
        }

        return switch (option) {
            case TREAT -> approveTreat(admin, risk);
            case TOLERATE -> approveTolerate(admin, risk);
            case TERMINATE -> approveTerminate(admin, risk);
            case TRANSFER -> approveTransfer(admin, risk);
        };
    }

    @Transactional
    public RiskRecordResponse upsertTreatmentPlan(User admin, long id, UpsertTreatmentPlanRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.MITIGATING && risk.getStatus() != RiskStatus.UNDER_VALIDATION
                && risk.getStatus() != RiskStatus.SUBMITTED && risk.getStatus() != RiskStatus.ASSIGNED
                && risk.getStatus() != RiskStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot edit treatment plan in this status");
        }
        TreatmentPlan plan = ensurePlan(risk);
        applyPlanFields(plan, req.actionDescription(), req.actionOwnerId(), req.custodianUserId(), req.startDate(),
                req.targetDate(), req.resourcesRequired(), req.costEstimateAed(), req.evidenceRequired());
        if (Boolean.TRUE.equals(req.markInProgress()) && plan.getStatus() == TreatmentPlanStatus.NOT_STARTED) {
            plan.setStatus(TreatmentPlanStatus.IN_PROGRESS);
        }
        treatmentPlanRepository.save(plan);
        if (req.actionDescription() != null && !req.actionDescription().isBlank()) {
            risk.setTreatmentPlan(req.actionDescription().trim());
            riskRepository.save(risk);
        }
        syncCustodianTaskDueDate(risk, plan);
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "treatmentPlan", "updated");
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse addMilestone(User user, long id, CreateMilestoneRequest req) {
        RiskRecord risk = requireVisible(user, id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.MITIGATING && risk.getStatus() != RiskStatus.UNDER_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Milestones only while mitigating/validating");
        }
        if (risk.getRiskProjectId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Project risks finish at Approve (Action + Next Review Date). Milestones are not used.");
        }
        assertCanEditMilestones(user, risk);
        TreatmentPlan plan = ensurePlan(risk);
        DateRules.requireActionDueDateIfPresent(req.dueDate(), "Due date");
        TreatmentMilestone m = new TreatmentMilestone();
        m.setTreatmentPlanId(plan.getId());
        m.setMilestoneTitle(req.milestoneTitle().trim());
        m.setDueDate(req.dueDate());
        m.setCompleted(false);
        milestoneRepository.save(m);
        if (plan.getStatus() == TreatmentPlanStatus.NOT_STARTED) {
            plan.setStatus(TreatmentPlanStatus.IN_PROGRESS);
            treatmentPlanRepository.save(plan);
        }
        syncMilestoneTasks(risk, plan, user.getId());
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse completeMilestone(User user, long riskId, long milestoneId) {
        return completeMilestone(user, riskId, milestoneId, null);
    }

    @Transactional
    public RiskRecordResponse completeMilestone(User user, long riskId, long milestoneId, String progressNotes) {
        RiskRecord risk = requireVisible(user, riskId);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.MITIGATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Complete milestones only while MITIGATING");
        }
        if (risk.getRiskProjectId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Project risks finish at Approve (Action + Next Review Date). Milestones are not used.");
        }
        assertCanEditMilestones(user, risk);
        TreatmentPlan plan = treatmentPlanRepository.findByRiskId(risk.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Treatment plan not found"));
        TreatmentMilestone m = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
        if (!plan.getId().equals(m.getTreatmentPlanId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Milestone does not belong to this risk");
        }
        m.setCompleted(true);
        m.setCompletedDate(LocalDate.now());
        m.setCompletedByUserId(user.getId());
        if (progressNotes != null) {
            m.setProgressNotes(blankToNull(progressNotes));
        }
        requireMilestoneEvidence(m.getId());
        milestoneRepository.save(m);
        markMilestoneTaskSubmitted(m.getId());

        List<TreatmentMilestone> all = milestoneRepository
                .findByTreatmentPlanIdOrderByDueDateAscIdAsc(plan.getId());
        if (!all.isEmpty() && all.stream().allMatch(TreatmentMilestone::isCompleted)) {
            plan.setStatus(TreatmentPlanStatus.COMPLETED);
            plan.setCompletionDate(LocalDate.now());
            treatmentPlanRepository.save(plan);
        } else if (plan.getStatus() == TreatmentPlanStatus.NOT_STARTED) {
            plan.setStatus(TreatmentPlanStatus.IN_PROGRESS);
            treatmentPlanRepository.save(plan);
        }
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse changeTreatmentOption(User admin, long id, ChangeTreatmentRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.SUBMITTED && risk.getStatus() != RiskStatus.UNDER_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Treatment option can only be changed post-submission while SUBMITTED or UNDER_VALIDATION");
        }
        TreatmentOption previous = risk.getTreatmentOption();
        risk.setTreatmentOption(req.treatmentOption());
        risk.setTreatmentChangeJustification(
                "Admin post-submission change from " + previous + ": " + req.justification().trim());
        riskRepository.save(risk);
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "treatmentOption", req.treatmentOption().name());
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse close(User admin, long id, CloseRiskRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.MITIGATING && risk.getStatus() != RiskStatus.OVERDUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only MITIGATING or OVERDUE risks can be closed");
        }
        if (risk.getRiskProjectId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Project risks finish at Approve (Action + Next Review Date). Close is not used.");
        }
        TreatmentPlan plan = treatmentPlanRepository.findByRiskId(risk.getId()).orElse(null);
        if (plan != null) {
            List<TreatmentMilestone> milestones = milestoneRepository
                    .findByTreatmentPlanIdOrderByDueDateAscIdAsc(plan.getId());
            boolean milestonesOk = milestones.isEmpty()
                    || milestones.stream().allMatch(TreatmentMilestone::isCompleted);
            if (plan.getStatus() != TreatmentPlanStatus.COMPLETED && !milestonesOk) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Complete treatment plan or all milestones before closing");
            }
            if (plan.getStatus() != TreatmentPlanStatus.COMPLETED) {
                plan.setStatus(TreatmentPlanStatus.COMPLETED);
                plan.setCompletionDate(LocalDate.now());
                treatmentPlanRepository.save(plan);
            }
        }
        if (req != null) {
            ScoreSnapshot beforeClose = ScoreSnapshot.of(risk);
            if (req.probability() != null) {
                risk.setProbability(req.probability());
            }
            if (req.impact() != null) {
                risk.setImpact(req.impact());
            }
            if (req.controlEffectiveness() != null) {
                risk.setControlEffectiveness(req.controlEffectiveness());
            }
            // Excel residual = residual P×I; on close, re-score residual from final P×I when provided.
            if (risk.getResidualScoringMode() == ResidualScoringMode.MANUAL_PROBABILITY_IMPACT
                    || risk.getResidualScoringMode() == null) {
                if (req.probability() != null) {
                    risk.setResidualProbability(req.probability());
                }
                if (req.impact() != null) {
                    risk.setResidualImpact(req.impact());
                }
                if (risk.getResidualScoringMode() == null) {
                    risk.setResidualScoringMode(ResidualScoringMode.MANUAL_PROBABILITY_IMPACT);
                }
            }
            if (req.probability() != null || req.impact() != null || req.controlEffectiveness() != null) {
                recomputeScores(risk);
                recordScoreHistoryIfChanged(risk, admin, beforeClose,
                        blankToNull(req.closeNotes()) != null
                                ? req.closeNotes().trim()
                                : "Close residual re-score");
            }
            if (req.closeNotes() != null && !req.closeNotes().isBlank()) {
                risk.setValidationNotes(blankToNull(req.closeNotes()));
            }
        }
        risk.setStatus(RiskStatus.CLOSED);
        risk.setClosedAt(Instant.now());
        riskRepository.save(risk);
        notifyFinalDecision(risk, "CLOSED", "Treat path closed");
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "CLOSED");
        return toResponse(risk);
    }

    /** @deprecated Prefer validate + path-specific approve; kept for brief compatibility. */
    @Transactional
    public RiskRecordResponse approve(User admin, long id) {
        return approveValidation(admin, id, new ValidateApproveRequest(null, null));
    }

    @Transactional
    public RiskRecordResponse ownerDecideAcceptance(User user, long id, AcceptanceOwnerDecideRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        RiskAcceptanceForm pendingForm = acceptanceFormRepository.findByRiskId(risk.getId()).orElse(null);
        if (pendingForm != null && pendingForm.getApprover2UserId() != null) {
            return approverDecideChain(user, risk, pendingForm, req);
        }
        if (risk.getStatus() != RiskStatus.PENDING_ACCEPTANCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Risk is not awaiting Risk Owner");
        }
        if (risk.getRiskOwnerUserId() == null || !risk.getRiskOwnerUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the designated Risk Owner may decide");
        }
        if (!Boolean.TRUE.equals(req.declaration())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk Owner declaration is required");
        }
        if (req.decision() != AcceptanceDecision.APPROVED && req.decision() != AcceptanceDecision.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }
        RiskAcceptanceForm form = acceptanceFormRepository.findByRiskId(risk.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The custodian has not submitted the acceptance form yet"));
        form.setRiskOwnerDecision(req.decision());
        form.setRiskOwnerComments(blankToNull(req.comments()));
        form.setRiskOwnerReviewedAt(Instant.now());

        if (req.decision() == AcceptanceDecision.REJECTED) {
            form.setStatus(AcceptanceFormStatus.REJECTED);
            acceptanceFormRepository.save(form);
            cancelAcceptanceTasks(risk);
            String reason = blankToNull(req.comments()) != null ? req.comments().trim()
                    : "Risk Owner rejected acceptance";
            returnSendToContributor(risk, reason, user, null);
            notifyAcceptanceReturned(risk, reason, user, "RISK_OWNER_REJECTED", "the Risk Owner");
            return toResponse(risk);
        }

        form.setStatus(AcceptanceFormStatus.PENDING_ADMIN);
        acceptanceFormRepository.save(form);
        risk.setStatus(RiskStatus.PENDING_ADMIN_REVIEW);
        riskRepository.save(risk);
        completeAcceptanceTask(risk);
        notifyPendingAdmin(risk);
        audit(user, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "PENDING_ADMIN_REVIEW");
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse adminDecideAcceptance(User admin, long id, AcceptanceAdminDecideRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.PENDING_ADMIN_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Risk is not awaiting Admin acceptance");
        }
        if (!Boolean.TRUE.equals(req.declaration())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin declaration is required");
        }
        if (req.decision() != AcceptanceDecision.ACCEPTED && req.decision() != AcceptanceDecision.SENT_BACK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be ACCEPTED or SENT_BACK");
        }
        RiskAcceptanceForm form = acceptanceFormRepository.findByRiskId(risk.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acceptance form missing"));
        form.setAdminDecision(req.decision());
        form.setAdminComments(blankToNull(req.comments()));
        form.setAdminReviewedAt(Instant.now());

        if (req.decision() == AcceptanceDecision.SENT_BACK) {
            form.setStatus(AcceptanceFormStatus.REJECTED);
            acceptanceFormRepository.save(form);
            cancelAcceptanceTasks(risk);
            returnAcceptanceToContributor(risk, blankToNull(req.comments()) != null ? req.comments().trim()
                    : "Admin sent back acceptance", admin, "ADMIN_FINAL_DECISION", "the Administrator");
            return toResponse(risk);
        }

        form.setStatus(AcceptanceFormStatus.ACCEPTED);
        LocalDate review = req.nextReviewDate() != null ? req.nextReviewDate() : form.getReviewDate();
        if (review == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Next Review Date is required");
        }
        DateRules.requireActionDueDateIfPresent(review, "Next Review Date");
        form.setReviewDate(review);
        acceptanceFormRepository.save(form);
        TreatmentFormKind kind = form.getFormKind() != null ? form.getFormKind() : TreatmentFormKind.ACCEPT;
        if (kind == TreatmentFormKind.TRANSFER) {
            risk.setStatus(RiskStatus.TRANSFERRED);
        } else if (kind == TreatmentFormKind.TERMINATE) {
            risk.setStatus(RiskStatus.AVOIDED);
        } else if (risk.getRiskProjectId() != null) {
            risk.setStatus(RiskStatus.ACCEPTED);
        } else {
            risk.setStatus(RiskStatus.TOLERATED);
        }
        risk.setRejectionReason(null);
        risk.setFinalRiskStatus(FinalRiskStatus.APPROVE);
        risk.setNextReviewDate(review);
        risk.setClosedAt(Instant.now());
        riskRepository.save(risk);

        saveAcceptanceEvidenceSnapshot(admin, risk, form);

        notificationService.triggerInApp(
                "RISK_TOLERATED",
                String.valueOf(risk.getId()),
                List.of(
                        String.valueOf(risk.getAssignedContributorId()),
                        String.valueOf(risk.getRiskOwnerUserId())),
                Map.of(
                        "displayId", risk.getDisplayId(),
                        "title", risk.getTitle(),
                        "reviewDate", form.getReviewDate() != null ? form.getReviewDate().toString() : "-"));
        notifyFinalDecision(risk, "TOLERATED", "Acceptance approved");

        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "TOLERATED");
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse reopen(User admin, long id, ReopenRiskRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (!REOPENABLE.contains(risk.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only CLOSED/AUTO_CLOSED/AVOIDED/TRANSFERRED/TOLERATED risks can be re-opened");
        }
        risk.setClosedAt(null);
        risk.setSubmittedAt(null);
        risk.setValidatedAt(null);
        risk.setRejectionReason(null);
        if (risk.getAssignedContributorId() != null) {
            risk.setStatus(RiskStatus.ASSIGNED);
            if (risk.getWorkTaskId() != null) {
                workTaskRepository.findById(risk.getWorkTaskId()).ifPresent(task -> {
                    task.setStatus(WorkTaskStatus.PENDING);
                    task.setSubmittedAt(null);
                    workTaskRepository.save(task);
                });
            }
        } else {
            risk.setStatus(RiskStatus.DRAFT);
        }
        riskRepository.save(risk);

        java.util.ArrayList<String> recipients = new java.util.ArrayList<>(riskAdmins(risk));
        if (risk.getAssignedContributorId() != null) {
            recipients.add(String.valueOf(risk.getAssignedContributorId()));
        }
        if (risk.getRiskOwnerUserId() != null) {
            recipients.add(String.valueOf(risk.getRiskOwnerUserId()));
        }
        notificationService.trigger(
                "RISK_RE_OPENED",
                String.valueOf(risk.getId()),
                recipients,
                Map.of(
                        "displayId", risk.getDisplayId(),
                        "title", risk.getTitle(),
                        "reason", req.reason().trim()));

        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "reopenReason", req.reason().trim());
        return toResponse(risk);
    }

    private RiskRecordResponse approveTreat(User admin, RiskRecord risk) {
        TreatmentPlan plan = ensurePlan(risk);
        if ((plan.getActionDescription() == null || plan.getActionDescription().isBlank())
                && risk.getTreatmentPlan() != null) {
            plan.setActionDescription(risk.getTreatmentPlan());
        }
        if (plan.getActionDescription() == null || plan.getActionDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Treatment plan action description is required for Treat path");
        }
        if (plan.getStatus() == TreatmentPlanStatus.NOT_STARTED) {
            plan.setStatus(TreatmentPlanStatus.IN_PROGRESS);
        }
        if (plan.getStartDate() == null) {
            plan.setStartDate(LocalDate.now());
        }
        treatmentPlanRepository.save(plan);
        risk.setStatus(RiskStatus.MITIGATING);
        riskRepository.save(risk);
        syncMilestoneTasks(risk, plan, admin.getId());
        notificationService.trigger(
                "RISK_MITIGATING",
                String.valueOf(risk.getId()),
                List.of(String.valueOf(risk.getAssignedContributorId())),
                Map.of("displayId", risk.getDisplayId(), "title", risk.getTitle()));
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "MITIGATING");
        return toResponse(risk);
    }

    private RiskRecordResponse approveTolerate(User admin, RiskRecord risk) {
        RiskAcceptanceForm form = acceptanceFormRepository.findByRiskId(risk.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Acceptance form required for Tolerate path"));
        assertAcceptanceComplete(form);
        if (risk.getRiskOwnerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Risk Owner must be set before Tolerate approval");
        }
        form.setStatus(AcceptanceFormStatus.PENDING_RISK_OWNER);
        acceptanceFormRepository.save(form);
        risk.setStatus(RiskStatus.PENDING_ACCEPTANCE);
        risk.setNextReviewDate(form.getReviewDate());
        risk.setOwnerChaseSentAt(null);
        riskRepository.save(risk);
        assignAcceptanceTask(risk, form, risk.getRiskOwnerUserId(), "Risk Owner", admin.getId());
        notificationService.trigger(
                "RISK_PENDING_OWNER",
                String.valueOf(risk.getId()),
                List.of(String.valueOf(risk.getRiskOwnerUserId())),
                Map.of("displayId", risk.getDisplayId(), "title", risk.getTitle()));
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "PENDING_ACCEPTANCE");
        return toResponse(risk);
    }

    private void requireTerminateReady(RiskRecord risk) {
        if (risk.getRiskProjectId() == null) {
            requireEvidence(risk, "Terminate");
            if (risk.getEliminationJustification() == null || risk.getEliminationJustification().isBlank()
                    || risk.getEliminationHow() == null || risk.getEliminationHow().isBlank()
                    || !Boolean.TRUE.equals(risk.getActivityStopped())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Terminate path fields incomplete");
            }
        } else {
            requirePlanEvidenceIfSpecified(risk, "Terminate");
        }
    }

    private void requireTransferReady(RiskRecord risk) {
        if (risk.getRiskProjectId() == null) {
            requireEvidence(risk, "Transfer");
            if (risk.getTransferMethod() == null
                    || risk.getThirdPartyName() == null || risk.getThirdPartyName().isBlank()
                    || risk.getTransferDescription() == null || risk.getTransferDescription().isBlank()
                    || risk.getResidualAfterTransfer() == null || risk.getResidualAfterTransfer().isBlank()
                    || risk.getTransferCompensatingControls() == null
                    || risk.getTransferCompensatingControls().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer path fields incomplete");
            }
        } else {
            requirePlanEvidenceIfSpecified(risk, "Transfer");
        }
    }

    private RiskRecordResponse approveTerminate(User admin, RiskRecord risk) {
        requireTerminateReady(risk);
        risk.setStatus(RiskStatus.AVOIDED);
        risk.setClosedAt(Instant.now());
        riskRepository.save(risk);
        notifyFinalDecision(risk, "AVOIDED", "Terminate path approved");
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "AVOIDED");
        return toResponse(risk);
    }

    private RiskRecordResponse approveTransfer(User admin, RiskRecord risk) {
        requireTransferReady(risk);
        risk.setStatus(RiskStatus.TRANSFERRED);
        risk.setClosedAt(Instant.now());
        riskRepository.save(risk);
        notifyFinalDecision(risk, "TRANSFERRED", "Transfer path approved");
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "status", "TRANSFERRED");
        return toResponse(risk);
    }

    private void applyTreatOnSubmit(RiskRecord risk, SubmitRiskRequest req) {
        TreatmentPlan plan = ensurePlan(risk);
        applyPlanFields(plan,
                req.actionDescription() != null ? req.actionDescription() : risk.getTreatmentPlan(),
                req.actionOwnerId(),
                req.custodianUserId(),
                req.startDate(),
                req.targetDate(),
                req.resourcesRequired(),
                req.costEstimateAed(),
                req.evidenceRequired());
        if (plan.getActionDescription() == null || plan.getActionDescription().isBlank()) {
            if (risk.getTreatmentPlan() != null && !risk.getTreatmentPlan().isBlank()) {
                plan.setActionDescription(risk.getTreatmentPlan());
            }
        }
        if (plan.getActionDescription() != null && !plan.getActionDescription().isBlank()) {
            risk.setTreatmentPlan(plan.getActionDescription());
        }
        treatmentPlanRepository.save(plan);
        // Path B8: when Admin specified evidence required on the plan, enforce at submit.
        if (plan.getEvidenceRequired() != null && !plan.getEvidenceRequired().isBlank()) {
            requireEvidence(risk, "Treat");
        }
    }

    private void applyTolerateOnSubmit(RiskRecord risk, SubmitRiskRequest req) {
        if (risk.getRiskOwnerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Risk Owner must be set before Tolerate submit");
        }
        requireText(req.acceptanceJustification(), "Acceptance justification");
        requireText(req.conditionsOfAcceptance(), "Conditions of acceptance");
        requireText(req.compensatingControls(), "Compensating controls");
        if (req.compControlOwnerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compensating control owner is required");
        }
        if (userRepository.findById(req.compControlOwnerId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compensating control owner not found");
        }
        if (req.compControlEffectiveness() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compensating control effectiveness is required");
        }
        if (req.reviewDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review date is required");
        }
        DateRules.requireToleranceReviewDate(req.reviewDate());
        if (!Boolean.TRUE.equals(req.contributorDeclaration())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contributor declaration is required");
        }

        RiskAcceptanceForm form = acceptanceFormRepository.findByRiskId(risk.getId()).orElseGet(() -> {
            RiskAcceptanceForm f = new RiskAcceptanceForm();
            f.setRiskId(risk.getId());
            return f;
        });
        form.setJustification(req.acceptanceJustification().trim());
        form.setBusinessRationale(blankToNull(req.businessRationale()));
        form.setConditionsOfAcceptance(req.conditionsOfAcceptance().trim());
        form.setCompensatingControls(req.compensatingControls().trim());
        form.setCompControlOwnerId(req.compControlOwnerId());
        form.setCompControlEffectiveness(req.compControlEffectiveness());
        form.setReviewDate(req.reviewDate());
        form.setContributorDeclaration(true);
        form.setStatus(AcceptanceFormStatus.DRAFT);
        form.setRiskOwnerDecision(null);
        form.setAdminDecision(null);
        acceptanceFormRepository.save(form);
        risk.setNextReviewDate(req.reviewDate());
    }

    private void applyTerminateOnSubmit(RiskRecord risk, SubmitRiskRequest req) {
        requireText(req.eliminationJustification(), "Elimination justification");
        requireText(req.eliminationHow(), "How risk is eliminated");
        if (!Boolean.TRUE.equals(req.activityStopped())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm activity stopped is required");
        }
        requireEvidence(risk, "Terminate");
        risk.setEliminationJustification(req.eliminationJustification().trim());
        risk.setEliminationHow(req.eliminationHow().trim());
        risk.setActivityStopped(true);
    }

    private void applyTransferOnSubmit(RiskRecord risk, SubmitRiskRequest req) {
        if (req.transferMethod() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer method is required");
        }
        requireText(req.thirdPartyName(), "Third party name");
        requireText(req.transferDescription(), "Transfer description");
        requireText(req.residualAfterTransfer(), "Residual risk after transfer");
        requireText(req.transferCompensatingControls(), "Compensating controls during transfer");
        requireEvidence(risk, "Transfer");
        risk.setTransferMethod(req.transferMethod());
        risk.setThirdPartyName(req.thirdPartyName().trim());
        risk.setTransferDescription(req.transferDescription().trim());
        risk.setResidualAfterTransfer(req.residualAfterTransfer().trim());
        risk.setTransferCompensatingControls(req.transferCompensatingControls().trim());
    }

    private void saveDecisionRemark(RiskRecord risk, User admin, String remark) {
        RiskStageNote note = new RiskStageNote();
        note.setRiskId(risk.getId());
        note.setWorkflowStage(risk.getStatus());
        note.setBody(remark.trim());
        note.setInternalOnly(false);
        note.setCreatedByUserId(admin.getId());
        stageNoteRepository.save(note);
    }

    private void returnSendToContributor(RiskRecord risk, String reason, User actor, String notifyTrigger) {
        risk.setRejectionReason(reason);
        risk.setStatus(RiskStatus.RE_OPENED);
        risk.setSubmittedAt(null);
        risk.setValidatedAt(null);
        riskRepository.save(risk);
        if (risk.getWorkTaskId() != null) {
            workTaskRepository.findById(risk.getWorkTaskId()).ifPresent(task -> {
                task.setStatus(WorkTaskStatus.PENDING);
                task.setSubmittedAt(null);
                workTaskRepository.save(task);
            });
        }
        if (notifyTrigger != null) {
            Map<String, String> vars = riskMailVars(risk);
            vars.put("reason", reason != null ? reason : "-");
            vars.put("decision", "SENT_BACK");
            vars.put("detail", reason != null ? reason : "-");
            vars.put(AppNotificationMailComposer.RISK_MAIL_KIND, "SENT_BACK");
            notificationService.trigger(notifyTrigger, String.valueOf(risk.getId()), custodianAndAdmins(risk), vars);
        }
        audit(actor, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "rejectionReason", reason);
    }

    /** Returns go to the Risk Custodian with an Admin copy. The Risk Owner joins only at approval. */
    private List<String> custodianAndAdmins(RiskRecord risk) {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        if (risk.getAssignedContributorId() != null) {
            ids.add(String.valueOf(risk.getAssignedContributorId()));
        }
        ids.addAll(riskAdmins(risk));
        return ids;
    }

    private List<String> riskAdmins(RiskRecord risk) {
        return riskAdminRecipients.forRisk(risk);
    }

    private static boolean sameUser(Long a, Long b) {
        return a != null && a.equals(b);
    }

    private String userName(Long userId) {
        if (userId == null) {
            return "-";
        }
        return userRepository.findById(userId)
                .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName().trim() : u.getEmail())
                .orElse("-");
    }

    /**
     * A rejected or sent back Risk Acceptance. The Risk Custodian is asked to revise. Who gets
     * the FYI and who gets the confirmation depends on whether the Risk Owner or a later approver
     * rejected it. On an Admin send back only the Custodian and the Risk Owner hear about it.
     */
    private void notifyAcceptanceReturned(RiskRecord risk, String reason, User actor, String trigger,
                                          String rejectorRole) {
        boolean adminSendBack = "ADMIN_FINAL_DECISION".equals(trigger);
        Map<String, String> vars = riskMailVars(risk);
        vars.put("rejectorRole", rejectorRole);
        vars.put("reason", reason != null ? reason : "-");
        vars.put("decision", "SENT_BACK");
        vars.put("detail", reason != null ? reason : "-");
        vars.put(AppNotificationMailComposer.RISK_MAIL_KIND,
                adminSendBack ? "ACCEPTANCE_ADMIN_SENT_BACK" : "ACCEPTANCE_REJECTED");
        vars.put("rejectorName", actor != null ? userName(actor.getId()) : "-");
        vars.put("rejectedByOwner", String.valueOf(actor != null && sameUser(actor.getId(), risk.getRiskOwnerUserId())));
        if (risk.getRiskOwnerUserId() != null) {
            vars.put(AppNotificationMailComposer.RISK_OWNER_ID, String.valueOf(risk.getRiskOwnerUserId()));
        }
        java.util.ArrayList<String> recipients = new java.util.ArrayList<>(stakeholderUserIds(risk));
        if (!adminSendBack) {
            recipients.addAll(riskAdmins(risk));
        }
        notificationService.trigger(trigger, String.valueOf(risk.getId()), recipients, vars);
    }

    private static List<String> stakeholderUserIds(RiskRecord risk) {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        if (risk.getAssignedContributorId() != null) {
            ids.add(String.valueOf(risk.getAssignedContributorId()));
        }
        if (risk.getRiskOwnerUserId() != null) {
            ids.add(String.valueOf(risk.getRiskOwnerUserId()));
        }
        return ids;
    }

    private void notifyRiskOwnerNamed(RiskRecord risk, Long previousOwnerId) {
        Long ownerId = risk.getRiskOwnerUserId();
        if (ownerId == null || ownerId.equals(previousOwnerId)) {
            return;
        }
        Map<String, String> vars = riskMailVars(risk);
        String recordId = String.valueOf(risk.getId());
        List<String> to = List.of(String.valueOf(ownerId));
        // The owner's email is the "assigned for treatment" FYI, so it waits for a Risk Custodian.
        if (risk.getAssignedContributorId() != null && !ownerId.equals(risk.getAssignedContributorId())
                && (risk.getStatus() == RiskStatus.ASSIGNED || risk.getStatus() == RiskStatus.RE_OPENED)) {
            notificationService.trigger("RISK_OWNER_NAMED", recordId, to, vars);
        } else {
            notificationService.triggerInApp("RISK_OWNER_NAMED", recordId, to, vars);
        }
    }

    private void notifyRisk(RiskRecord risk, String trigger, Map<String, String> vars) {
        java.util.ArrayList<String> recipients = new java.util.ArrayList<>(stakeholderUserIds(risk));
        recipients.addAll(riskAdmins(risk));
        notificationService.trigger(trigger, String.valueOf(risk.getId()), recipients, vars);
    }

    /** One "Risk Closed" mail to the Admin, the Risk Owner and the Risk Custodian. */
    private void notifyFinalDecision(RiskRecord risk, String decision, String detail) {
        Map<String, String> vars = riskMailVars(risk);
        vars.put("decision", decision);
        vars.put("detail", detail != null ? detail : "");
        if (risk.getClosedAt() != null) {
            vars.put(AppNotificationMailComposer.RISK_MAIL_KIND, "CLOSED");
            vars.put("closureDecision", closureDecisionLabel(risk.getStatus()));
        }
        notifyRisk(risk, "ADMIN_FINAL_DECISION", vars);
    }

    /** Decision words from the Arbor "Risk Closed" mail. */
    private static String closureDecisionLabel(RiskStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case TOLERATED -> "Tolerated";
            case ACCEPTED -> "Risk Accepted";
            case AVOIDED -> "Avoided";
            case TRANSFERRED -> "Transferred";
            case CLOSED, AUTO_CLOSED -> "Treated and closed";
            default -> status.name();
        };
    }

    private void saveAcceptanceEvidenceSnapshot(User admin, RiskRecord risk, RiskAcceptanceForm form) {
        String body = """
                Risk Acceptance Form
                Risk ID: %s
                Title: %s
                Justification: %s
                Other Details: %s
                Conditions: %s
                Compensating controls: %s
                Comp control effectiveness: %s
                Review date: %s
                Risk Owner decision: %s
                Admin decision: %s
                """.formatted(
                risk.getDisplayId(),
                risk.getTitle(),
                form.getJustification(),
                form.getBusinessRationale(),
                form.getConditionsOfAcceptance(),
                form.getCompensatingControls(),
                form.getCompControlEffectiveness(),
                form.getReviewDate(),
                form.getRiskOwnerDecision(),
                form.getAdminDecision());
        byte[] pdf = ("%PDF-1.4\n" + body).getBytes(StandardCharsets.UTF_8);
        evidenceService.createLinkedTextDocument(
                admin,
                "Risk Acceptance: " + risk.getDisplayId(),
                EvidenceClassification.CONFIDENTIAL,
                "risk-acceptance-" + risk.getDisplayId() + ".pdf",
                "application/pdf",
                pdf,
                EVIDENCE_MODULE,
                EVIDENCE_RECORD_TYPE,
                String.valueOf(risk.getId()),
                "TOLERANCE");
    }

    private TreatmentPlan ensurePlan(RiskRecord risk) {
        return treatmentPlanRepository.findByRiskId(risk.getId()).orElseGet(() -> {
            TreatmentPlan p = new TreatmentPlan();
            p.setRiskId(risk.getId());
            p.setStatus(TreatmentPlanStatus.NOT_STARTED);
            if (risk.getTreatmentPlan() != null) {
                p.setActionDescription(risk.getTreatmentPlan());
            }
            return treatmentPlanRepository.save(p);
        });
    }

    private void applyPlanFields(TreatmentPlan plan, String actionDescription, Long actionOwnerId,
                                 Long custodianUserId,
                                 LocalDate startDate, LocalDate targetDate, String resourcesRequired,
                                 java.math.BigDecimal costEstimateAed, String evidenceRequired) {
        if (actionDescription != null) {
            plan.setActionDescription(blankToNull(actionDescription));
        }
        if (actionOwnerId != null) {
            if (actionOwnerId == 0L) {
                plan.setActionOwnerId(null);
            } else {
                if (userRepository.findById(actionOwnerId).isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action owner not found");
                }
                plan.setActionOwnerId(actionOwnerId);
            }
        }
        if (custodianUserId != null) {
            if (custodianUserId == 0L) {
                plan.setCustodianUserId(null);
            } else {
                if (userRepository.findById(custodianUserId).isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custodian not found");
                }
                plan.setCustodianUserId(custodianUserId);
            }
        }
        if (startDate != null) {
            plan.setStartDate(startDate);
        }
        if (targetDate != null) {
            plan.setTargetDate(targetDate);
        }
        DateRules.validatePlanDates(plan.getStartDate(), plan.getTargetDate());
        if (resourcesRequired != null) {
            plan.setResourcesRequired(blankToNull(resourcesRequired));
        }
        if (costEstimateAed != null) {
            plan.setCostEstimateAed(costEstimateAed);
        }
        if (evidenceRequired != null) {
            plan.setEvidenceRequired(blankToNull(evidenceRequired));
        }
    }

    private void syncMilestoneTasks(RiskRecord risk, TreatmentPlan plan, Long assignedByUserId) {
        if (risk.getStatus() != RiskStatus.MITIGATING || plan.getActionOwnerId() == null) {
            return;
        }
        Long ownerId = plan.getActionOwnerId();
        Long assigner = assignedByUserId != null ? assignedByUserId : ownerId;
        List<TreatmentMilestone> milestones = milestoneRepository
                .findByTreatmentPlanIdOrderByDueDateAscIdAsc(plan.getId());
        for (TreatmentMilestone m : milestones) {
            if (m.isCompleted()) {
                markMilestoneTaskSubmitted(m.getId());
                continue;
            }
            WorkTask task = workTaskRepository
                    .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                            MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_MILESTONE, m.getId())
                    .orElseGet(WorkTask::new);
            task.setSourceModule(MyTasksService.MODULE_RISK);
            task.setSourceRecordType(MyTasksService.TYPE_RISK_MILESTONE);
            task.setSourceRecordId(m.getId());
            task.setTitle("Milestone: " + m.getMilestoneTitle());
            task.setDetail(risk.getDisplayId() + " · " + risk.getTitle());
            task.setAssignedToUserId(ownerId);
            task.setAssignedByUserId(assigner);
            task.setDueDate(m.getDueDate());
            if (task.getStatus() == null || task.getStatus() == WorkTaskStatus.CANCELLED) {
                task.setStatus(WorkTaskStatus.PENDING);
            }
            workTaskRepository.save(task);
        }
    }

    private void markMilestoneTaskSubmitted(long milestoneId) {
        workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_MILESTONE, milestoneId)
                .ifPresent(task -> {
                    if (task.getStatus() != WorkTaskStatus.SUBMITTED) {
                        task.setStatus(WorkTaskStatus.SUBMITTED);
                        task.setSubmittedAt(Instant.now());
                        workTaskRepository.save(task);
                    }
                });
    }

    private void assertCanEditMilestones(User user, RiskRecord risk) {
        if (RoleAccess.isElevated(user.getRoles())) {
            return;
        }
        TreatmentPlan plan = treatmentPlanRepository.findByRiskId(risk.getId()).orElse(null);
        if (plan != null && user.getId().equals(plan.getActionOwnerId())) {
            return;
        }
        if (user.getId().equals(risk.getAssignedContributorId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to update milestones");
    }

    private void assertAcceptanceComplete(RiskAcceptanceForm form) {
        requireText(form.getJustification(), "Acceptance justification");
        requireText(form.getConditionsOfAcceptance(), "Conditions of acceptance");
        requireText(form.getCompensatingControls(), "Compensating controls");
        if (form.getCompControlOwnerId() == null || form.getCompControlEffectiveness() == null
                || form.getReviewDate() == null || !form.isContributorDeclaration()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acceptance form incomplete");
        }
    }

    private void requireRevisionIfReturned(RiskRecord risk, SubmitRiskRequest req) {
        if (risk.getStatus() != RiskStatus.RE_OPENED) {
            return;
        }
        String previous = treatmentPlanRepository.findByRiskId(risk.getId())
                .map(TreatmentPlan::getActionDescription)
                .filter(s -> s != null && !s.isBlank())
                .orElse(risk.getTreatmentPlan());
        String incoming = req.actionDescription() != null ? req.actionDescription() : previous;
        boolean detailsChanged = !normPlanText(incoming).equals(normPlanText(previous));
        boolean newEvidence = evidenceService.hasNonIdentificationLinkOnOrAfter(
                EVIDENCE_MODULE, EVIDENCE_RECORD_TYPE, String.valueOf(risk.getId()), risk.getUpdatedAt());
        if (!detailsChanged && !newEvidence) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Update Treatment Action Plan Detail or add evidence before submitting this returned risk");
        }
    }

    private static String normPlanText(String value) {
        return value == null ? "" : value.trim();
    }

    private void requirePlanEvidenceIfSpecified(RiskRecord risk, String path) {
        TreatmentPlan plan = treatmentPlanRepository.findByRiskId(risk.getId()).orElse(null);
        if (plan != null && plan.getEvidenceRequired() != null && !plan.getEvidenceRequired().isBlank()) {
            requireEvidence(risk, path);
        }
    }

    private void requireEvidence(RiskRecord risk, String path) {
        int n = evidenceService.countLinksForRecord(
                EVIDENCE_MODULE, EVIDENCE_RECORD_TYPE, String.valueOf(risk.getId()));
        if (n < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    path + " path requires at least one linked evidence file");
        }
    }

    private void requireMilestoneEvidence(long milestoneId) {
        int n = evidenceService.countLinksForRecord(
                EVIDENCE_MODULE, EVIDENCE_MILESTONE_RECORD_TYPE, String.valueOf(milestoneId));
        if (n < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Milestone completion requires at least one linked evidence file");
        }
    }

    private void notifyAdmins(RiskRecord risk, String trigger, Map<String, String> vars) {
        notificationService.trigger(trigger, String.valueOf(risk.getId()), riskAdmins(risk), vars);
    }

    /**
     * All approvers said yes. Admins get the action mail and the bell. The Risk Custodian and the
     * Risk Owner get one "approved" update mail each, unless they are also an Admin of the risk.
     */
    private void notifyPendingAdmin(RiskRecord risk) {
        Map<String, String> vars = riskMailVars(risk);
        String recordId = String.valueOf(risk.getId());
        List<String> admins = riskAdmins(risk);
        List<String> updates = stakeholderUserIds(risk).stream().filter(id -> !admins.contains(id)).toList();
        vars.put(AppNotificationMailComposer.RISK_UPDATE_IDS, String.join(",", updates));
        notificationService.triggerInApp("RISK_PENDING_ADMIN", recordId, admins, vars);
        java.util.LinkedHashSet<String> mailTo = new java.util.LinkedHashSet<>(admins);
        mailTo.addAll(updates);
        notificationService.triggerEmail("RISK_PENDING_ADMIN", recordId, new java.util.ArrayList<>(mailTo), vars);
    }

    private RiskRecordResponse toResponse(RiskRecord risk) {
        TreatmentPlanResponse planResp = treatmentPlanRepository.findByRiskId(risk.getId())
                .map(p -> TreatmentPlanResponse.from(p,
                        milestoneRepository.findByTreatmentPlanIdOrderByDueDateAscIdAsc(p.getId())))
                .orElse(null);
        RiskAcceptanceFormResponse acc = acceptanceFormRepository.findByRiskId(risk.getId())
                .map(this::toAcceptanceResponse)
                .orElse(null);
        List<RiskFrameworkMappingResponse> mappings = frameworkMappingRepository
                .findByRiskIdOrderByIdAsc(risk.getId()).stream()
                .map(RiskFrameworkMappingResponse::from)
                .toList();
        Integer inherentScore = null;
        String inherentRating = null;
        String inherentAction = null;
        Integer residualScore = null;
        String residualRating = null;
        String residualAction = null;
        ResidualScoringMode mode = risk.getResidualScoringMode() != null
                ? risk.getResidualScoringMode()
                : ResidualScoringMode.MANUAL_PROBABILITY_IMPACT;
        try {
            if (risk.getRiskProjectId() != null && risk.getInherentScore() != null) {
                MatrixGridSize gs = riskMatrixService.currentGridSize();
                inherentScore = risk.getInherentScore();
                inherentRating = ExcelRiskScoring.rating(gs, inherentScore);
                inherentAction = ExcelRiskScoring.action(gs, inherentScore);
                if (risk.getResidualScore() != null) {
                    residualScore = risk.getResidualScore();
                    residualRating = ExcelRiskScoring.rating(gs, residualScore);
                    residualAction = ExcelRiskScoring.action(gs, residualScore);
                }
            } else if (risk.getProbability() != null && risk.getImpact() != null) {
                inherentScore = riskMatrixService.score(risk.getProbability(), risk.getImpact());
                inherentRating = riskMatrixService.rating(risk.getProbability(), risk.getImpact());
                inherentAction = riskMatrixService.action(risk.getProbability(), risk.getImpact());
            }
            if (risk.getRiskProjectId() != null && residualScore != null) {
                // already filled from stored Excel scores
            } else if (mode == ResidualScoringMode.CONTROL_EFFECTIVENESS) {
                residualAction = "Control effectiveness × Inherent (auto)";
            } else {
                RiskLevel rp = risk.getResidualProbability() != null
                        ? risk.getResidualProbability() : risk.getProbability();
                RiskLevel ri = risk.getResidualImpact() != null
                        ? risk.getResidualImpact() : risk.getImpact();
                if (rp != null && ri != null) {
                    residualScore = riskMatrixService.score(rp, ri);
                    residualRating = riskMatrixService.rating(rp, ri);
                    residualAction = riskMatrixService.action(rp, ri);
                }
            }
        } catch (Exception ignored) {
            // Levels outside active axes (frozen legacy): leave Excel fields null.
        }
        return RiskRecordResponse.from(
                risk, planResp, acc, mappings,
                inherentScore, inherentRating, inherentAction,
                residualScore, residualRating, residualAction,
                resolveOrgUnitName(risk.getOrgUnitId()),
                resolveUserName(risk.getRiskOwnerUserId()),
                resolveUserName(risk.getAssignedContributorId()));
    }

    private RiskAcceptanceFormResponse toAcceptanceResponse(RiskAcceptanceForm form) {
        return RiskAcceptanceFormResponse.from(
                form,
                resolveUserName(form.getApprover1UserId()),
                resolveUserName(form.getApprover2UserId()),
                resolveUserName(form.getApprover3UserId()));
    }

    private String resolveOrgUnitName(Long orgUnitId) {
        if (orgUnitId == null) {
            return null;
        }
        return orgUnitRepository.findById(orgUnitId).map(u -> u.getName()).orElse(null);
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(User::getFullName).orElse(null);
    }

    private boolean isExcelAutoClosable(RiskRecord risk) {
        if (risk.getRiskProjectId() != null && risk.getResidualScore() != null) {
            return ExcelRiskScoring.autoClosable(riskMatrixService.currentGridSize(), risk.getResidualScore());
        }
        ResidualScoringMode mode = risk.getResidualScoringMode() != null
                ? risk.getResidualScoringMode()
                : ResidualScoringMode.MANUAL_PROBABILITY_IMPACT;
        if (mode == ResidualScoringMode.CONTROL_EFFECTIVENESS) {
            return RiskMatrix.autoClosable(risk.getResidualRisk());
        }
        RiskLevel rp = risk.getResidualProbability() != null
                ? risk.getResidualProbability() : risk.getProbability();
        RiskLevel ri = risk.getResidualImpact() != null
                ? risk.getResidualImpact() : risk.getImpact();
        if (rp == null || ri == null) {
            return RiskMatrix.autoClosable(risk.getResidualRisk());
        }
        try {
            return riskMatrixService.autoClosableScore(rp, ri);
        } catch (Exception e) {
            return RiskMatrix.autoClosable(risk.getResidualRisk());
        }
    }

    private void requireModuleForType(RiskType type) {
        ModuleCode code = RiskModuleCodes.forType(type);
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank() || !moduleLicensingService.isEnabled(tenantId, code)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module " + code.name() + " is not licensed for this tenant");
        }
        adminModuleAccessService.assertMayUse(code, false);
    }

    private void requireWriteForType(RiskType type) {
        requireModuleForType(type);
        ModuleCode code = RiskModuleCodes.forType(type);
        adminModuleAccessService.assertMayUse(code, true);
        moduleLicensingService.requireWritable(TenantContext.getTenantId(), code);
    }

    /** Public gate for bulk import / template download. */
    public void assertLicensed(RiskType type) {
        requireWriteForType(type);
    }

    private void applyInitialStatus(RiskRecord risk) {
        if (risk.isAutoCloseOverride()) {
            risk.setStatus(RiskStatus.DRAFT);
            risk.setClosedAt(null);
        } else if (isExcelAutoClosable(risk)) {
            risk.setStatus(RiskStatus.AUTO_CLOSED);
            risk.setClosedAt(Instant.now());
        } else {
            risk.setStatus(RiskStatus.DRAFT);
        }
    }

    private void applyDisplayId(RiskRecord risk, RiskType type, String override, User user) {
        if (override == null || override.isBlank()) {
            risk.setDisplayId(idGenerator.nextDisplayId(type));
            risk.setDisplayIdOverridden(false);
            return;
        }
        String id = override.trim().toUpperCase();
        if (id.length() > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk ID must be at most 32 characters");
        }
        List<String> clashes = riskRepository.findDisplayIdsByKey(RiskIdGenerator.idKey(id));
        if (!clashes.isEmpty()) {
            String existing = clashes.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT, existing.equalsIgnoreCase(id)
                    ? "Risk ID already in use: " + id
                    : "Risk ID " + id + " matches existing Risk ID " + existing);
        }
        risk.setDisplayId(id);
        risk.setDisplayIdOverridden(true);
    }

    private void resolveUaeIaPriority(RiskRecord risk, UaeIaPriority manual) {
        if (risk.getRiskType() != RiskType.IS_RISK) {
            risk.setUaeIaPriority(null);
            return;
        }
        if (manual != null) {
            risk.setUaeIaPriority(manual);
            return;
        }
        if (risk.getFrameworkId() == null || risk.getFrameworkNodeId() == null) {
            risk.setUaeIaPriority(null);
            return;
        }
        var node = frameworkQueryService.nodesById(risk.getFrameworkId()).get(risk.getFrameworkNodeId());
        risk.setUaeIaPriority(node != null ? UaeIaPriority.fromNodePriority(node.priority()) : null);
    }

    private static void applyAutoCloseOverrideFlags(Boolean override, String justification) {
        if (!Boolean.TRUE.equals(override)) {
            return;
        }
        if (justification == null || justification.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Justification is required to override auto-closure");
        }
    }

    private void notifyAutoClosed(RiskRecord risk) {
        java.util.ArrayList<String> recipients = new java.util.ArrayList<>(riskAdmins(risk));
        if (risk.getRiskOwnerUserId() != null) {
            recipients.add(String.valueOf(risk.getRiskOwnerUserId()));
        }
        notificationService.trigger(
                "RISK_AUTO_CLOSED",
                String.valueOf(risk.getId()),
                recipients,
                Map.of(
                        "displayId", risk.getDisplayId(),
                        "title", risk.getTitle(),
                        "residualRisk", risk.getResidualRisk().name()));
    }

    private void replaceAdditionalFrameworks(long riskId, List<RiskFrameworkMappingRequest> mappings) {
        frameworkMappingRepository.deleteByRiskId(riskId);
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        for (RiskFrameworkMappingRequest req : mappings) {
            RiskFrameworkMapping m = new RiskFrameworkMapping();
            m.setRiskId(riskId);
            m.setFrameworkId(req.frameworkId());
            m.setFrameworkCode(blankToNull(req.frameworkCode()));
            m.setFrameworkNodeId(req.frameworkNodeId());
            m.setControlRef(blankToNull(req.controlRef()));
            m.setCustomRef(blankToNull(req.customRef()));
            frameworkMappingRepository.save(m);
        }
    }

    private void applyTreatmentPlanDraft(RiskRecord risk, TreatmentPlanDraftRequest detail, String planText) {
        if (detail == null && (planText == null || planText.isBlank())) {
            return;
        }
        TreatmentPlan plan = ensurePlan(risk);
        if (detail != null) {
            applyPlanFields(plan, detail.actionDescription(), detail.actionOwnerId(), detail.custodianUserId(),
                    detail.startDate(),
                    detail.targetDate(), detail.resourcesRequired(), detail.costEstimateAed(),
                    detail.evidenceRequired());
            if (detail.actionDescription() != null && !detail.actionDescription().isBlank()) {
                risk.setTreatmentPlan(detail.actionDescription().trim());
            }
        } else if (planText != null && !planText.isBlank()) {
            plan.setActionDescription(planText.trim());
            risk.setTreatmentPlan(planText.trim());
        }
        treatmentPlanRepository.save(plan);
        riskRepository.save(risk);
        syncCustodianTaskDueDate(risk, plan);
    }

    /** Register risks: the Risk Custodian task is due on the Admin's Target Date. */
    private void syncCustodianTaskDueDate(RiskRecord risk, TreatmentPlan plan) {
        if (risk.getRiskProjectId() == null || risk.getWorkTaskId() == null || plan.getTargetDate() == null) {
            return;
        }
        workTaskRepository.findById(risk.getWorkTaskId()).ifPresent(task -> {
            if (!plan.getTargetDate().equals(task.getDueDate())) {
                task.setDueDate(plan.getTargetDate());
                workTaskRepository.save(task);
            }
        });
    }

    private LocalDate riskTargetDate(RiskRecord risk) {
        return treatmentPlanRepository.findByRiskId(risk.getId()).map(TreatmentPlan::getTargetDate).orElse(null);
    }

    private LocalDate riskDueDate(RiskRecord risk) {
        LocalDate target = riskTargetDate(risk);
        if (target != null || risk.getWorkTaskId() == null) {
            return target;
        }
        return workTaskRepository.findById(risk.getWorkTaskId()).map(WorkTask::getDueDate).orElse(null);
    }

    /** Vars shared by the risk workflow emails: title, due date and the Risk Custodian. */
    private Map<String, String> riskMailVars(RiskRecord risk) {
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("displayId", risk.getDisplayId());
        vars.put("title", risk.getTitle() != null ? risk.getTitle() : "");
        LocalDate due = riskDueDate(risk);
        vars.put("dueDate", due != null ? due.toString() : "-");
        Long custodianId = risk.getAssignedContributorId();
        if (custodianId != null) {
            vars.put(AppNotificationMailComposer.RISK_CUSTODIAN_ID, String.valueOf(custodianId));
            vars.put(AppNotificationMailComposer.RISK_CUSTODIAN_NAME, userRepository.findById(custodianId)
                    .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName().trim() : u.getEmail())
                    .orElse("-"));
        }
        return vars;
    }

    @Transactional(readOnly = true)
    public List<RiskScoreHistoryResponse> listScoreHistory(User user, long id) {
        RiskRecord risk = requireVisible(user, id);
        requireModuleForType(risk.getRiskType());
        return scoreHistoryRepository.findByRiskIdOrderByRecordedAtAscIdAsc(risk.getId()).stream()
                .map(RiskScoreHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RiskStageNoteResponse> listNotes(User user, long id) {
        RiskRecord risk = requireVisible(user, id);
        requireModuleForType(risk.getRiskType());
        boolean seeInternal = canSeeInternalNotes(user);
        return stageNoteRepository.findByRiskIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(risk.getId())
                .stream()
                .filter(n -> seeInternal || !n.isInternalOnly())
                .map(RiskStageNoteResponse::from)
                .toList();
    }

    @Transactional
    public RiskStageNoteResponse addNote(User user, long id, CreateRiskStageNoteRequest req) {
        RiskRecord risk = requireVisible(user, id);
        requireWriteForType(risk.getRiskType());
        if (RoleAccess.isContributorOnly(user) && !user.getId().equals(risk.getAssignedContributorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can view this risk. Approval opens when Risk Acceptance starts.");
        }
        boolean adminOrOwner = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.OWNER);
        boolean internal = adminOrOwner && Boolean.TRUE.equals(req.internalOnly());
        RiskStageNote note = new RiskStageNote();
        note.setRiskId(risk.getId());
        note.setWorkflowStage(risk.getStatus());
        note.setBody(req.body().trim());
        note.setInternalOnly(internal);
        note.setCreatedByUserId(user.getId());
        stageNoteRepository.save(note);
        audit(user, AuditActionType.CREATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "stageNote", internal ? "INTERNAL" : "SHARED");
        return RiskStageNoteResponse.from(note);
    }

    @Transactional
    public void deleteNote(User user, long id, long noteId) {
        RiskRecord risk = requireVisible(user, id);
        requireWriteForType(risk.getRiskType());
        RiskStageNote note = stageNoteRepository.findByIdAndRiskIdAndDeletedAtIsNull(noteId, risk.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
        boolean adminOrOwner = user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.OWNER);
        if (!adminOrOwner && !user.getId().equals(note.getCreatedByUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator or Admin may delete this note");
        }
        note.setDeletedAt(Instant.now());
        note.setDeletedByUserId(user.getId());
        stageNoteRepository.save(note);
        audit(user, AuditActionType.DELETED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "stageNote", String.valueOf(noteId));
    }

    private static boolean canSeeInternalNotes(User user) {
        return user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.OWNER);
    }

    private void recordScoreHistory(RiskRecord risk, User user, String reason) {
        RiskScoreHistory h = new RiskScoreHistory();
        h.setRiskId(risk.getId());
        h.setProbability(risk.getProbability());
        h.setImpact(risk.getImpact());
        h.setControlEffectiveness(risk.getControlEffectiveness());
        h.setInherentRisk(risk.getInherentRisk());
        h.setResidualRisk(risk.getResidualRisk());
        h.setReason(blankToNull(reason));
        h.setChangedByUserId(user.getId());
        scoreHistoryRepository.save(h);
    }

    private void recordScoreHistoryIfChanged(RiskRecord risk, User user, ScoreSnapshot before, String reason) {
        if (before != null && before.matches(risk)) {
            return;
        }
        recordScoreHistory(risk, user, reason);
    }

    private record ScoreSnapshot(
            RiskLevel probability,
            RiskLevel impact,
            RiskLevel controlEffectiveness,
            ResidualScoringMode residualScoringMode,
            RiskLevel residualProbability,
            RiskLevel residualImpact,
            RiskLevel inherentRisk,
            RiskLevel residualRisk
    ) {
        static ScoreSnapshot of(RiskRecord risk) {
            return new ScoreSnapshot(
                    risk.getProbability(),
                    risk.getImpact(),
                    risk.getControlEffectiveness(),
                    risk.getResidualScoringMode(),
                    risk.getResidualProbability(),
                    risk.getResidualImpact(),
                    risk.getInherentRisk(),
                    risk.getResidualRisk());
        }

        boolean matches(RiskRecord risk) {
            return probability == risk.getProbability()
                    && impact == risk.getImpact()
                    && controlEffectiveness == risk.getControlEffectiveness()
                    && residualScoringMode == risk.getResidualScoringMode()
                    && residualProbability == risk.getResidualProbability()
                    && residualImpact == risk.getResidualImpact()
                    && inherentRisk == risk.getInherentRisk()
                    && residualRisk == risk.getResidualRisk();
        }
    }

    private void applyResidualScoringFields(RiskRecord risk,
                                            ResidualScoringMode mode,
                                            RiskLevel residualProbability,
                                            RiskLevel residualImpact,
                                            boolean creating) {
        ResidualScoringMode resolved = mode != null
                ? mode
                : (risk.getResidualScoringMode() != null
                        ? risk.getResidualScoringMode()
                        : ResidualScoringMode.MANUAL_PROBABILITY_IMPACT);
        if (creating && mode == null) {
            resolved = risk.getRiskProjectId() != null
                    ? ResidualScoringMode.CONTROL_EFFECTIVENESS
                    : ResidualScoringMode.MANUAL_PROBABILITY_IMPACT;
        }
        risk.setResidualScoringMode(resolved);
        if (risk.getRiskProjectId() != null && resolved == ResidualScoringMode.MANUAL_PROBABILITY_IMPACT) {
            return;
        }
        if (resolved == ResidualScoringMode.MANUAL_PROBABILITY_IMPACT) {
            RiskLevel rp = residualProbability != null ? residualProbability : risk.getResidualProbability();
            RiskLevel ri = residualImpact != null ? residualImpact : risk.getResidualImpact();
            if (creating) {
                if (rp == null) {
                    rp = risk.getProbability();
                }
                if (ri == null) {
                    ri = risk.getImpact();
                }
            }
            if (residualProbability != null) {
                risk.setResidualProbability(residualProbability);
            } else if (rp != null) {
                risk.setResidualProbability(rp);
            }
            if (residualImpact != null) {
                risk.setResidualImpact(residualImpact);
            } else if (ri != null) {
                risk.setResidualImpact(ri);
            }
            if (risk.getResidualProbability() == null || risk.getResidualImpact() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Residual probability and impact are required (Excel P×I residual scoring)");
            }
        } else if (resolved == ResidualScoringMode.CONTROL_EFFECTIVENESS) {
            risk.setResidualProbability(null);
            risk.setResidualImpact(null);
        } else {
            if (residualProbability != null) {
                risk.setResidualProbability(residualProbability);
            }
            if (residualImpact != null) {
                risk.setResidualImpact(residualImpact);
            }
        }
    }

    private void recomputeScores(RiskRecord risk) {
        MatrixAxes axes = riskMatrixService.currentAxes();
        if (!axes.isActive(risk.getProbability()) || !axes.isActive(risk.getImpact())) {
            return;
        }
        MatrixGridSize size = risk.getRiskProjectId() != null
                ? riskMatrixService.currentGridSize()
                : riskMatrixService.currentGridSize();
        if (risk.getRiskProjectId() != null) {
            recomputeRegisterScores(risk, axes, size);
            return;
        }
        ResidualScoringMode mode = risk.getResidualScoringMode() != null
                ? risk.getResidualScoringMode()
                : ResidualScoringMode.MANUAL_PROBABILITY_IMPACT;
        if (mode == ResidualScoringMode.CONTROL_EFFECTIVENESS
                && !axes.isActive(risk.getControlEffectiveness())) {
            return;
        }
        if (mode == ResidualScoringMode.MANUAL_PROBABILITY_IMPACT) {
            if (risk.getResidualProbability() == null
                    || risk.getResidualImpact() == null
                    || !axes.isActive(risk.getResidualProbability())
                    || !axes.isActive(risk.getResidualImpact())) {
                return;
            }
        }
        RiskLevel inherent = riskMatrixService.inherent(risk.getProbability(), risk.getImpact());
        RiskLevel residual;
        if (mode == ResidualScoringMode.MANUAL_PROBABILITY_IMPACT) {
            residual = riskMatrixService.residualProbabilityImpact(
                    risk.getResidualProbability(), risk.getResidualImpact());
        } else {
            residual = RiskMatrix.residualFromControlEffectiveness(
                    risk.getControlEffectiveness(), inherent);
        }
        risk.setInherentRisk(inherent);
        risk.setResidualRisk(residual);
        risk.setMatrixGridSize(size.size());
    }

    private void recomputeRegisterScores(RiskRecord risk, MatrixAxes axes, MatrixGridSize size) {
        int inherentScore = ExcelRiskScoring.score(axes, risk.getProbability(), risk.getImpact());
        double reduction = 0;
        ResidualScoringMode mode = risk.getResidualScoringMode() != null
                ? risk.getResidualScoringMode()
                : ResidualScoringMode.CONTROL_EFFECTIVENESS;
        int residualScore;
        if (mode == ResidualScoringMode.CONTROL_EFFECTIVENESS) {
            if (!axes.isActive(risk.getControlEffectiveness())) {
                return;
            }
            reduction = ExcelRiskScoring.reductionPercent(size, axes, risk.getControlEffectiveness());
            residualScore = ExcelRiskScoring.residualScore(inherentScore, reduction);
        } else if (risk.getResidualScore() != null) {
            residualScore = Math.max(1, risk.getResidualScore());
        } else {
            reduction = ExcelRiskScoring.reductionPercent(size, axes, risk.getControlEffectiveness());
            residualScore = ExcelRiskScoring.residualScore(inherentScore, reduction);
        }
        RiskLevel appetite = riskMatrixService.currentAppetite();
        risk.setInherentScore(inherentScore);
        risk.setResidualScore(residualScore);
        risk.setRiskReductionPct(java.math.BigDecimal.valueOf(reduction));
        risk.setInherentRisk(ExcelRiskScoring.workflowLevel(size, inherentScore));
        risk.setResidualRisk(ExcelRiskScoring.workflowLevel(size, residualScore));
        risk.setWithinAppetite(residualScore <= ExcelRiskScoring.appetiteMaxResidual(size, appetite));
        risk.setEvaluationDecision(ExcelRiskScoring.evaluation(size, residualScore, appetite));
        if (risk.getTargetControlEffectiveness() != null && axes.isActive(risk.getTargetControlEffectiveness())) {
            int target = ExcelRiskScoring.residualScore(
                    size, axes, inherentScore, risk.getTargetControlEffectiveness());
            risk.setTargetResidualScore(target);
            risk.setTargetResidualManual(false);
            if (risk.getTargetResidualScore() != null) {
                risk.setTargetWithinAppetite(
                        risk.getTargetResidualScore() <= ExcelRiskScoring.appetiteMaxResidual(size, appetite));
            }
        }
        risk.setAcceptanceRequired(ExcelRiskScoring.acceptanceSheetRequired(
                risk.getTreatmentOption(), risk.getTargetResidualScore(), size, appetite));
        risk.setMatrixGridSize(size.size());
    }

    private void validateRefs(Long orgUnitId, Long assetId, Long riskOwnerUserId) {
        if (orgUnitId != null && orgUnitRepository.findById(orgUnitId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org unit not found");
        }
        if (assetId != null) {
            var asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset not found"));
            if (asset.getStatus() == AssetStatus.DISPOSED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asset is disposed");
            }
        }
        if (riskOwnerUserId != null && userRepository.findById(riskOwnerUserId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk owner not found");
        }
    }

    private static void validateCategory(RiskCategory category, String categoryOther) {
        if (category == RiskCategory.OTHER && (categoryOther == null || categoryOther.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Specify the category when Other is selected");
        }
    }

    private RiskRecord require(long id) {
        return riskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));
    }

    private RiskRecord requireVisible(User user, long id) {
        RiskRecord risk = require(id);
        if (RoleAccess.isContributorOnly(user)) {
            boolean isAssignee = user.getId().equals(risk.getAssignedContributorId());
            boolean isOwner = user.getId().equals(risk.getRiskOwnerUserId());
            boolean isApprover = acceptanceFormRepository.findByRiskId(risk.getId())
                    .map(f -> user.getId().equals(f.getApprover1UserId())
                            || user.getId().equals(f.getApprover2UserId())
                            || user.getId().equals(f.getApprover3UserId()))
                    .orElse(false);
            if (!isAssignee && !isOwner && !isApprover) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Risk not assigned to you");
            }
        }
        return risk;
    }

    private void requireUniqueTitle(Long projectId, String title, Long excludeId) {
        if (projectId == null || title == null || title.isBlank()) {
            return;
        }
        String name = title.trim();
        boolean taken = excludeId == null
                ? riskRepository.existsByRiskProjectIdAndTitleIgnoreCase(projectId, name)
                : riskRepository.existsByRiskProjectIdAndTitleIgnoreCaseAndIdNot(projectId, name, excludeId);
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A risk titled \"" + name + "\" already exists in this project");
        }
    }

    private static void assertEditable(RiskRecord risk) {
        if (risk.getStatus() != RiskStatus.DRAFT && risk.getStatus() != RiskStatus.RE_OPENED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Risk can only be edited in DRAFT or RE_OPENED status");
        }
    }

    private static void applyFields(RiskRecord risk, String title, String description,
                                    com.a9.arbor.grc.modules.risk.domain.RiskCategory category,
                                    String categoryOther,
                                    Long orgUnitId, Long assetId, Long frameworkId, String frameworkCode,
                                    Long frameworkNodeId, String nodeCode, String observedGap, String threats,
                                    String existingControls, Long riskOwnerUserId, String adminNotes,
                                    String strategicObjective, java.math.BigDecimal financialImpactAed,
                                    Long tprmVendorId) {
        risk.setTitle(title.trim());
        risk.setDescription(blankToNull(description));
        risk.setCategory(category);
        if (category == RiskCategory.OTHER) {
            risk.setCategoryOther(blankToNull(categoryOther));
        } else {
            risk.setCategoryOther(null);
        }
        risk.setOrgUnitId(orgUnitId);
        risk.setAssetId(assetId);
        risk.setFrameworkId(frameworkId);
        risk.setFrameworkCode(blankToNull(frameworkCode));
        risk.setFrameworkNodeId(frameworkNodeId);
        risk.setNodeCode(blankToNull(nodeCode));
        risk.setObservedGap(blankToNull(observedGap));
        risk.setThreats(blankToNull(threats));
        risk.setExistingControls(blankToNull(existingControls));
        risk.setRiskOwnerUserId(riskOwnerUserId);
        risk.setAdminNotes(blankToNull(adminNotes));
        risk.setStrategicObjective(blankToNull(strategicObjective));
        risk.setFinancialImpactAed(financialImpactAed);
        risk.setTprmVendorId(tprmVendorId);
    }

    private static String mergeComments(String plan, String comments) {
        if (plan == null || plan.isBlank()) {
            return comments;
        }
        return plan + "\n\nContributor: " + comments;
    }

    private static void requireFilledIdentification(RiskRecord risk) {
        requireText(risk.getDescription(), "Risk Description");
        requireText(risk.getObservedGap(), "Gap / Observation");
        if (risk.getRiskType() == RiskType.IS_RISK || risk.getRiskType() == RiskType.PRIVACY_RISK) {
            requireText(risk.getThreats(), "Associated Threat");
        } else {
            if (risk.getCategory() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk Category is required");
            }
            requireText(risk.getCause(), "Risk Cause / Source");
            if (risk.getStrategicObjective() == null || risk.getStrategicObjective().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Strategic objective is required for ERM risks");
            }
            if ("other".equalsIgnoreCase(risk.getCause().trim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Specify the cause when Other is selected");
            }
        }
        requireText(risk.getTreatmentChangeJustification(), "Treatment Action Plan");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
    }

    private void requireProductScoreInRange(Integer score, String field, int maxInclusive) {
        ExcelRiskScoring.requireProductScore(score, riskMatrixService.currentGridSize(), maxInclusive, field);
    }

    private int inherentProduct(RiskLevel probability, RiskLevel impact) {
        return riskMatrixService.score(probability, impact);
    }

    private int storedInherent(RiskRecord risk) {
        if (risk.getInherentScore() != null && risk.getInherentScore() > 0) {
            return risk.getInherentScore();
        }
        return inherentProduct(risk.getProbability(), risk.getImpact());
    }

    private void audit(User user, AuditActionType action, String recordId, String recordName,
                       String field, String newVal) {
        auditTrailService.record(
                action, "RISK", user.getId(), primaryRole(user),
                "RISK_RECORD", recordId, recordName, field, null, newVal, null, null);
    }

    private static String primaryRole(User user) {
        return user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    @Transactional
    public RiskRecordResponse postTreatment(User admin, long id, PostTreatmentRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.SUBMITTED && risk.getStatus() != RiskStatus.UNDER_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Post-treatment scoring is available after Custodian submit");
        }
        requireProductScoreInRange(
                req.residualScore(),
                "Residual Risk Score",
                storedInherent(risk));
        if (risk.getRiskType() == RiskType.IS_RISK && Boolean.TRUE.equals(req.targetResidualManual())) {
            requireProductScoreInRange(
                    req.targetResidualScore(),
                    "Treated Target Residual",
                    storedInherent(risk));
        }
        ScoreSnapshot before = ScoreSnapshot.of(risk);
        if (req.controlEffectiveness() != null) {
            risk.setControlEffectiveness(req.controlEffectiveness());
        }
        if (req.residualScoringMode() != null) {
            applyResidualScoringFields(risk, req.residualScoringMode(), null, null, false);
        }
        if (req.residualScore() != null) {
            risk.setResidualScore(req.residualScore());
            if (risk.getResidualScoringMode() == ResidualScoringMode.CONTROL_EFFECTIVENESS) {
                risk.setResidualScoringMode(ResidualScoringMode.MANUAL_PROBABILITY_IMPACT);
            }
        }
        if (req.targetControlEffectiveness() != null) {
            risk.setTargetControlEffectiveness(req.targetControlEffectiveness());
        }
        if (risk.getRiskType() == RiskType.IS_RISK && Boolean.TRUE.equals(req.targetResidualManual())
                && req.targetResidualScore() != null) {
            risk.setTargetResidualManual(true);
            risk.setTargetResidualScore(req.targetResidualScore());
        } else if (req.targetResidualManual() != null && !req.targetResidualManual()) {
            risk.setTargetResidualManual(false);
        }
        if (Boolean.TRUE.equals(req.overrideAutoClose())) {
            applyAutoCloseOverrideFlags(true, req.autoCloseOverrideJustification());
            risk.setAutoCloseOverride(true);
            risk.setAutoCloseOverrideJustification(req.autoCloseOverrideJustification().trim());
        }
        recomputeScores(risk);
        risk.setPostTreatmentAt(Instant.now());
        risk.setPostTreatmentByUserId(admin.getId());
        if (req.nextReviewDate() != null) {
            risk.setNextReviewDate(req.nextReviewDate());
        }
        if (risk.getStatus() == RiskStatus.SUBMITTED) {
            risk.setStatus(RiskStatus.UNDER_VALIDATION);
            risk.setValidatedAt(Instant.now());
            risk.setValidatedByUserId(admin.getId());
        }
        riskRepository.save(risk);
        recordScoreHistoryIfChanged(risk, admin, before, "Post-treatment scoring");
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "postTreatment", risk.getStatus().name());
        return toResponse(risk);
    }

    @Transactional
    public RiskRecordResponse decideFinalStatus(User admin, long id, FinalRiskStatusRequest req) {
        RiskRecord risk = require(id);
        requireWriteForType(risk.getRiskType());
        if (risk.getStatus() != RiskStatus.SUBMITTED && risk.getStatus() != RiskStatus.UNDER_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Final Risk Status is set after Contributor submit");
        }
        recomputeScores(risk);
        if (req.decision() == FinalRiskStatus.REJECT) {
            boolean startAcceptance = Boolean.TRUE.equals(req.startAcceptance()) && risk.isAcceptanceRequired();
            if (!startAcceptance) {
                if (req.reason() == null || req.reason().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send-back reason is required");
                }
                risk.setFinalRiskStatus(null);
                cancelAcceptanceTasks(risk);
                returnSendToContributor(risk, req.reason().trim(), admin, "RISK_RETURNED");
                return toResponse(risk);
            }
            if (risk.isAcceptanceRequired()) {
                if (req.reason() == null || req.reason().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Remarks are required");
                }
                risk.setFinalRiskStatus(null);
                risk.setRejectionReason("Acceptance required");
                risk.setStatus(RiskStatus.PENDING_ACCEPTANCE);
                risk.setSubmittedAt(null);
                risk.setValidatedAt(null);
                riskRepository.save(risk);
                if (risk.getWorkTaskId() != null) {
                    workTaskRepository.findById(risk.getWorkTaskId()).ifPresent(task -> {
                        task.setStatus(WorkTaskStatus.PENDING);
                        task.setSubmittedAt(null);
                        workTaskRepository.save(task);
                    });
                }
                String remark = req.reason() != null ? req.reason().trim() : "";
                if (!remark.isEmpty() && !remark.equalsIgnoreCase("Acceptance required")) {
                    saveDecisionRemark(risk, admin, remark);
                }
                Map<String, String> vars = riskMailVars(risk);
                vars.put("reason", risk.getRejectionReason() != null ? risk.getRejectionReason() : "-");
                vars.put("decision", "REJECT");
                vars.put("detail", "Acceptance journey");
                vars.put(AppNotificationMailComposer.RISK_MAIL_KIND, "ACCEPTANCE");
                notificationService.trigger("RISK_RETURNED", String.valueOf(risk.getId()),
                        custodianAndAdmins(risk), vars);
                audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                        "finalRiskStatus", "REJECT");
                return toResponse(risk);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send-back reason is required");
        }
        if (risk.isAcceptanceRequired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Acceptance Required is YES. Reject to start the acceptance journey");
        }
        TreatmentOption option = risk.getTreatmentOption() != null ? risk.getTreatmentOption() : TreatmentOption.TREAT;
        if (option == TreatmentOption.TREAT && risk.getTargetControlEffectiveness() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Set Target Control Effectiveness before Approve");
        }
        if (option == TreatmentOption.TOLERATE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tolerate always requires the acceptance journey");
        }
        if (option == TreatmentOption.TERMINATE) {
            requireTerminateReady(risk);
        }
        if (option == TreatmentOption.TRANSFER) {
            requireTransferReady(risk);
        }
        boolean project = risk.getRiskProjectId() != null;
        if (project) {
            if (req.actionStatus() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Action (Risk Status) is required to Approve");
            }
            if (!ADMIN_FINISH_ACTIONS.contains(req.actionStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Action must be Risk Accepted, Risk Treated & Close, Risk Avoided, or Risk Transferred");
            }
            if (req.nextReviewDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Next Review Date is required to Approve");
            }
            DateRules.requireActionDueDateIfPresent(req.nextReviewDate(), "Next Review Date");
        }
        if (req.reason() == null || req.reason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Remarks are required");
        }
        RiskStatus action = project && req.actionStatus() != null
                ? req.actionStatus()
                : defaultApproveAction(option);
        LocalDate review = req.nextReviewDate() != null
                ? req.nextReviewDate()
                : (risk.getNextReviewDate() != null ? risk.getNextReviewDate() : LocalDate.now().plusMonths(12));
        risk.setFinalRiskStatus(FinalRiskStatus.APPROVE);
        risk.setValidatedAt(Instant.now());
        risk.setValidatedByUserId(admin.getId());
        risk.setNextReviewDate(review);
        risk.setStatus(action);
        if (APPROVE_CLOSES_RECORD.contains(action)) {
            risk.setClosedAt(Instant.now());
        }
        riskRepository.save(risk);
        if (req.reason() != null && !req.reason().isBlank()) {
            saveDecisionRemark(risk, admin, req.reason().trim());
        }
        if (action == RiskStatus.MITIGATING || action == RiskStatus.OVERDUE) {
            notifyRisk(risk, "RISK_MITIGATING", Map.of(
                    "displayId", risk.getDisplayId(),
                    "title", risk.getTitle(),
                    "nextReviewDate", review.toString()));
        } else {
            notifyFinalDecision(risk, action.name(), "Approved");
        }
        audit(admin, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "finalRiskStatus", "APPROVE:" + action.name());
        return toResponse(risk);
    }

    private static RiskStatus defaultApproveAction(TreatmentOption option) {
        return switch (option) {
            case TERMINATE -> RiskStatus.AVOIDED;
            case TRANSFER -> RiskStatus.TRANSFERRED;
            default -> RiskStatus.CLOSED;
        };
    }

    @Transactional(readOnly = true)
    public User requireUserForSeed(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.getRoles().size();
        return user;
    }

    public int countSeedStubsForIsProject(Long frameworkId) {
        if (frameworkId == null) {
            return 0;
        }
        var fw = frameworkQueryService.requireFramework(frameworkId);
        return defaultIsRiskLibrary.countMatching(fw, frameworkQueryService.nodesById(frameworkId));
    }

    @Transactional
    public void markIsProjectSeedFailed(long projectId) {
        riskProjectRepository.findById(projectId).ifPresent(p -> {
            p.setSeedStatus(RiskProjectSeedStatus.FAILED);
            riskProjectRepository.save(p);
        });
    }

    /**
     * Seed IS Risk register from Arbor Default Risk Excel when the project framework has one.
     * Unmatched control codes are skipped. No Excel means an empty project. Each create commits
     * so GET /api/risk-projects/{id} can show live riskCount.
     */
    public int seedStubsForIsProject(long userId, long projectId) {
        User user = self.requireUserForSeed(userId);
        RiskProject project = riskProjectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk project not found"));
        if (project.getFrameworkId() == null) {
            project.setSeedStatus(RiskProjectSeedStatus.DONE);
            riskProjectRepository.save(project);
            return 0;
        }
        var fw = frameworkQueryService.requireFramework(project.getFrameworkId());
        var nodes = frameworkQueryService.nodesById(project.getFrameworkId());
        Map<String, com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse> byCode = new java.util.HashMap<>();
        for (var node : nodes.values()) {
            if (node.retired()) {
                continue;
            }
            if (node.code() != null && !node.code().isBlank()) {
                byCode.putIfAbsent(node.code().trim().toLowerCase(java.util.Locale.ROOT), node);
            }
            if (node.officialRef() != null && !node.officialRef().isBlank()) {
                byCode.putIfAbsent(node.officialRef().trim().toLowerCase(java.util.Locale.ROOT), node);
            }
        }
        MatrixAxes axes = riskMatrixService.currentAxes();
        RiskLevel mid = axes.isActive(RiskLevel.MEDIUM) ? RiskLevel.MEDIUM
                : (axes.colLevels().length > 0 ? axes.colLevels()[axes.colLevels().length / 2] : RiskLevel.MEDIUM);
        int seeded = 0;
        for (DefaultIsRiskLibrary.DefaultRiskRow row : defaultIsRiskLibrary.matchingRows(fw, nodes)) {
            var node = byCode.get(row.controlCode().trim().toLowerCase(java.util.Locale.ROOT));
            if (node == null) {
                continue;
            }
            String code = node.code() != null ? node.code() : row.controlCode();
            TreatmentOption treatment = row.treatment() != null ? row.treatment() : TreatmentOption.TREAT;
            CreateRiskRequest req = new CreateRiskRequest(
                    RiskType.IS_RISK, null, row.title(), row.description(),
                    RiskCategory.IT, null, null, null,
                    project.getFrameworkId(), fw.getCode(), node.id(), code,
                    null, row.observedGap(), row.threats(), mid, mid, null, mid,
                    ResidualScoringMode.CONTROL_EFFECTIVENESS, null, null,
                    treatment, null, null, null, null, null, null, null,
                    null, null, null, null, null, null,
                    project.getId(), java.time.LocalDate.now(), null, null, null, null, null, null,
                    SEEDED_TREATMENT_ACTION_PLAN, null, null, null, null);
            self.create(user, req);
            seeded++;
        }
        RiskProject done = riskProjectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk project not found"));
        done.setSeedStatus(RiskProjectSeedStatus.DONE);
        riskProjectRepository.save(done);
        return seeded;
    }

    @Transactional(readOnly = true)
    public List<RiskAcceptanceFormResponse> listAcceptanceRegister(User user) {
        return acceptanceFormRepository.findByStatusOrderByUpdatedAtDesc(AcceptanceFormStatus.ACCEPTED).stream()
                .map(this::toAcceptanceResponse)
                .toList();
    }

    private void startFourTApproval(RiskRecord risk, User contributor, SubmitRiskRequest req, TreatmentOption option) {
        applyFourTFormFields(risk, req);
        Long previousOwnerId = risk.getRiskOwnerUserId();
        if (req.riskOwnerUserId() != null) {
            if (userRepository.findById(req.riskOwnerUserId()).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk Owner was not found");
            }
            risk.setRiskOwnerUserId(req.riskOwnerUserId());
        }
        notifyRiskOwnerNamed(risk, previousOwnerId);
        if (risk.getRiskOwnerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Risk Owner (Approver 1) is required");
        }
        String level2Label = level2RoleLabel(risk);
        if (risk.getRiskProjectId() != null) {
            requireInternalApprover(req.approver2UserId(), level2Label);
            rejectIfAlreadyOnRisk(risk, req.approver2UserId(), level2Label);
            if (req.approver3UserId() != null) {
                requireInternalApprover(req.approver3UserId(), "Level 3 approver");
                if (java.util.Objects.equals(req.approver2UserId(), req.approver3UserId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            level2Label + " and Level 3 approver must be different users");
                }
            }
        } else if (req.approver2UserId() != null || req.approver3UserId() != null) {
            requireInternalApprover(req.approver2UserId(), "Approver 2");
            rejectIfAlreadyOnRisk(risk, req.approver2UserId(), "Approver 2");
            if (req.approver3UserId() != null) {
                requireInternalApprover(req.approver3UserId(), "Approver 3");
                if (java.util.Objects.equals(req.approver2UserId(), req.approver3UserId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Approvers 2 and 3 must be different users");
                }
            }
        }
        RiskAcceptanceForm form = acceptanceFormRepository.findByRiskId(risk.getId()).orElseGet(() -> {
            RiskAcceptanceForm f = new RiskAcceptanceForm();
            f.setRiskId(risk.getId());
            return f;
        });
        if (form.getDisplayId() == null) {
            form.setDisplayId(idGenerator.nextAcceptanceId());
        } else {
            form.setCycleNo(form.getCycleNo() + 1);
        }
        form.setFormKind(switch (option) {
            case TRANSFER -> TreatmentFormKind.TRANSFER;
            case TERMINATE -> TreatmentFormKind.TERMINATE;
            default -> TreatmentFormKind.ACCEPT;
        });
        form.setApprover1UserId(risk.getRiskOwnerUserId());
        form.setApprover2UserId(req.approver2UserId());
        form.setApprover3UserId(req.approver3UserId());
        form.setApprover2Decision(null);
        form.setApprover2Comments(null);
        form.setApprover2ReviewedAt(null);
        form.setApprover3Decision(null);
        form.setApprover3Comments(null);
        form.setApprover3ReviewedAt(null);
        form.setRiskOwnerDecision(null);
        form.setAdminDecision(null);
        form.setSubmittedAt(Instant.now());
        form.setSubmittedByUserId(contributor.getId());
        form.setStatus(AcceptanceFormStatus.PENDING_RISK_OWNER);
        acceptanceFormRepository.save(form);
        ensureAcceptanceTask(risk, form, contributor.getId(), true);
    }

    private void applyFourTFormFields(RiskRecord risk, SubmitRiskRequest req) {
        requireText(req.acceptanceJustification(), "Justification for Accepting");
        requireText(req.compensatingControls(), "Compensating Controls");
        boolean project = risk.getRiskProjectId() != null;
        if (!project) {
            requireText(req.conditionsOfAcceptance(), "Conditions of acceptance");
            if (req.compControlOwnerId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compensating control owner is required");
            }
            if (req.compControlEffectiveness() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compensating control effectiveness is required");
            }
        }
        if (req.reviewDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acceptance Review Date is required");
        }
        DateRules.requireToleranceReviewDate(req.reviewDate());
        if (!Boolean.TRUE.equals(req.contributorDeclaration())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contributor declaration is required");
        }
        RiskAcceptanceForm form = acceptanceFormRepository.findByRiskId(risk.getId()).orElseGet(() -> {
            RiskAcceptanceForm f = new RiskAcceptanceForm();
            f.setRiskId(risk.getId());
            return f;
        });
        form.setJustification(req.acceptanceJustification().trim());
        form.setBusinessRationale(blankToNull(req.businessRationale()));
        form.setConditionsOfAcceptance(blankToNull(req.conditionsOfAcceptance()));
        form.setCompensatingControls(req.compensatingControls().trim());
        form.setCompControlOwnerId(req.compControlOwnerId());
        form.setCompControlEffectiveness(req.compControlEffectiveness());
        form.setReviewDate(req.reviewDate());
        form.setContributorDeclaration(true);
        acceptanceFormRepository.save(form);
        risk.setNextReviewDate(req.reviewDate());
    }

    /**
     * Champion inbox is a work task. Forms submitted before that existed (or after a missed
     * assign) stay {@code PENDING_*} with no row. Heal on My Tasks / register load:
     * current-turn card, and keep prior approvers (ERM Head / CISO) after they approve.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureAcceptanceInboxForUser(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        Long userId = user.getId();
        List<RiskAcceptanceForm> current = new java.util.ArrayList<>();
        current.addAll(acceptanceFormRepository.findByStatusAndApprover1UserId(
                AcceptanceFormStatus.PENDING_RISK_OWNER, userId));
        current.addAll(acceptanceFormRepository.findByStatusAndApprover2UserId(
                AcceptanceFormStatus.PENDING_APPROVER_2, userId));
        current.addAll(acceptanceFormRepository.findByStatusAndApprover3UserId(
                AcceptanceFormStatus.PENDING_APPROVER_3, userId));
        for (RiskAcceptanceForm form : current) {
            riskRepository.findById(form.getRiskId()).ifPresent(risk -> ensureAcceptanceTask(risk, form, null));
        }
        java.util.Collection<AcceptanceFormStatus> inFlight = List.of(
                AcceptanceFormStatus.PENDING_APPROVER_2,
                AcceptanceFormStatus.PENDING_APPROVER_3,
                AcceptanceFormStatus.PENDING_ADMIN);
        java.util.LinkedHashMap<Long, RiskAcceptanceForm> prior = new java.util.LinkedHashMap<>();
        for (RiskAcceptanceForm form : acceptanceFormRepository.findByApprover1UserIdAndStatusIn(userId, inFlight)) {
            prior.put(form.getId(), form);
        }
        for (RiskAcceptanceForm form : acceptanceFormRepository.findByApprover2UserIdAndStatusIn(userId, inFlight)) {
            prior.put(form.getId(), form);
        }
        for (RiskAcceptanceForm form : acceptanceFormRepository.findByApprover3UserIdAndStatusIn(userId, inFlight)) {
            prior.put(form.getId(), form);
        }
        for (RiskAcceptanceForm form : prior.values()) {
            if (isCurrentAcceptanceTurn(form, userId) || !alreadyApprovedAcceptance(form, userId)) {
                continue;
            }
            riskRepository.findById(form.getRiskId())
                    .ifPresent(risk -> keepCompletedAcceptanceTask(risk, form, userId));
        }
    }

    private static boolean isCurrentAcceptanceTurn(RiskAcceptanceForm form, Long userId) {
        AcceptanceFormStatus st = form.getStatus();
        if (st == AcceptanceFormStatus.PENDING_RISK_OWNER) {
            return userId.equals(form.getApprover1UserId());
        }
        if (st == AcceptanceFormStatus.PENDING_APPROVER_2) {
            return userId.equals(form.getApprover2UserId());
        }
        if (st == AcceptanceFormStatus.PENDING_APPROVER_3) {
            return userId.equals(form.getApprover3UserId());
        }
        return false;
    }

    private static boolean alreadyApprovedAcceptance(RiskAcceptanceForm form, Long userId) {
        if (userId.equals(form.getApprover1UserId()) && form.getRiskOwnerDecision() == AcceptanceDecision.APPROVED) {
            return true;
        }
        if (userId.equals(form.getApprover2UserId()) && form.getApprover2Decision() == AcceptanceDecision.APPROVED) {
            return true;
        }
        return userId.equals(form.getApprover3UserId()) && form.getApprover3Decision() == AcceptanceDecision.APPROVED;
    }

    private void ensureAcceptanceTask(RiskRecord risk, RiskAcceptanceForm form, Long assignedByUserId) {
        ensureAcceptanceTask(risk, form, assignedByUserId, false);
    }

    /** {@code submitted}: a Custodian (re)submit, so a reopened approval task mails its approver again. */
    private void ensureAcceptanceTask(RiskRecord risk, RiskAcceptanceForm form, Long assignedByUserId,
                                      boolean submitted) {
        Long assigneeId = null;
        String roleLabel = null;
        AcceptanceFormStatus st = form.getStatus();
        if (st == AcceptanceFormStatus.PENDING_RISK_OWNER) {
            assigneeId = form.getApprover1UserId();
            roleLabel = "Risk Owner";
        } else if (st == AcceptanceFormStatus.PENDING_APPROVER_2) {
            assigneeId = form.getApprover2UserId();
            roleLabel = level2RoleLabel(risk);
        } else if (st == AcceptanceFormStatus.PENDING_APPROVER_3) {
            assigneeId = form.getApprover3UserId();
            roleLabel = "Level 3 approver";
        } else {
            return;
        }
        WorkTask existing = assigneeId == null
                ? null
                : workTaskRepository
                        .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                                MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_ACCEPTANCE, risk.getId(),
                                assigneeId)
                        .orElse(null);
        if (existing != null
                && existing.getStatus() != WorkTaskStatus.SUBMITTED
                && existing.getStatus() != WorkTaskStatus.CANCELLED) {
            return;
        }
        boolean reopen = existing != null;
        assignAcceptanceTask(risk, form, assigneeId, roleLabel, assignedByUserId, submitted || !reopen);
    }

    private void assignAcceptanceTask(RiskRecord risk, RiskAcceptanceForm form, Long assigneeId,
                                      String roleLabel, Long assignedByUserId) {
        assignAcceptanceTask(risk, form, assigneeId, roleLabel, assignedByUserId, true);
    }

    private void assignAcceptanceTask(RiskRecord risk, RiskAcceptanceForm form, Long assigneeId,
                                      String roleLabel, Long assignedByUserId, boolean notify) {
        if (assigneeId == null) {
            completeAcceptanceTask(risk);
            return;
        }
        Long by = assignedByUserId != null ? assignedByUserId
                : (risk.getAssignedContributorId() != null ? risk.getAssignedContributorId() : assigneeId);
        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                        MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_ACCEPTANCE, risk.getId(), assigneeId)
                .orElseGet(WorkTask::new);
        task.setSourceModule(MyTasksService.MODULE_RISK);
        task.setSourceRecordType(MyTasksService.TYPE_RISK_ACCEPTANCE);
        task.setSourceRecordId(risk.getId());
        task.setTitle(risk.getDisplayId() + ": " + risk.getTitle());
        String ra = form.getDisplayId() != null ? form.getDisplayId() : "Risk Acceptance";
        task.setDetail(roleLabel + " · " + ra);
        task.setAssignedToUserId(assigneeId);
        task.setAssignedByUserId(by);
        task.setStatus(WorkTaskStatus.PENDING);
        task.setSubmittedAt(null);
        workTaskRepository.save(task);
        if (notify) {
            notificationService.trigger(
                    "RISK_PENDING_OWNER",
                    String.valueOf(risk.getId()),
                    List.of(String.valueOf(assigneeId)),
                    Map.of(
                            "displayId", risk.getDisplayId(),
                            "title", risk.getTitle(),
                            "role", roleLabel));
        }
    }

    /** Keep ERM Head / CISO / Owner on My Tasks after they approve (do not steal this row). */
    private void keepCompletedAcceptanceTask(RiskRecord risk, RiskAcceptanceForm form, Long userId) {
        if (userId == null) {
            return;
        }
        WorkTask existing = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                        MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_ACCEPTANCE, risk.getId(), userId)
                .orElse(null);
        if (existing != null) {
            return;
        }
        String roleLabel = userId.equals(form.getApprover2UserId())
                ? level2RoleLabel(risk)
                : (userId.equals(form.getApprover3UserId()) ? "Level 3 approver" : "Risk Owner");
        Instant decidedAt = userId.equals(form.getApprover2UserId())
                ? form.getApprover2ReviewedAt()
                : (userId.equals(form.getApprover3UserId()) ? form.getApprover3ReviewedAt() : form.getRiskOwnerReviewedAt());
        WorkTask task = new WorkTask();
        task.setSourceModule(MyTasksService.MODULE_RISK);
        task.setSourceRecordType(MyTasksService.TYPE_RISK_ACCEPTANCE);
        task.setSourceRecordId(risk.getId());
        task.setTitle(risk.getDisplayId() + ": " + risk.getTitle());
        String ra = form.getDisplayId() != null ? form.getDisplayId() : "Risk Acceptance";
        task.setDetail(roleLabel + " · " + ra);
        task.setAssignedToUserId(userId);
        Long by = risk.getAssignedContributorId() != null ? risk.getAssignedContributorId() : userId;
        task.setAssignedByUserId(by);
        task.setStatus(WorkTaskStatus.SUBMITTED);
        task.setSubmittedAt(decidedAt != null ? decidedAt : Instant.now());
        workTaskRepository.save(task);
    }

    private void completeAcceptanceTask(RiskRecord risk) {
        workTaskRepository
                .findAllBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_ACCEPTANCE, risk.getId())
                .forEach(task -> {
                    if (task.getStatus() == WorkTaskStatus.SUBMITTED
                            || task.getStatus() == WorkTaskStatus.CANCELLED) {
                        return;
                    }
                    task.setStatus(WorkTaskStatus.SUBMITTED);
                    task.setSubmittedAt(Instant.now());
                    workTaskRepository.save(task);
                });
    }

    private void cancelAcceptanceTasks(RiskRecord risk) {
        workTaskRepository
                .findAllBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_ACCEPTANCE, risk.getId())
                .forEach(task -> {
                    if (task.getStatus() == WorkTaskStatus.CANCELLED) {
                        return;
                    }
                    task.setStatus(WorkTaskStatus.CANCELLED);
                    workTaskRepository.save(task);
                });
    }

    private void completeAcceptanceTaskForUser(RiskRecord risk, Long userId) {
        if (userId == null) {
            return;
        }
        workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                        MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_ACCEPTANCE, risk.getId(), userId)
                .ifPresent(task -> {
                    if (task.getStatus() == WorkTaskStatus.SUBMITTED
                            || task.getStatus() == WorkTaskStatus.CANCELLED) {
                        return;
                    }
                    task.setStatus(WorkTaskStatus.SUBMITTED);
                    task.setSubmittedAt(Instant.now());
                    workTaskRepository.save(task);
                });
    }

    private void returnAcceptanceToContributor(RiskRecord risk, String reason, User actor, String notifyTrigger,
                                               String rejectorRole) {
        risk.setRejectionReason(reason);
        if (risk.getRiskProjectId() != null) {
            risk.setStatus(RiskStatus.PENDING_ACCEPTANCE);
            riskRepository.save(risk);
            if (risk.getWorkTaskId() != null) {
                workTaskRepository.findById(risk.getWorkTaskId()).ifPresent(task -> {
                    task.setStatus(WorkTaskStatus.PENDING);
                    task.setSubmittedAt(null);
                    workTaskRepository.save(task);
                });
            }
            if (notifyTrigger != null) {
                notifyAcceptanceReturned(risk, reason, actor, notifyTrigger, rejectorRole);
            }
            audit(actor, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                    "rejectionReason", reason);
            return;
        }
        returnSendToContributor(risk, reason, actor, null);
        if (notifyTrigger != null) {
            notifyAcceptanceReturned(risk, reason, actor, notifyTrigger, rejectorRole);
        }
    }

    private void requireEnabledUser(Long userId, String label) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " not found"));
        if (u.getStatus() == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is disabled");
        }
    }

    private static final java.util.Set<Role> EXTERNAL_APPROVER_ROLES =
            java.util.Set.of(Role.VENDOR, Role.INTERNAL_AUDITOR, Role.EXTERNAL_AUDITOR);

    private void requireInternalApprover(Long userId, String label) {
        requireEnabledUser(userId, label);
        User u = userRepository.findById(userId).orElseThrow();
        if (u.getRoles().stream().anyMatch(EXTERNAL_APPROVER_ROLES::contains)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be an internal user");
        }
    }

    private void rejectIfAlreadyOnRisk(RiskRecord risk, Long userId, String label) {
        if (java.util.Objects.equals(userId, risk.getRiskOwnerUserId())
                || java.util.Objects.equals(userId, risk.getAssignedContributorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " cannot be the Risk Owner or Risk Custodian");
        }
    }

    private static String level2RoleLabel(RiskRecord risk) {
        return switch (risk.getRiskType()) {
            case ERM -> "ERM Head";
            case PRIVACY_RISK -> "Data Privacy Officer";
            case IS_RISK -> "CISO";
        };
    }

    private void sendAcceptanceToAdmin(RiskRecord risk, RiskAcceptanceForm form) {
        form.setStatus(AcceptanceFormStatus.PENDING_ADMIN);
        acceptanceFormRepository.save(form);
        risk.setStatus(RiskStatus.PENDING_ADMIN_REVIEW);
        completeAcceptanceTask(risk);
        notifyPendingAdmin(risk);
    }


    private RiskRecordResponse approverDecideChain(User user, RiskRecord risk, RiskAcceptanceForm form,
                                                   AcceptanceOwnerDecideRequest req) {
        if (!Boolean.TRUE.equals(req.declaration())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Declaration is required");
        }
        if (req.decision() != AcceptanceDecision.APPROVED && req.decision() != AcceptanceDecision.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }
        AcceptanceFormStatus st = form.getStatus();
        int step;
        if (st == AcceptanceFormStatus.PENDING_RISK_OWNER && user.getId().equals(form.getApprover1UserId())) {
            step = 1;
        } else if (st == AcceptanceFormStatus.PENDING_APPROVER_2 && user.getId().equals(form.getApprover2UserId())) {
            step = 2;
        } else if (st == AcceptanceFormStatus.PENDING_APPROVER_3 && user.getId().equals(form.getApprover3UserId())) {
            step = 3;
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not this approver's turn");
        }
        if (req.decision() == AcceptanceDecision.REJECTED) {
            if (req.comments() == null || req.comments().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reject reason is required");
            }
            if (step == 1) {
                form.setRiskOwnerDecision(AcceptanceDecision.REJECTED);
                form.setRiskOwnerComments(blankToNull(req.comments()));
                form.setRiskOwnerReviewedAt(Instant.now());
            } else if (step == 2) {
                form.setApprover2Decision(AcceptanceDecision.REJECTED);
                form.setApprover2Comments(blankToNull(req.comments()));
                form.setApprover2ReviewedAt(Instant.now());
            } else {
                form.setApprover3Decision(AcceptanceDecision.REJECTED);
                form.setApprover3Comments(blankToNull(req.comments()));
                form.setApprover3ReviewedAt(Instant.now());
            }
            form.setStatus(AcceptanceFormStatus.REJECTED);
            acceptanceFormRepository.save(form);
            cancelAcceptanceTasks(risk);
            String rejectorRole = step == 1 ? "the Risk Owner"
                    : step == 2 ? "the " + level2RoleLabel(risk) : "the Level 3 approver";
            returnAcceptanceToContributor(risk, req.comments().trim(), user, "RISK_OWNER_REJECTED", rejectorRole);
            return toResponse(risk);
        }
        if (step == 1) {
            form.setRiskOwnerDecision(AcceptanceDecision.APPROVED);
            form.setRiskOwnerComments(blankToNull(req.comments()));
            form.setRiskOwnerReviewedAt(Instant.now());
            form.setStatus(AcceptanceFormStatus.PENDING_APPROVER_2);
            acceptanceFormRepository.save(form);
            risk.setStatus(RiskStatus.PENDING_ACCEPTANCE);
            completeAcceptanceTaskForUser(risk, user.getId());
            assignAcceptanceTask(risk, form, form.getApprover2UserId(), level2RoleLabel(risk), user.getId());
        } else if (step == 2) {
            form.setApprover2Decision(AcceptanceDecision.APPROVED);
            form.setApprover2Comments(blankToNull(req.comments()));
            form.setApprover2ReviewedAt(Instant.now());
            completeAcceptanceTaskForUser(risk, user.getId());
            if (form.getApprover3UserId() != null) {
                form.setStatus(AcceptanceFormStatus.PENDING_APPROVER_3);
                acceptanceFormRepository.save(form);
                risk.setStatus(RiskStatus.PENDING_ACCEPTANCE);
                assignAcceptanceTask(risk, form, form.getApprover3UserId(), "Level 3 approver", user.getId());
            } else {
                sendAcceptanceToAdmin(risk, form);
            }
        } else {
            form.setApprover3Decision(AcceptanceDecision.APPROVED);
            form.setApprover3Comments(blankToNull(req.comments()));
            form.setApprover3ReviewedAt(Instant.now());
            completeAcceptanceTaskForUser(risk, user.getId());
            sendAcceptanceToAdmin(risk, form);
        }
        riskRepository.save(risk);
        audit(user, AuditActionType.UPDATED, String.valueOf(risk.getId()), risk.getDisplayId(),
                "acceptanceStep", String.valueOf(step));
        return toResponse(risk);
    }

}
