package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.catalog.domain.FrameworkQuestion;
import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.catalog.service.CloudNodeSupport;
import com.a9.arbor.grc.catalog.service.FrameworkLibraryService;
import com.a9.arbor.grc.catalog.service.FrameworkQueryService;
import com.a9.arbor.grc.catalog.service.FrameworkQuestionService;
import com.a9.arbor.grc.catalog.service.FrameworkScoringSupport;
import com.a9.arbor.grc.catalog.service.ModuleRouting;
import com.a9.arbor.grc.catalog.web.dto.FrameworkDetailResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkQuestionResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkSummaryResponse;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.capa.service.CapaService;
import com.a9.arbor.grc.modules.capa.web.dto.CapaResponse;
import com.a9.arbor.grc.modules.export.AssessmentWorkbookWriter;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.export.ReportPdfWriter;
import com.a9.arbor.grc.modules.gap.pack.GapContributorText;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentType;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapItemCustomQuestion;
import com.a9.arbor.grc.modules.gap.domain.GapItemAnswer;
import com.a9.arbor.grc.modules.gap.domain.GapItemReviewStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItemSubmission;
import com.a9.arbor.grc.modules.gap.domain.GapQuestionAssignmentMode;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemCustomQuestionRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemAnswerRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemSubmissionRepository;
import com.a9.arbor.grc.modules.gap.web.dto.AssignGapItemRequest;
import com.a9.arbor.grc.modules.gap.web.dto.CreateGapAssessmentRequest;
import com.a9.arbor.grc.modules.gap.web.dto.ControlAssessmentStatusResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapItemLocatorResponse;
import com.a9.arbor.grc.modules.gap.web.dto.FinalizeGapItemRequest;
import com.a9.arbor.grc.modules.gap.web.dto.GapAssessmentDetailResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapAssessmentSummaryResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;
import com.a9.arbor.grc.modules.gap.web.dto.GapItemResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapItemSubmissionResponse;
import com.a9.arbor.grc.modules.gap.web.dto.CreateGapCustomQuestionRequest;
import com.a9.arbor.grc.modules.gap.web.dto.GapCustomQuestionResponse;
import com.a9.arbor.grc.modules.gap.web.dto.GapQuestionAnswerResponse;
import com.a9.arbor.grc.modules.gap.web.dto.SendBackGapItemRequest;
import com.a9.arbor.grc.modules.gap.web.dto.SendBackGapItemsRequest;
import com.a9.arbor.grc.modules.gap.web.dto.SendGapItemsRequest;
import com.a9.arbor.grc.modules.gap.web.dto.SetGapFlagRequest;
import com.a9.arbor.grc.modules.gap.web.dto.UpdateGapItemRequest;
import com.a9.arbor.grc.modules.gap.web.dto.UpsertGapAnswersRequest;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.iam.service.ContributorAccessService;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.project.service.ComplianceProjectService;
import com.a9.arbor.grc.modules.soa.domain.Soa;
import com.a9.arbor.grc.modules.soa.domain.SoaApplicability;
import com.a9.arbor.grc.modules.soa.domain.SoaItem;
import com.a9.arbor.grc.modules.soa.service.SoaService;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GapAssessmentService {

    private final GapAssessmentRepository assessmentRepository;
    private final GapItemRepository itemRepository;
    private final GapItemSubmissionRepository submissionRepository;
    private final GapItemAnswerRepository answerRepository;
    private final GapItemCustomQuestionRepository customQuestionRepository;
    private final FrameworkQuestionService frameworkQuestionService;
    private final FrameworkQueryService frameworkQueryService;
    private final FrameworkLibraryService frameworkLibraryService;
    private final SoaService soaService;
    private final ComplianceProjectService projectService;
    private final WorkTaskRepository workTaskRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final AssessmentWorkbookWriter workbookWriter;
    private final ReportPdfWriter pdfWriter;
    private final TenantRepository tenantRepository;
    private final CapaService capaService;
    private final ContributorAccessService contributorAccess;
    private final CrossFrameworkDualCreditService crossFrameworkDualCreditService;
    private final com.a9.arbor.grc.catalog.service.ModuleLicensingService moduleLicensingService;
    private final com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService adminModuleAccessService;
    private final GapFrameworkMeaning gapFrameworkMeaning;

    public GapAssessmentService(GapAssessmentRepository assessmentRepository,
                                GapItemRepository itemRepository,
                                GapItemSubmissionRepository submissionRepository,
                                GapItemAnswerRepository answerRepository,
                                GapItemCustomQuestionRepository customQuestionRepository,
                                FrameworkQuestionService frameworkQuestionService,
                                FrameworkQueryService frameworkQueryService,
                                FrameworkLibraryService frameworkLibraryService,
                                SoaService soaService,
                                ComplianceProjectService projectService,
                                WorkTaskRepository workTaskRepository,
                                UserRepository userRepository,
                                NotificationService notificationService,
                                AuditTrailService auditTrailService,
                                AssessmentWorkbookWriter workbookWriter,
                                ReportPdfWriter pdfWriter,
                                TenantRepository tenantRepository,
                                CapaService capaService,
                                ContributorAccessService contributorAccess,
                                CrossFrameworkDualCreditService crossFrameworkDualCreditService,
                                com.a9.arbor.grc.catalog.service.ModuleLicensingService moduleLicensingService,
                                com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService adminModuleAccessService,
                                GapFrameworkMeaning gapFrameworkMeaning) {
        this.assessmentRepository = assessmentRepository;
        this.itemRepository = itemRepository;
        this.submissionRepository = submissionRepository;
        this.answerRepository = answerRepository;
        this.customQuestionRepository = customQuestionRepository;
        this.frameworkQuestionService = frameworkQuestionService;
        this.frameworkQueryService = frameworkQueryService;
        this.frameworkLibraryService = frameworkLibraryService;
        this.soaService = soaService;
        this.projectService = projectService;
        this.workTaskRepository = workTaskRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
        this.workbookWriter = workbookWriter;
        this.pdfWriter = pdfWriter;
        this.tenantRepository = tenantRepository;
        this.capaService = capaService;
        this.contributorAccess = contributorAccess;
        this.crossFrameworkDualCreditService = crossFrameworkDualCreditService;
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
        this.gapFrameworkMeaning = gapFrameworkMeaning;
    }

    @Transactional(readOnly = true)
    public List<GapAssessmentSummaryResponse> list(User user, Long projectId) {
        List<GapAssessment> rows = projectId == null
                ? assessmentRepository.findAllByOrderByUpdatedAtDesc()
                : assessmentRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        if (RoleAccess.isContributorOnly(user)) {
            Set<Long> mine = assignedAssessmentIds(user.getId());
            rows = rows.stream().filter(a -> mine.contains(a.getId())).toList();
        }
        rows = rows.stream().filter(a -> gapVisible(a.getFrameworkId())).toList();
        return rows.stream().map(this::toSummary).toList();
    }

    /**
     * Doc 02 R-09: newest Gap item for framework + control (risk identification pull).
     */
    @Transactional(readOnly = true)
    public ControlAssessmentStatusResponse latestControlAssessmentStatus(long frameworkId, long frameworkNodeId) {
        List<GapAssessment> assessments = assessmentRepository.findByFrameworkIdOrderByUpdatedAtDesc(frameworkId);
        for (GapAssessment a : assessments) {
            var item = itemRepository.findByGapAssessmentIdAndFrameworkNodeId(a.getId(), frameworkNodeId);
            if (item.isPresent()) {
                return ControlAssessmentStatusResponse.from(a, item.get());
            }
        }
        return ControlAssessmentStatusResponse.empty();
    }

    /**
     * Notification deep-link: Gap control {@code recordId} → assessment + project.
     */
    @Transactional(readOnly = true)
    public GapItemLocatorResponse locateItem(User user, long itemId) {
        GapItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap control not found"));
        GapAssessment a = require(item.getGapAssessmentId());
        assertGapVisible(a.getFrameworkId());
        if (RoleAccess.isContributorOnly(user)) {
            contributorAccess.assertCanAccessGapItem(user, itemId);
        }
        return new GapItemLocatorResponse(item.getId(), a.getId(), a.getProjectId());
    }

    @Transactional(readOnly = true)
    public GapAssessmentDetailResponse get(User user, long id) {
        GapAssessment a = require(id);
        assertGapVisible(a.getFrameworkId());
        var nodes = frameworkQueryService.nodesById(a.getFrameworkId());
        gapFrameworkMeaning.fillEmpty(id, nodes);
        List<GapItem> ordered = itemsInExcelOrder(id, nodes);
        if (RoleAccess.isContributorOnly(user)) {
            ordered = ordered.stream()
                    .filter(item -> visibleToContributor(item, user.getId()))
                    .toList();
            if (ordered.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found");
            }
        }
        List<GapItemResponse> items = ordered.stream()
                .map(item -> toListItemResponse(item, nodes))
                .toList();
        return new GapAssessmentDetailResponse(toSummary(a, items), items);
    }

    @Transactional(readOnly = true)
    public GapAssessmentSummaryResponse header(User user, long id) {
        GapAssessment a = require(id);
        assertGapVisible(a.getFrameworkId());
        if (RoleAccess.isContributorOnly(user) && !assignedAssessmentIds(user.getId()).contains(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found");
        }
        return toSummary(a);
    }

    @Transactional(readOnly = true)
    public GapItemResponse getItem(User user, long assessmentId, long itemId) {
        GapAssessment a = require(assessmentId);
        GapItem item = itemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap control not found"));
        if (RoleAccess.isContributorOnly(user)) {
            contributorAccess.assertCanAccessGapItem(user, itemId);
        }
        var nodes = frameworkQueryService.nodePath(a.getFrameworkId(), item.getFrameworkNodeId());
        return meaningResponse(item, nodes, toItemResponse(item, nodes));
    }

    /**
     * Write response for one control. Loads that control's catalog path only.
     * Meaning backfill runs when the assessment is opened, not on each save.
     */
    private GapItemResponse respondSaved(GapAssessment assessment, GapItem item) {
        var nodes = frameworkQueryService.nodePath(assessment.getFrameworkId(), item.getFrameworkNodeId());
        return meaningResponse(item, nodes, toItemResponse(item, nodes));
    }

    /** Enrich a gap item with catalog definition/priority (used by My Tasks). */
    @Transactional(readOnly = true)
    public GapItemResponse toItemResponse(GapItem item) {
        GapAssessment a = require(item.getGapAssessmentId());
        var nodes = frameworkQueryService.nodesById(a.getFrameworkId());
        gapFrameworkMeaning.fillEmpty(a.getId(), nodes);
        return meaningResponse(item, nodes, toItemResponse(item, nodes));
    }

    private GapItemResponse toListItemResponse(GapItem item,
                                               Map<Long, com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse> nodes) {
        var node = nodes != null ? nodes.get(item.getFrameworkNodeId()) : null;
        String[] domain = clauseHeading(node, nodes);
        return meaningResponse(item, nodes, GapItemResponse.fromList(
                item, node != null ? node.priority() : null, domain[0], domain[1]));
    }

    /**
     * Prefer the value stored on the gap control. When that is still null, show the framework
     * column. An empty string is an admin clear and stays empty.
     */
    private static GapItemResponse meaningResponse(
            GapItem item,
            Map<Long, FrameworkNodeResponse> nodes,
            GapItemResponse response) {
        FrameworkNodeResponse node = nodes != null ? nodes.get(item.getFrameworkNodeId()) : null;
        String what = item.getWhatItMeans() != null
                ? item.getWhatItMeans()
                : GapFrameworkMeaning.catalogAttr(node, "what_it_means");
        String evidence = item.getEvidenceGuidance() != null
                ? item.getEvidenceGuidance()
                : GapFrameworkMeaning.catalogAttr(node, "evidence", "evidence_examples");
        return response.withMeaning(what, evidence);
    }

    private GapItemResponse toItemResponse(GapItem item,
                                           Map<Long, com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse> nodes) {
        var node = nodes != null ? nodes.get(item.getFrameworkNodeId()) : null;
        String definition = item.getDefinitionText() != null
                ? item.getDefinitionText()
                : (node != null ? node.definition() : null);
        java.time.LocalDate dueDate = null;
        if (item.getWorkTaskId() != null) {
            dueDate = workTaskRepository.findById(item.getWorkTaskId())
                    .map(task -> task.getDueDate())
                    .orElse(null);
        }
        if (dueDate == null) {
            dueDate = item.getPendingDueDate();
        }
        String[] domain = clauseHeading(node, nodes);
        return GapItemResponse.from(
                item,
                definition,
                node != null ? node.priority() : null,
                dueDate,
                domain[0],
                domain[1]);
    }

    /** Top catalog ancestor code and title, used as the Gap section heading. */
    private static String[] clauseHeading(
            FrameworkNodeResponse node,
            Map<Long, FrameworkNodeResponse> nodes) {
        if (node == null || nodes == null || nodes.isEmpty()) {
            return new String[] {null, null};
        }
        FrameworkNodeResponse top = node;
        FrameworkNodeResponse cur = node;
        int guard = 0;
        while (cur.parentId() != null && guard++ < 12) {
            FrameworkNodeResponse parent = nodes.get(cur.parentId());
            if (parent == null) {
                break;
            }
            top = parent;
            cur = parent;
        }
        if (top == node) {
            return new String[] {null, null};
        }
        String code = top.code() == null ? null : top.code().trim();
        String title = top.title() == null ? "" : top.title().trim();
        if (code == null || code.isEmpty()) {
            return new String[] {null, null};
        }
        if (title.isEmpty() || title.equalsIgnoreCase(code)) {
            return new String[] {code, null};
        }
        return new String[] {code, title};
    }

    /**
     * Excel export for IN_PROGRESS or COMPLETED Gap (Path B6).
     * OWNER/ADMIN/INTERNAL_AUDITOR/EXTERNAL_AUDITOR: not Contributor/Global Reader.
     */
    @Transactional
    public ExportFile exportExcel(User user, long id) {
        GapAssessment a = requireExportable(id);
        var nodes = frameworkQueryService.nodesById(a.getFrameworkId());
        gapFrameworkMeaning.fillEmpty(id, nodes);
        List<GapItem> items = itemsInExcelOrder(id, nodes);
        byte[] bytes = workbookWriter.writeGap(a, items);
        String filename = "Gap-" + safeFilePart(a.getName()) + "-" + a.getId() + ".xlsx";
        audit(user, AuditActionType.EXPORT, "GAP_ASSESSMENT", String.valueOf(a.getId()),
                a.getName(), "export", null, "XLSX");
        return new ExportFile(bytes, filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /** PDF export for IN_PROGRESS or COMPLETED Gap (§19.3). Same role gate as Excel. */
    @Transactional
    public ExportFile exportPdf(User user, long id) {
        GapAssessment a = requireExportable(id);
        var nodes = frameworkQueryService.nodesById(a.getFrameworkId());
        gapFrameworkMeaning.fillEmpty(id, nodes);
        List<GapItem> items = itemsInExcelOrder(id, nodes);
        byte[] bytes = pdfWriter.writeGap(orgName(), user, a, items);
        String filename = "Gap-" + safeFilePart(a.getName()) + "-" + a.getId() + ".pdf";
        audit(user, AuditActionType.EXPORT, "GAP_ASSESSMENT", String.valueOf(a.getId()),
                a.getName(), "export", null, "PDF");
        return new ExportFile(bytes, filename, "application/pdf");
    }

    private GapAssessment requireExportable(long id) {
        GapAssessment a = require(id);
        assertFrameworkWritable(a.getFrameworkId());
        Set<GapAssessmentStatus> allowed = EnumSet.of(
                GapAssessmentStatus.IN_PROGRESS, GapAssessmentStatus.COMPLETED);
        if (!allowed.contains(a.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Gap assessment must be IN_PROGRESS or COMPLETED before export (status="
                            + a.getStatus() + ")");
        }
        return a;
    }

    private String orgName() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return "-";
        }
        return tenantRepository.findByTenantId(tenantId).map(Tenant::getName).orElse(tenantId);
    }

    private static String safeFilePart(String raw) {
        if (raw == null || raw.isBlank()) {
            return "gap";
        }
        String s = raw.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
        return s.isBlank() ? "gap" : s.substring(0, Math.min(s.length(), 64));
    }

    @Transactional(readOnly = true)
    public List<GapItemSubmissionResponse> listSubmissions(long assessmentId, long itemId) {
        require(assessmentId);
        itemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        return submissionRepository.findByGapItemIdOrderBySubmissionNumberAsc(itemId).stream()
                .map(GapItemSubmissionResponse::from)
                .toList();
    }

    @Transactional
    public GapAssessmentDetailResponse create(User user, CreateGapAssessmentRequest req) {
        projectService.requireActive(req.projectId());
        projectService.assertFrameworkInProject(req.projectId(), req.frameworkId());

        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant context required");
        }
        FrameworkDetailResponse controls = frameworkLibraryService.getActivatedControls(
                tenantId, req.frameworkId(), ModuleRouting.GAP);
        FrameworkSummaryResponse fw = controls.framework();
        assertFrameworkWritable(fw.id());
        if (req.assessmentType() == GapAssessmentType.SELF) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type must be Internal or External");
        }
        if (req.scope() == null || req.scope().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scope is required");
        }
        User assessor = requireAssessor(req.assessorUserId());
        boolean includeCloud = projectService.require(req.projectId()).isIncludeCloud();
        boolean alwaysApplicable = FrameworkScoringSupport.isUaeIa(fw.code(), fw.name())
                || FrameworkScoringSupport.isNcema(fw.code(), fw.name());

        // Option A: SoA optional. Prefer complete SoA applicability when present; else catalog seed.
        var soaOpt = soaService.findCompleteForProjectFramework(req.projectId(), req.frameworkId());

        GapAssessment a = new GapAssessment();
        a.setProjectId(req.projectId());
        a.setFrameworkId(fw.id());
        a.setFrameworkCode(fw.code());
        a.setFrameworkName(fw.name());
        a.setFrameworkVersion(fw.version());
        a.setSoaId(soaOpt.map(Soa::getId).orElse(null));
        a.setName(req.name().trim());
        a.setAssessmentType(req.assessmentType());
        DateRules.requireActionDueDateIfPresent(req.targetDate(), "Target date");
        a.setTargetDate(req.targetDate());
        a.setScope(blankToNull(req.scope()));
        a.setAssessorUserId(assessor.getId());
        a.setStatus(GapAssessmentStatus.DRAFT);
        a.setCreatedByUserId(user.getId());
        int depth = resolveAssessmentDepth(req.assessmentDepth(), fw.code(), fw.name());
        a.setAssessmentDepth(depth);
        assessmentRepository.save(a);

        // Always seed Gap-routed catalog leaves (UAE IA default = L4 sub-controls). Depth 3
        // (UAE IA only) walks each leaf to its L3 control and deduplicates. When a complete SoA
        // exists, inherit applicability from the matching SoA node or an ancestor (control → leaf).
        Map<Long, SoaApplicability> soaByNodeId = Map.of();
        if (soaOpt.isPresent()) {
            soaByNodeId = soaService.itemsForSoa(soaOpt.get().getId()).stream()
                    .collect(Collectors.toMap(SoaItem::getFrameworkNodeId, SoaItem::getApplicability,
                            (a1, a2) -> a1));
        }
        Map<Long, FrameworkNodeResponse> allNodes =
                frameworkQueryService.nodesById(fw.id());

        Set<Long> seededNodeIds = new LinkedHashSet<>();
        for (FrameworkNodeResponse gapLeaf : controls.nodes()) {
            if (!includeCloud && CloudNodeSupport.isCloud(gapLeaf)) {
                continue;
            }
            FrameworkNodeResponse node = UaeIaGapScoring.seedNode(gapLeaf, allNodes, depth);
            if (node == null || !seededNodeIds.add(node.id())) {
                continue;
            }
            GapItem item = new GapItem();
            item.setGapAssessmentId(a.getId());
            item.setFrameworkNodeId(node.id());
            item.setNodeCode(node.code());
            item.setTitle(node.title());
            item.setDefinitionText(node.definition());
            GapApplicability applicability = GapApplicability.APPLICABLE;
            if (!alwaysApplicable && !soaByNodeId.isEmpty()) {
                SoaApplicability soaApp = resolveSoaApplicability(node, soaByNodeId, allNodes);
                if (soaApp == SoaApplicability.NOT_APPLICABLE) {
                    applicability = GapApplicability.NOT_APPLICABLE;
                }
            }
            item.setApplicability(applicability);
            item.setWhatItMeans(GapFrameworkMeaning.catalogAttr(node, "what_it_means"));
            item.setEvidenceGuidance(GapFrameworkMeaning.catalogAttr(node, "evidence", "evidence_examples"));
            item.setApplicabilityOverride(false);
            item.setReviewStatus(GapItemReviewStatus.OPEN);
            if (applicability == GapApplicability.NOT_APPLICABLE) {
                item.setScore(null);
                item.setImplementationStatus(null);
                item.setMaturity(null);
            }
            itemRepository.save(item);
        }

        refreshScores(a);
        assessmentRepository.save(a);

        audit(user, AuditActionType.CREATED, "GAP_ASSESSMENT", String.valueOf(a.getId()),
                a.getName(), "seed", null, soaOpt.isPresent() ? "SOA" : "CATALOG");
        notifyGapAssessor(a, "GAP_ASSESSMENT_CREATED");
        return get(user, a.getId());
    }

    /**
     * Walk the Gap leaf and its ancestors until a SoA row matches (control-level SoA → sub-control Gap).
     */
    private static SoaApplicability resolveSoaApplicability(FrameworkNodeResponse node,
                                                            Map<Long, SoaApplicability> soaByNodeId,
                                                            Map<Long, FrameworkNodeResponse> allNodes) {
        FrameworkNodeResponse current = node;
        while (current != null) {
            SoaApplicability hit = soaByNodeId.get(current.id());
            if (hit != null) {
                return hit;
            }
            Long parentId = current.parentId();
            current = parentId != null ? allNodes.get(parentId) : null;
        }
        return SoaApplicability.APPLICABLE;
    }

    private static boolean isUaeIa(GapAssessment a) {
        return FrameworkScoringSupport.isUaeIa(a.getFrameworkCode(), a.getFrameworkName());
    }

    /** UAE IA locks Final only for Fully Implemented. Other frameworks accept every score. */
    private static boolean uaeIaFinalStatusAllowed(GapAssessment a, GapImplementationStatus status) {
        if (!isUaeIa(a)) {
            return true;
        }
        return status == GapImplementationStatus.FULLY_IMPLEMENTED;
    }

    /** UAE IA: 3 = L3 controls, 4 = L4 sub-controls. Other frameworks always seed GAP leaves (4). */
    private static int resolveAssessmentDepth(Integer requested, String frameworkCode, String frameworkName) {
        if (!FrameworkScoringSupport.isUaeIa(frameworkCode, frameworkName)) {
            return 4;
        }
        int depth = requested != null ? requested : 4;
        if (depth != 3 && depth != 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assessmentDepth must be 3 or 4");
        }
        return depth;
    }

    @Transactional
    public GapItemResponse updateItem(User user, long assessmentId, long itemId, UpdateGapItemRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        GapItem item = itemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        GapApplicability scoreApplicability = item.getApplicability();
        var scoreImplementation = item.getImplementationStatus();
        Integer scoreMaturity = item.getMaturity();
        String scoreGates = item.getMaturityGatesJson();
        GapItemReviewStatus scoreReview = item.getReviewStatus();

        boolean contributorOnly = RoleAccess.isContributorOnly(user);
        boolean elevated = RoleAccess.isElevated(user.getRoles());

        if (contributorOnly) {
            if (!visibleToContributor(item, user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this control");
            }
            if (item.getReviewStatus() == GapItemReviewStatus.SUBMITTED
                    || item.getReviewStatus() == GapItemReviewStatus.FINALIZED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Control is under Admin review: wait for send-back to edit");
            }
            if (req.applicability() != null && req.applicability() != item.getApplicability()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Contributors cannot change applicability: use Admin");
            }
        }

        boolean alwaysApplicable = FrameworkScoringSupport.isUaeIa(a.getFrameworkCode(), a.getFrameworkName())
                || FrameworkScoringSupport.isNcema(a.getFrameworkCode(), a.getFrameworkName());
        if (alwaysApplicable && req.applicability() == GapApplicability.NOT_APPLICABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Applicability is always Applicable for this framework");
        }
        if (alwaysApplicable && item.getApplicability() != GapApplicability.APPLICABLE) {
            item.setApplicability(GapApplicability.APPLICABLE);
            item.setApplicabilityOverride(false);
            item.setOverrideJustification(null);
        }
        if (!contributorOnly) {
            if (req.whatItMeans() != null) {
                item.setWhatItMeans(req.whatItMeans().isBlank() ? "" : req.whatItMeans().trim());
            }
            if (req.evidenceGuidance() != null) {
                item.setEvidenceGuidance(req.evidenceGuidance().isBlank() ? "" : req.evidenceGuidance().trim());
            }
        }

        GapApplicability previous = item.getApplicability();
        if (req.applicability() != null && req.applicability() != previous) {
            boolean linkedToSoa = a.getSoaId() != null;
            if (req.overrideJustification() == null || req.overrideJustification().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        linkedToSoa
                                ? "Override justification required when changing applicability from SoA"
                                : "Justification required when changing applicability");
            }
            item.setApplicability(req.applicability());
            // Override flag only when diverging from a linked SoA (drives post-complete SoA prompt).
            item.setApplicabilityOverride(linkedToSoa);
            item.setOverrideJustification(req.overrideJustification().trim());
        } else if (req.overrideJustification() != null && item.isApplicabilityOverride()) {
            item.setOverrideJustification(blankToNull(req.overrideJustification()));
        }

        boolean assignedToContributor = item.getAssignedToUserId() != null;
        boolean protectContributorInput = elevated && assignedToContributor
                && (item.getReviewStatus() == GapItemReviewStatus.SUBMITTED
                || item.getReviewStatus() == GapItemReviewStatus.FINALIZED
                || item.getReviewStatus() == GapItemReviewStatus.SENT_BACK);

        if (item.getApplicability() == GapApplicability.NOT_APPLICABLE) {
            item.setImplementationStatus(null);
            item.setFinalImplementationStatus(null);
            item.setMaturity(null);
            item.setFinalMaturity(null);
            item.setMaturityGatesJson(null);
            item.setScore(null);
            item.setReviewStatus(GapItemReviewStatus.OPEN);
        } else if (protectContributorInput) {
            if (req.implementationStatus() != null || req.maturity() != null
                    || req.maturityGates() != null
                    || req.observations() != null || req.gapDescription() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Contributor input is preserved: use Finalize or Send back for Admin review");
            }
        } else {
            boolean sama = isSamaAssessment(a);
            if (sama) {
                applySamaMaturityInput(item, req.maturityGates(), req.maturity());
                item.setImplementationStatus(null);
            } else if (req.implementationStatus() != null && !contributorOnly) {
                item.setImplementationStatus(req.implementationStatus());
            }
            if (!contributorOnly && req.observations() != null) {
                item.setObservations(blankToNull(req.observations()));
            }
            if (!contributorOnly && req.gapDescription() != null) {
                item.setGapDescription(blankToNull(req.gapDescription()));
            }
            // A partial draft can be saved. An unassigned control becomes Final only when the
            // admin has both a classification and a Gap description. Send checks the rest.
            // UAE IA: that lock is only for Fully Implemented. Planned, Partial, and Not
            // Implemented stay scored and open.
            if (elevated && !assignedToContributor) {
                if (sama && item.getMaturity() != null) {
                    item.setFinalMaturity(item.getMaturity());
                    item.setReviewStatus(GapItemReviewStatus.FINALIZED);
                    item.setAdminReviewedAt(Instant.now());
                    item.setAdminReviewedByUserId(user.getId());
                } else if (!sama && item.getImplementationStatus() != null
                        && item.getObservations() != null && !item.getObservations().isBlank()
                        && uaeIaFinalStatusAllowed(a, item.getImplementationStatus())) {
                    item.setFinalImplementationStatus(item.getImplementationStatus());
                    item.setReviewStatus(GapItemReviewStatus.FINALIZED);
                    item.setAdminReviewedAt(Instant.now());
                    item.setAdminReviewedByUserId(user.getId());
                } else if (!sama && isUaeIa(a)
                        && item.getImplementationStatus() != null
                        && item.getImplementationStatus() != GapImplementationStatus.FULLY_IMPLEMENTED
                        && item.getReviewStatus() == GapItemReviewStatus.FINALIZED) {
                    item.setFinalImplementationStatus(null);
                    item.setReviewStatus(GapItemReviewStatus.OPEN);
                    item.setAdminReviewedAt(null);
                    item.setAdminReviewedByUserId(null);
                }
            }
            syncScore(item, a);
        }

        item.setUpdatedByUserId(user.getId());
        itemRepository.save(item);

        if (a.getStatus() == GapAssessmentStatus.DRAFT) {
            a.setStatus(GapAssessmentStatus.IN_PROGRESS);
        }
        boolean scoreChanged = scoreApplicability != item.getApplicability()
                || scoreImplementation != item.getImplementationStatus()
                || !Objects.equals(scoreMaturity, item.getMaturity())
                || !Objects.equals(scoreGates, item.getMaturityGatesJson())
                || scoreReview != item.getReviewStatus();
        if (scoreChanged) {
            refreshScores(a);
        }
        assessmentRepository.save(a);

        if (contributorOnly && item.getWorkTaskId() != null) {
            workTaskRepository.findById(item.getWorkTaskId()).ifPresent(task -> {
                if (task.getStatus() != WorkTaskStatus.SUBMITTED && task.getStatus() != WorkTaskStatus.CANCELLED) {
                    task.setStatus(WorkTaskStatus.IN_PROGRESS);
                    workTaskRepository.save(task);
                }
            });
        }

        audit(user, AuditActionType.UPDATED, "GAP_ITEM", String.valueOf(item.getId()),
                item.getNodeCode(), "implementationStatus",
                null,
                item.getImplementationStatus() != null ? item.getImplementationStatus().name() : null);
        return respondSaved(a, item);
    }

    @Transactional
    public GapItemResponse finalizeItem(User admin, long assessmentId, long itemId, FinalizeGapItemRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        GapItem item = itemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        if (item.getApplicability() == GapApplicability.NOT_APPLICABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot finalize N/A controls");
        }

        String oldFinal = item.getFinalImplementationStatus() != null
                ? item.getFinalImplementationStatus().name()
                : (item.getFinalMaturity() != null ? "M" + item.getFinalMaturity() : null);
        boolean sama = isSamaAssessment(a);
        if (sama) {
            if (req.finalMaturity() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "finalMaturity (0–5) is required to finalize SAMA controls");
            }
            try {
                SamaGapScoring.requireValid(req.finalMaturity(), "finalMaturity");
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            item.setFinalMaturity(req.finalMaturity());
            item.setFinalImplementationStatus(null);
            if (item.getAssignedToUserId() == null && item.getMaturity() == null) {
                item.setMaturity(req.finalMaturity());
            }
        } else {
            if (req.finalImplementationStatus() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "finalImplementationStatus is required to finalize");
            }
            if (!uaeIaFinalStatusAllowed(a, req.finalImplementationStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Final status can only be set when the control is Fully Implemented");
            }
            item.setFinalImplementationStatus(req.finalImplementationStatus());
            item.setFinalMaturity(null);
            // Unassigned Admin self-fill: also keep contributor fields aligned.
            if (item.getAssignedToUserId() == null && item.getImplementationStatus() == null) {
                item.setImplementationStatus(req.finalImplementationStatus());
            }
        }
        item.setAdminReviewNotes(blankToNull(req.adminReviewNotes()));
        item.setSendBackReason(null);
        item.setFlaggedForLater(false);
        item.setReviewStatus(GapItemReviewStatus.FINALIZED);
        item.setAdminReviewedAt(Instant.now());
        item.setAdminReviewedByUserId(admin.getId());
        syncScore(item, a);
        item.setUpdatedByUserId(admin.getId());
        itemRepository.save(item);

        if (a.getStatus() == GapAssessmentStatus.DRAFT) {
            a.setStatus(GapAssessmentStatus.IN_PROGRESS);
        }
        refreshScores(a);
        assessmentRepository.save(a);

        if (item.getAssignedToUserId() != null) {
            Map<String, String> notice = gapControlNotice(a, item);
            notice.put("finalStatus", sama
                    ? "M" + item.getFinalMaturity()
                    : req.finalImplementationStatus().name());
            notificationService.trigger(
                    "GAP_CONTROL_FINALIZED",
                    String.valueOf(item.getId()),
                    List.of(item.getAssignedToUserId().toString()),
                    notice);
        }

        audit(admin, AuditActionType.UPDATED, "GAP_ITEM", String.valueOf(item.getId()),
                item.getNodeCode(), sama ? "finalMaturity" : "finalImplementationStatus", oldFinal,
                sama ? String.valueOf(item.getFinalMaturity()) : req.finalImplementationStatus().name());

        if (!sama && req.finalImplementationStatus() != null) {
            List<Long> dualCreditAssessments = crossFrameworkDualCreditService.applyOnFinalize(
                    admin, a, item, req.finalImplementationStatus());
            for (Long peerId : dualCreditAssessments) {
                assessmentRepository.findById(peerId).ifPresent(peer -> {
                    refreshScores(peer);
                    assessmentRepository.save(peer);
                });
            }
        }

        return respondSaved(a, item);
    }

    @Transactional
    public GapItemResponse sendBackItem(User admin, long assessmentId, long itemId, SendBackGapItemRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        GapItem item = itemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        if (item.getAssignedToUserId() == null || item.getWorkTaskId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Send back requires an assigned Contributor task");
        }
        if (item.getReviewStatus() == GapItemReviewStatus.FINALIZED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Final status is already set");
        }
        if (item.getReviewStatus() != GapItemReviewStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Control must be submitted before send-back");
        }

        String reason = req.reason().trim();
        if (!isSamaAssessment(a)) {
            if (req.implementationStatus() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "implementationStatus is required when sending back");
            }
            item.setImplementationStatus(req.implementationStatus());
        }
        item.setSendBackReason(reason);
        item.setFinalImplementationStatus(null);
        item.setFinalMaturity(null);
        item.setAdminReviewNotes(null);
        item.setReviewStatus(GapItemReviewStatus.SENT_BACK);
        item.setAdminReviewedAt(Instant.now());
        item.setAdminReviewedByUserId(admin.getId());
        syncScore(item, a);
        item.setUpdatedByUserId(admin.getId());
        itemRepository.save(item);

        WorkTask task = workTaskRepository.findById(item.getWorkTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work task not found"));
        task.setStatus(WorkTaskStatus.PENDING);
        task.setSubmittedAt(null);
        workTaskRepository.save(task);

        refreshScores(a);
        assessmentRepository.save(a);

        notifySentBack(a, List.of(item), reason);

        audit(admin, AuditActionType.UPDATED, "GAP_ITEM", String.valueOf(item.getId()),
                item.getNodeCode(), "reviewStatus", "SUBMITTED", "SENT_BACK");
        return respondSaved(a, item);
    }

    /**
     * Called from My Tasks on Contributor submit: writes immutable snapshot and marks SUBMITTED.
     */
    @Transactional
    public void recordContributorSubmission(User user, GapItem item, Long workTaskId) {
        int next = submissionRepository.maxSubmissionNumber(item.getId()) + 1;
        GapItemSubmission snap = new GapItemSubmission();
        snap.setGapItemId(item.getId());
        snap.setSubmissionNumber(next);
        snap.setImplementationStatus(item.getImplementationStatus());
        snap.setMaturity(item.getMaturity());
        snap.setMaturityGatesJson(item.getMaturityGatesJson());
        snap.setObservations(item.getObservations());
        snap.setGapDescription(item.getGapDescription());
        snap.setContributorComments(item.getContributorComments());
        GapAssessment assessment = assessmentRepository.findById(item.getGapAssessmentId()).orElse(null);
        if (assessment != null && isSamaAssessment(assessment)) {
            snap.setScore(SamaGapScoring.scoreOf(item.getMaturity()));
        } else {
            snap.setScore(GapScoring.scoreOf(item.getImplementationStatus()));
        }
        snap.setSubmittedByUserId(user.getId());
        snap.setSubmittedAt(Instant.now());
        snap.setWorkTaskId(workTaskId);
        submissionRepository.save(snap);

        item.setReviewStatus(GapItemReviewStatus.SUBMITTED);
        item.setContributorSubmittedAt(snap.getSubmittedAt());
        item.setSendBackReason(null);
        item.setFinalImplementationStatus(null);
        item.setFinalMaturity(null);
        item.setAdminReviewNotes(null);
        item.setAdminReviewedAt(null);
        item.setAdminReviewedByUserId(null);
        if (assessment != null) {
            syncScore(item, assessment);
        } else {
            item.setScore(GapScoring.scoreOf(item.getImplementationStatus()));
        }
        itemRepository.save(item);

        audit(user, AuditActionType.UPDATED, "GAP_ITEM", String.valueOf(item.getId()),
                item.getNodeCode(), "reviewStatus", null, "SUBMITTED");

        assessmentRepository.findById(item.getGapAssessmentId()).ifPresent(a -> {
            Map<String, String> notice = gapControlNotice(a, item);
            notice.put("status", isSamaAssessment(a)
                    ? (item.getMaturity() != null ? "M" + item.getMaturity() : "-")
                    : (item.getImplementationStatus() != null
                    ? item.getImplementationStatus().name() : "-"));
            notificationService.trigger(
                    "GAP_CONTROL_SUBMITTED",
                    String.valueOf(item.getId()),
                    List.of("ADMIN", "OWNER"),
                    notice);
        });
    }

    /** Recalculate overall / layer scores after external Gap item mutations (e.g. My Tasks submit). */
    @Transactional
    public void refreshAssessmentScores(long assessmentId) {
        GapAssessment a = require(assessmentId);
        refreshScores(a);
        assessmentRepository.save(a);
    }

    @Transactional
    public GapItemResponse assignItem(User admin, long assessmentId, long itemId, AssignGapItemRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        GapItem item = itemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        if (item.getApplicability() == GapApplicability.NOT_APPLICABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign N/A controls");
        }
        if (req.userId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contributor is required");
        }
        boolean hold = Boolean.FALSE.equals(req.release());
        boolean wasReleased = item.isReleasedToContributor();
        // A held draft may omit the due date. Send, and an assign that releases now, still require it.
        if (req.dueDate() == null && !(hold && !wasReleased)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date is required");
        }
        User assignee = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        if (!assignee.getRoles().contains(Role.CONTRIBUTOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must have CONTRIBUTOR role");
        }
        if (req.dueDate() != null) {
            DateRules.requireActionDueDateIfPresent(req.dueDate(), "Due date");
            DateRules.requireOnOrBefore(
                    req.dueDate(),
                    a.getTargetDate(),
                    "Due date cannot be later than the Gap Assessment target date");
        }

        if (!isSamaAssessment(a) && req.implementationStatus() != null) {
            item.setImplementationStatus(req.implementationStatus());
            syncScore(item, a);
        }

        // Re-open only when the contributor changes. Saving the same assignee must
        // keep a submitted control on Set final status / Send back.
        Long previousAssignee = item.getAssignedToUserId();
        boolean assigneeChanged = previousAssignee == null || !previousAssignee.equals(assignee.getId());
        boolean reopenForNewAssignee = assigneeChanged
                && (item.getReviewStatus() == GapItemReviewStatus.SUBMITTED
                || (item.getReviewStatus() == GapItemReviewStatus.FINALIZED
                && item.getContributorSubmittedAt() != null));

        GapQuestionAssignmentSupport.apply(
                item, req.assignAllQuestions(), req.frameworkQuestionIds(), req.customQuestionIds());
        if (item.getQuestionAssignmentMode() == GapQuestionAssignmentMode.SELECTED) {
            validateAssignedQuestionIds(a, item,
                    GapQuestionAssignmentSupport.parseIds(item.getAssignedFrameworkQuestionIdsJson()),
                    GapQuestionAssignmentSupport.parseIds(item.getAssignedCustomQuestionIdsJson()));
        }

        item.setPendingDueDate(req.dueDate());
        item.setAssignedToUserId(assignee.getId());
        if (reopenForNewAssignee
                || (item.getReviewStatus() == GapItemReviewStatus.FINALIZED
                && (item.getFinalImplementationStatus() != null || item.getFinalMaturity() != null)
                && item.getContributorSubmittedAt() == null)) {
            // Re-open for (new) Contributor input; clear prior submission / final review fields.
            item.setReviewStatus(GapItemReviewStatus.OPEN);
            item.setContributorSubmittedAt(null);
            item.setFinalImplementationStatus(null);
            item.setFinalMaturity(null);
            item.setAdminReviewNotes(null);
            syncScore(item, a);
        }

        // Save stores the contributor. Send is what the contributor sees, unless the caller
        // still uses the older release-immediately assign.
        if (hold && !wasReleased) {
            itemRepository.save(item);
            markAssessmentInProgress(a);
            audit(admin, AuditActionType.UPDATED, "GAP_ITEM", String.valueOf(item.getId()),
                    item.getNodeCode(), "assignedTo", null, assignee.getEmail());
            return respondSaved(a, item);
        }

        WorkTask task = openContributorTask(admin, a, item, assignee, req.dueDate(), assigneeChanged);
        item.setWorkTaskId(task.getId());
        item.setReleasedToContributor(true);
        itemRepository.save(item);
        markAssessmentInProgress(a);

        if (!hold) {
            notificationService.trigger(
                    "GAP_CONTROL_ASSIGNED",
                    String.valueOf(item.getId()),
                    List.of(assignee.getId().toString()),
                    gapControlNotice(a, item));
        } else if (assigneeChanged) {
            notifyReleased(a, List.of(item));
        }

        audit(admin, AuditActionType.UPDATED, "GAP_ITEM", String.valueOf(item.getId()),
                item.getNodeCode(), "assignedTo", null, assignee.getEmail());
        return respondSaved(a, item);
    }

    @Transactional
    public List<GapItemResponse> sendItems(User admin, long assessmentId, SendGapItemsRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        List<GapItem> items = loadSendSelection(assessmentId, req.itemIds());
        boolean sama = isSamaAssessment(a);
        List<String> blocked = new ArrayList<>();
        for (GapItem item : items) {
            String reason = releaseBlockReason(item, sama);
            if (reason != null) {
                blocked.add(reason);
            }
        }
        if (!blocked.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot send. " + String.join(". ", blocked));
        }
        for (GapItem item : items) {
            User assignee = userRepository.findById(item.getAssignedToUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            LocalDate due = item.getPendingDueDate();
            WorkTask task = openContributorTask(admin, a, item, assignee, due, true);
            item.setWorkTaskId(task.getId());
            item.setReleasedToContributor(true);
            item.setFlaggedForLater(false);
            item.setUpdatedByUserId(admin.getId());
            itemRepository.save(item);
            audit(admin, AuditActionType.UPDATED, "GAP_ITEM", String.valueOf(item.getId()),
                    item.getNodeCode(), "releasedToContributor", "false", "true");
        }
        markAssessmentInProgress(a);
        notifyReleased(a, items);
        return items.stream().map(item -> respondSaved(a, item)).toList();
    }

    @Transactional
    public List<GapItemResponse> sendBackItems(User admin, long assessmentId, SendBackGapItemsRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        String reason = req.reason().trim();
        if (reason.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send-back reason is required");
        }
        List<GapItem> items = loadSendSelection(assessmentId, req.itemIds());
        boolean sama = isSamaAssessment(a);
        List<String> blocked = new ArrayList<>();
        for (GapItem item : items) {
            String block = sendBackBlockReason(item, sama);
            if (block != null) {
                blocked.add(block);
            }
        }
        if (!blocked.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot send back. " + String.join(". ", blocked));
        }
        for (GapItem item : items) {
            applySendBack(admin, a, item, reason, sama ? null : item.getImplementationStatus());
        }
        refreshScores(a);
        assessmentRepository.save(a);
        notifySentBack(a, items, reason);
        return items.stream().map(item -> respondSaved(a, item)).toList();
    }

    @Transactional
    public GapItemResponse setFlag(User admin, long assessmentId, long itemId, SetGapFlagRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        GapItem item = itemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        if (item.getReviewStatus() == GapItemReviewStatus.FINALIZED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Flag is not available after final status is set");
        }
        item.setFlaggedForLater(req.flagged());
        item.setUpdatedByUserId(admin.getId());
        itemRepository.save(item);
        return respondSaved(a, item);
    }

    @Transactional
    public GapAssessmentDetailResponse complete(User user, long id) {
        GapAssessment a = require(id);
        assertFrameworkWritable(a.getFrameworkId());
        if (a.getStatus() == GapAssessmentStatus.COMPLETED) {
            return get(user, id);
        }
        List<GapItem> items = itemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(id);
        boolean sama = isSamaAssessment(a);
        List<String> missing = items.stream()
                .filter(i -> i.getApplicability() == GapApplicability.APPLICABLE
                        && (sama ? i.getFinalMaturity() == null : i.getFinalImplementationStatus() == null))
                .map(GapItem::getNodeCode)
                .limit(10)
                .toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    sama
                            ? "Finalize maturity on all applicable controls before complete. Examples: "
                                    + String.join(", ", missing)
                            : "Finalize implementation status on all applicable controls before complete. Examples: "
                                    + String.join(", ", missing));
        }
        List<String> pendingReview = items.stream()
                .filter(i -> i.getApplicability() == GapApplicability.APPLICABLE
                        && i.getReviewStatus() == GapItemReviewStatus.SUBMITTED)
                .map(GapItem::getNodeCode)
                .limit(10)
                .toList();
        if (!pendingReview.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Resolve submitted controls (Finalize or Send back) first. Examples: "
                            + String.join(", ", pendingReview));
        }
        refreshScores(a);
        a.setStatus(GapAssessmentStatus.COMPLETED);
        a.setCompletedAt(Instant.now());
        a.setCompletedByUserId(user.getId());
        assessmentRepository.save(a);
        notifyGapCompleted(a, items);
        notifyGapAssessor(a, "GAP_ASSESSMENT_COMPLETED_ASSESSOR");
        audit(user, AuditActionType.UPDATED, "GAP_ASSESSMENT", String.valueOf(a.getId()),
                a.getName(), "status", "IN_PROGRESS", "COMPLETED");
        return get(user, id);
    }

    private void notifyGapCompleted(GapAssessment a, List<GapItem> items) {
        Set<Long> contributorIds = items.stream()
                .map(GapItem::getAssignedToUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<String> recipients = contributorIds.stream()
                .map(String::valueOf)
                .toList();
        if (recipients.isEmpty()) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("assessmentName", a.getName());
        data.put("frameworkCode", a.getFrameworkCode());
        data.put("overallScore", a.getOverallScore() != null ? a.getOverallScore().toPlainString() + "%" : "-");
        notificationService.trigger("GAP_ASSESSMENT_COMPLETED", String.valueOf(a.getId()), recipients, data);
    }

    @Transactional
    public GapAssessmentDetailResponse reopen(User user, long id) {
        GapAssessment a = require(id);
        assertFrameworkWritable(a.getFrameworkId());
        if (a.getStatus() != GapAssessmentStatus.COMPLETED) {
            return get(user, id);
        }
        a.setStatus(GapAssessmentStatus.IN_PROGRESS);
        a.setCompletedAt(null);
        a.setCompletedByUserId(null);
        assessmentRepository.save(a);
        audit(user, AuditActionType.UPDATED, "GAP_ASSESSMENT", String.valueOf(a.getId()),
                a.getName(), "status", "COMPLETED", "IN_PROGRESS");
        return get(user, id);
    }

    /** Raise CAPA Register 1 from a gap item (Path C A2). Requires AUDIT or ISMS module for CAPA API. */
    @Transactional
    public CapaResponse raiseCapa(User admin, long assessmentId, long itemId) {
        GapAssessment a = require(assessmentId);
        assertFrameworkWritable(a.getFrameworkId());
        GapItem item = requireItem(assessmentId, itemId);
        assertContributorSubmittedForRaise(item);
        return capaService.createFromGapFinding(admin, a, item);
    }

    /** G-08/G-10: Raise CAPA/Risk only after Contributor submission. */
    public void assertContributorSubmittedForRaise(long assessmentId, long itemId) {
        assertContributorSubmittedForRaise(requireItem(assessmentId, itemId));
    }

    private void assertContributorSubmittedForRaise(GapItem item) {
        boolean submitted = item.getContributorSubmittedAt() != null
                || item.getReviewStatus() == GapItemReviewStatus.SUBMITTED
                || (item.getReviewStatus() == GapItemReviewStatus.FINALIZED
                && item.getContributorSubmittedAt() != null);
        if (!submitted) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Raise CAPA / Risk is available after Contributor has submitted the control");
        }
    }

    /** Contributor submit: every assigned question needs an assessment and a response. */
    @Transactional(readOnly = true)
    public void requireContributorAnswers(User user, long assessmentId, long itemId) {
        List<FrameworkQuestionResponse> questions = listQuestionsForItem(user, assessmentId, itemId);
        List<GapCustomQuestionResponse> custom = listCustomQuestions(user, assessmentId, itemId);
        if (questions.isEmpty() && custom.isEmpty()) {
            return;
        }
        List<GapItemAnswer> answers = answerRepository.findByGapItemId(itemId);
        for (FrameworkQuestionResponse q : questions) {
            GapItemAnswer row = answers.stream()
                    .filter(a -> q.id().equals(a.getFrameworkQuestionId()))
                    .findFirst()
                    .orElse(null);
            requireQuestionResponse(row);
        }
        for (GapCustomQuestionResponse q : custom) {
            GapItemAnswer row = answers.stream()
                    .filter(a -> q.id().equals(a.getCustomQuestionId()))
                    .findFirst()
                    .orElse(null);
            requireQuestionResponse(row);
        }
    }

    private static void requireQuestionResponse(GapItemAnswer row) {
        if (row == null || row.getAnswer() == null || row.getAnswer().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer is required for each question");
        }
        if (row.getNote() == null || row.getNote().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your response is required for each question");
        }
    }

    @Transactional(readOnly = true)
    public List<FrameworkQuestionResponse> listQuestionsForItem(User user, long assessmentId, long itemId) {
        GapAssessment a = require(assessmentId);
        GapItem item = requireItem(assessmentId, itemId);
        contributorAccess.assertCanAccessGapItem(user, itemId);
        List<FrameworkQuestionResponse> all =
                frameworkQuestionService.listActiveForNode(a.getFrameworkId(), item.getFrameworkNodeId());
        if (RoleAccess.isElevated(user.getRoles())
                || GapQuestionAssignmentSupport.modeOf(item) == GapQuestionAssignmentMode.ALL) {
            return all;
        }
        var allowed = new java.util.HashSet<>(
                GapQuestionAssignmentSupport.parseIds(item.getAssignedFrameworkQuestionIdsJson()));
        return all.stream().filter(q -> allowed.contains(q.id())).toList();
    }

    @Transactional(readOnly = true)
    public List<GapCustomQuestionResponse> listCustomQuestions(User user, long assessmentId, long itemId) {
        GapItem item = requireItem(assessmentId, itemId);
        contributorAccess.assertCanAccessGapItem(user, itemId);
        List<GapCustomQuestionResponse> all = customQuestionRepository
                .findByGapItemIdOrderBySortOrderAscIdAsc(itemId).stream()
                .map(q -> new GapCustomQuestionResponse(q.getId(), q.getQuestionText(), q.getSortOrder()))
                .toList();
        if (RoleAccess.isElevated(user.getRoles())
                || GapQuestionAssignmentSupport.modeOf(item) == GapQuestionAssignmentMode.ALL) {
            return all;
        }
        var allowed = new java.util.HashSet<>(
                GapQuestionAssignmentSupport.parseIds(item.getAssignedCustomQuestionIdsJson()));
        return all.stream().filter(q -> allowed.contains(q.id())).toList();
    }

    @Transactional
    public GapCustomQuestionResponse addCustomQuestion(User admin, long assessmentId, long itemId,
                                                       CreateGapCustomQuestionRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        if (admin.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        GapItem item = requireItem(assessmentId, itemId);
        int sort = customQuestionRepository.findByGapItemIdOrderBySortOrderAscIdAsc(itemId).size();
        GapItemCustomQuestion q = new GapItemCustomQuestion();
        q.setGapItemId(itemId);
        q.setQuestionText(req.questionText().trim());
        q.setSortOrder(sort);
        q.setCreatedByUserId(admin.getId());
        customQuestionRepository.save(q);
        audit(admin, AuditActionType.CREATED, "GAP_CUSTOM_QUESTION", String.valueOf(q.getId()),
                item.getNodeCode(), null, null, req.questionText().trim());
        return new GapCustomQuestionResponse(q.getId(), q.getQuestionText(), q.getSortOrder());
    }

    @Transactional(readOnly = true)
    public List<GapQuestionAnswerResponse> listAnswers(User user, long assessmentId, long itemId) {
        requireItem(assessmentId, itemId);
        contributorAccess.assertCanAccessGapItem(user, itemId);
        Map<Long, FrameworkQuestion> qById = new HashMap<>();
        return answerRepository.findByGapItemId(itemId).stream()
                .map(ans -> toAnswerResponse(ans, qById))
                .toList();
    }

    @Transactional
    public List<GapQuestionAnswerResponse> upsertAnswers(User user, long assessmentId, long itemId,
                                                         UpsertGapAnswersRequest req) {
        GapAssessment a = require(assessmentId);
        assertEditable(a);
        GapItem item = requireItem(assessmentId, itemId);
        contributorAccess.assertCanAccessGapItem(user, itemId);
        if (req.answers() == null || req.answers().isEmpty()) {
            return listAnswers(user, assessmentId, itemId);
        }
        for (UpsertGapAnswersRequest.Answer row : req.answers()) {
            try {
                GapContributorText.requireAllowed(row.note());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            if (row.customQuestionId() != null) {
                if (!RoleAccess.isElevated(user.getRoles())
                        && !GapQuestionAssignmentSupport.allowsCustomQuestion(item, row.customQuestionId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Custom question is not assigned to this Contributor");
                }
                GapItemCustomQuestion cq = customQuestionRepository.findById(row.customQuestionId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown custom question"));
                if (!cq.getGapItemId().equals(itemId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom question does not belong to this control");
                }
                GapItemAnswer answer = answerRepository
                        .findByGapItemIdAndCustomQuestionId(itemId, cq.getId())
                        .orElseGet(GapItemAnswer::new);
                answer.setGapItemId(itemId);
                answer.setCustomQuestionId(cq.getId());
                answer.setFrameworkQuestionId(null);
                answer.setQuestionVersion(1);
                answer.setAnswer(blankToNull(row.answer()));
                answer.setNote(blankToNull(row.note()));
                answer.setUpdatedByUserId(user.getId());
                answerRepository.save(answer);
                continue;
            }
            if (row.questionId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "questionId or customQuestionId required");
            }
            if (!RoleAccess.isElevated(user.getRoles())
                    && !GapQuestionAssignmentSupport.allowsFrameworkQuestion(item, row.questionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question is not assigned to this Contributor");
            }
            FrameworkQuestion q = frameworkQuestionService.findActive(row.questionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown question"));
            if (!q.getFrameworkId().equals(a.getFrameworkId())
                    || !q.getFrameworkNodeId().equals(item.getFrameworkNodeId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to this control");
            }
            GapItemAnswer answer = answerRepository
                    .findByGapItemIdAndFrameworkQuestionId(itemId, q.getId())
                    .orElseGet(GapItemAnswer::new);
            answer.setGapItemId(itemId);
            answer.setFrameworkQuestionId(q.getId());
            answer.setQuestionVersion(q.getVersionNumber());
            answer.setAnswer(blankToNull(row.answer()));
            answer.setNote(blankToNull(row.note()));
            answer.setUpdatedByUserId(user.getId());
            answerRepository.save(answer);
        }
        audit(user, AuditActionType.UPDATED, "GAP_ITEM_ANSWER", String.valueOf(itemId),
                item.getNodeCode(), null, null, "answers_updated");
        return listAnswers(user, assessmentId, itemId);
    }

    private GapQuestionAnswerResponse toAnswerResponse(GapItemAnswer ans, Map<Long, FrameworkQuestion> qById) {
        if (ans.getCustomQuestionId() != null) {
            GapItemCustomQuestion cq = customQuestionRepository.findById(ans.getCustomQuestionId()).orElse(null);
            return new GapQuestionAnswerResponse(
                    null,
                    ans.getCustomQuestionId(),
                    true,
                    "CUSTOM",
                    "YES_NO_PARTIAL_NA",
                    cq != null ? cq.getQuestionText() : "Custom question",
                    null,
                    ans.getQuestionVersion(),
                    ans.getAnswer(),
                    ans.getNote());
        }
        FrameworkQuestion q = qById.computeIfAbsent(ans.getFrameworkQuestionId(),
                id -> frameworkQuestionService.findActive(id).orElse(null));
        return new GapQuestionAnswerResponse(
                ans.getFrameworkQuestionId(),
                null,
                false,
                q != null ? q.getQuestionCode() : null,
                q != null ? q.getQuestionType() : null,
                q != null ? q.getQuestionText() : null,
                q != null ? q.getEvidenceGuidance() : null,
                ans.getQuestionVersion(),
                ans.getAnswer(),
                ans.getNote());
    }

    private GapItem requireItem(long assessmentId, long itemId) {
        GapItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        if (!item.getGapAssessmentId().equals(assessmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found");
        }
        return item;
    }

    private void validateAssignedQuestionIds(GapAssessment a, GapItem item,
                                             List<Long> frameworkQuestionIds, List<Long> customQuestionIds) {
        if (frameworkQuestionIds != null) {
            for (Long qid : frameworkQuestionIds) {
                FrameworkQuestion q = frameworkQuestionService.findActive(qid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Unknown framework question id: " + qid));
                if (!q.getFrameworkId().equals(a.getFrameworkId())
                        || !q.getFrameworkNodeId().equals(item.getFrameworkNodeId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Framework question " + qid + " does not belong to this control");
                }
            }
        }
        if (customQuestionIds != null) {
            for (Long cid : customQuestionIds) {
                GapItemCustomQuestion cq = customQuestionRepository.findById(cid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Unknown custom question id: " + cid));
                if (!cq.getGapItemId().equals(item.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Custom question " + cid + " does not belong to this control");
                }
            }
        }
    }

    private void refreshScores(GapAssessment a) {
        List<GapItem> items = itemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(a.getId());
        for (GapItem item : items) {
            syncScore(item, a);
        }

        if (FrameworkScoringSupport.isSama(a.getFrameworkCode(), a.getFrameworkName())) {
            GapScoring.Result result = SamaGapScoring.maturityAverage(items);
            a.setApplicableCount(result.applicableCount());
            a.setScoredCount(result.scoredCount());
            a.setOverallScore(result.overallScore());
            a.setRagStatus(result.ragStatus());
            var nodes = frameworkQueryService.nodesById(a.getFrameworkId());
            GapLayerScoreSummary layers = SamaGapScoring.compute(items, nodes);
            a.setLayerScoreJson(GapLayerScoreJson.write(layers));
            if (layers.overallUaeIa() != null && layers.overallUaeIa().score() != null) {
                a.setOverallScore(layers.overallUaeIa().score());
                a.setRagStatus(layers.overallUaeIa().rag());
            }
            return;
        }

        GapScoring.Result result = GapScoring.compute(items);
        a.setApplicableCount(result.applicableCount());
        a.setScoredCount(result.scoredCount());
        a.setOverallScore(result.overallScore());
        a.setRagStatus(result.ragStatus());

        if (FrameworkScoringSupport.isUaeIa(a.getFrameworkCode(), a.getFrameworkName())) {
            var nodes = frameworkQueryService.nodesById(a.getFrameworkId());
            GapLayerScoreSummary layers = UaeIaGapScoring.compute(items, nodes);
            a.setLayerScoreJson(GapLayerScoreJson.write(layers));
            if (layers.overallUaeIa() != null && layers.overallUaeIa().score() != null) {
                a.setOverallScore(layers.overallUaeIa().score());
                a.setRagStatus(layers.overallUaeIa().rag());
            }
        } else if (FrameworkScoringSupport.isNcema(a.getFrameworkCode(), a.getFrameworkName())) {
            var nodes = frameworkQueryService.nodesById(a.getFrameworkId());
            GapLayerScoreSummary layers = NcemaGapScoring.compute(items, nodes);
            a.setLayerScoreJson(GapLayerScoreJson.write(layers));
            if (layers.overallUaeIa() != null && layers.overallUaeIa().score() != null) {
                a.setOverallScore(layers.overallUaeIa().score());
                a.setRagStatus(layers.overallUaeIa().rag());
            }
        } else {
            a.setLayerScoreJson(null);
        }
    }

    private GapAssessmentSummaryResponse toSummary(GapAssessment a) {
        int overrides = (int) itemRepository.countByGapAssessmentIdAndApplicabilityOverrideTrue(a.getId());
        return GapAssessmentSummaryResponse.from(a, GapLayerScoreJson.parse(a.getLayerScoreJson()), overrides);
    }

    private GapAssessmentSummaryResponse toSummary(GapAssessment a, List<GapItemResponse> items) {
        int overrides = (int) items.stream().filter(GapItemResponse::applicabilityOverride).count();
        return GapAssessmentSummaryResponse.from(a, GapLayerScoreJson.parse(a.getLayerScoreJson()), overrides);
    }

    private static void syncScore(GapItem item, GapAssessment assessment) {
        Integer next = isSamaAssessment(assessment)
                ? SamaGapScoring.scoreOf(item.effectiveMaturity())
                : GapScoring.scoreOf(item.effectiveImplementationStatus());
        if (!Objects.equals(item.getScore(), next)) {
            item.setScore(next);
        }
    }

    /**
     * Prefer maturityGates (workbook M–Q); derive maturity. Else accept explicit maturity
     * (legacy / admin without gates).
     */
    private static void applySamaMaturityInput(GapItem item, java.util.List<Boolean> gatesList, Integer maturity) {
        if (gatesList != null) {
            Boolean[] gates = SamaMaturityGates.fromList(gatesList);
            item.setMaturityGatesJson(SamaMaturityGates.toJson(gates));
            Integer derived = SamaMaturityGates.maturityFromGates(gates);
            if (derived != null) {
                try {
                    SamaGapScoring.requireValid(derived, "maturity");
                } catch (IllegalArgumentException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
                }
                item.setMaturity(derived);
            } else {
                item.setMaturity(null);
            }
            return;
        }
        if (maturity != null) {
            try {
                SamaGapScoring.requireValid(maturity, "maturity");
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            item.setMaturity(maturity);
        }
    }

    private static boolean isSamaAssessment(GapAssessment a) {
        return FrameworkScoringSupport.isSama(a.getFrameworkCode(), a.getFrameworkName());
    }

    /**
     * Excel / catalog order ({@code displayOrder}), not lexicographic {@code nodeCode}
     * (which puts 10.x before 4.x).
     */
    private List<GapItem> itemsInExcelOrder(long assessmentId,
                                            Map<Long, FrameworkNodeResponse> nodes) {
        List<GapItem> items = itemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(assessmentId);
        return items.stream()
                .sorted(Comparator
                        .comparingInt((GapItem item) -> {
                            FrameworkNodeResponse node = nodes.get(item.getFrameworkNodeId());
                            return node != null ? node.displayOrder() : Integer.MAX_VALUE;
                        })
                        .thenComparing(GapItem::getNodeCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private static boolean visibleToContributor(GapItem item, Long userId) {
        return userId != null && userId.equals(item.getAssignedToUserId()) && item.isReleasedToContributor();
    }

    private void markAssessmentInProgress(GapAssessment a) {
        if (a.getStatus() == GapAssessmentStatus.DRAFT) {
            a.setStatus(GapAssessmentStatus.IN_PROGRESS);
            assessmentRepository.save(a);
        }
    }

    private WorkTask openContributorTask(User admin, GapAssessment a, GapItem item, User assignee,
                                         LocalDate dueDate, boolean resetProgress) {
        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_GAP, MyTasksService.TYPE_GAP_ITEM, item.getId())
                .orElseGet(WorkTask::new);
        task.setSourceModule(MyTasksService.MODULE_GAP);
        task.setSourceRecordType(MyTasksService.TYPE_GAP_ITEM);
        task.setSourceRecordId(item.getId());
        task.setTitle(item.getNodeCode() + ": " + item.getTitle());
        task.setDetail(a.getName() + " · " + a.getFrameworkCode());
        task.setAssignedToUserId(assignee.getId());
        task.setAssignedByUserId(admin.getId());
        task.setDueDate(dueDate);
        if (resetProgress) {
            task.setStatus(WorkTaskStatus.PENDING);
            task.setSubmittedAt(null);
        } else if (task.getStatus() == null) {
            task.setStatus(WorkTaskStatus.PENDING);
        }
        return workTaskRepository.save(task);
    }

    private List<GapItem> loadSendSelection(long assessmentId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one control");
        }
        List<Long> distinct = itemIds.stream().distinct().toList();
        List<GapItem> items = new ArrayList<>();
        for (Long itemId : distinct) {
            if (itemId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one control");
            }
            items.add(itemRepository.findByIdAndGapAssessmentId(itemId, assessmentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found")));
        }
        return items;
    }

    private static String releaseBlockReason(GapItem item, boolean sama) {
        if (item.getApplicability() == GapApplicability.NOT_APPLICABLE) {
            return item.getNodeCode() + " is Not Applicable";
        }
        if (item.isReleasedToContributor()) {
            return item.getNodeCode() + " is already with the contributor";
        }
        if (item.getAssignedToUserId() == null) {
            return item.getNodeCode() + " has no contributor";
        }
        if (item.getPendingDueDate() == null) {
            return item.getNodeCode() + " has no due date";
        }
        if (sama) {
            if (item.getMaturity() == null) {
                return item.getNodeCode() + " has no classification";
            }
        } else if (item.getImplementationStatus() == null) {
            return item.getNodeCode() + " has no classification";
        }
        if (item.getObservations() == null || item.getObservations().isBlank()) {
            return item.getNodeCode() + " has no Gap description";
        }
        return null;
    }

    private static String sendBackBlockReason(GapItem item, boolean sama) {
        if (item.getAssignedToUserId() == null || item.getWorkTaskId() == null || !item.isReleasedToContributor()) {
            return item.getNodeCode() + " has not been sent";
        }
        if (item.getReviewStatus() == GapItemReviewStatus.FINALIZED) {
            return item.getNodeCode() + " is already final";
        }
        if (item.getReviewStatus() != GapItemReviewStatus.SUBMITTED) {
            return item.getNodeCode() + " is not waiting for review";
        }
        if (!sama && item.getImplementationStatus() == null) {
            return item.getNodeCode() + " has no classification";
        }
        return null;
    }

    private void applySendBack(User admin, GapAssessment a, GapItem item, String reason,
                               com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus status) {
        if (status != null) {
            item.setImplementationStatus(status);
        }
        item.setSendBackReason(reason);
        item.setFinalImplementationStatus(null);
        item.setFinalMaturity(null);
        item.setAdminReviewNotes(null);
        item.setReviewStatus(GapItemReviewStatus.SENT_BACK);
        item.setAdminReviewedAt(Instant.now());
        item.setAdminReviewedByUserId(admin.getId());
        syncScore(item, a);
        item.setUpdatedByUserId(admin.getId());
        itemRepository.save(item);
        WorkTask task = workTaskRepository.findById(item.getWorkTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work task not found"));
        task.setStatus(WorkTaskStatus.PENDING);
        task.setSubmittedAt(null);
        workTaskRepository.save(task);
        audit(admin, AuditActionType.UPDATED, "GAP_ITEM", String.valueOf(item.getId()),
                item.getNodeCode(), "reviewStatus", "SUBMITTED", "SENT_BACK");
    }

    private void notifyReleased(GapAssessment a, List<GapItem> items) {
        for (GapItem item : items) {
            if (item.getAssignedToUserId() == null) {
                continue;
            }
            List<String> to = List.of(item.getAssignedToUserId().toString());
            Map<String, String> notice = gapControlNotice(a, item);
            notificationService.triggerEmail(
                    "GAP_CONTROL_ASSIGNED",
                    String.valueOf(item.getId()),
                    to,
                    notice);
            notificationService.triggerInApp(
                    "GAP_CONTROL_ASSIGNED",
                    String.valueOf(item.getId()),
                    to,
                    notice);
        }
    }

    private void notifySentBack(GapAssessment a, List<GapItem> items, String reason) {
        for (GapItem item : items) {
            if (item.getAssignedToUserId() == null) {
                continue;
            }
            List<String> to = List.of(item.getAssignedToUserId().toString());
            Map<String, String> notice = gapControlNotice(a, item);
            notice.put("reason", reason);
            notificationService.triggerEmail(
                    "GAP_CONTROL_SENT_BACK",
                    String.valueOf(item.getId()),
                    to,
                    notice);
            notificationService.triggerInApp(
                    "GAP_CONTROL_SENT_BACK",
                    String.valueOf(item.getId()),
                    to,
                    notice);
        }
    }

    /** Assessor is a stored name. Only an active Admin or Owner can be named, and the role does not change. */
    private User requireAssessor(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assessor is required");
        }
        User assessor = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assessor user not found"));
        boolean administrator = assessor.getRoles().contains(Role.OWNER) || assessor.getRoles().contains(Role.ADMIN);
        if (assessor.getStatus() != UserStatus.ACTIVE || !administrator) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assessor must be an active Admin or Owner");
        }
        return assessor;
    }

    private void notifyGapAssessor(GapAssessment a, String trigger) {
        if (a.getAssessorUserId() == null) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("assessmentName", a.getName() == null || a.getName().isBlank() ? "-" : a.getName());
        data.put("frameworkName", gapFrameworkName(a));
        data.put("frameworkCode", a.getFrameworkCode() == null ? "-" : a.getFrameworkCode());
        data.put("targetDate", a.getTargetDate() != null ? a.getTargetDate().toString() : "-");
        data.put("overallScore", a.getOverallScore() != null ? a.getOverallScore().toPlainString() + "%" : "-");
        notificationService.trigger(trigger, String.valueOf(a.getId()),
                List.of(String.valueOf(a.getAssessorUserId())), data);
    }

    private Map<String, String> gapControlNotice(GapAssessment a, GapItem item) {
        Map<String, String> data = new HashMap<>();
        data.put("frameworkName", gapFrameworkName(a));
        data.put("nodeCode", item.getNodeCode() == null || item.getNodeCode().isBlank() ? "-" : item.getNodeCode());
        data.put("title", item.getTitle() == null || item.getTitle().isBlank() ? "-" : item.getTitle());
        data.put("dueDate", gapDue(item));
        data.put("assessmentName", a.getName() == null || a.getName().isBlank() ? "-" : a.getName());
        return data;
    }

    private static String gapFrameworkName(GapAssessment a) {
        if (a.getFrameworkName() != null && !a.getFrameworkName().isBlank()) {
            return a.getFrameworkName();
        }
        if (a.getFrameworkCode() != null && !a.getFrameworkCode().isBlank()) {
            return a.getFrameworkCode();
        }
        return a.getName() == null || a.getName().isBlank() ? "-" : a.getName();
    }

    private String gapDue(GapItem item) {
        java.time.LocalDate due = item.getPendingDueDate();
        if (due == null && item.getWorkTaskId() != null) {
            due = workTaskRepository.findById(item.getWorkTaskId()).map(WorkTask::getDueDate).orElse(null);
        }
        return due != null ? due.toString() : "-";
    }

    private Set<Long> assignedAssessmentIds(long userId) {
        return itemRepository.findByAssignedToUserId(userId).stream()
                .filter(GapItem::isReleasedToContributor)
                .map(GapItem::getGapAssessmentId)
                .collect(Collectors.toSet());
    }

    private GapAssessment require(long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found"));
    }

    private void assertEditable(GapAssessment a) {
        assertFrameworkWritable(a.getFrameworkId());
        if (a.getStatus() == GapAssessmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assessment is completed: reopen to edit");
        }
    }

    private boolean gapVisible(Long frameworkId) {
        return adminModuleAccessService.mayUseFramework(frameworkId, false);
    }

    private void assertGapVisible(Long frameworkId) {
        if (!gapVisible(frameworkId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found");
        }
    }

    private void assertFrameworkWritable(Long frameworkId) {
        if (frameworkId == null) {
            return;
        }
        String tenantId = TenantContext.getTenantId();
        moduleLicensingService.requireWritableFramework(tenantId, frameworkId);
        if (!adminModuleAccessService.mayUseFramework(frameworkId, true)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this module");
        }
    }

    private void audit(User user, AuditActionType action, String recordType, String recordId,
                       String recordName, String field, String oldVal, String newVal) {
        auditTrailService.record(
                action, "GAP", user.getId(), primaryRole(user),
                recordType, recordId, recordName, field, oldVal, newVal, null, null);
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
}
