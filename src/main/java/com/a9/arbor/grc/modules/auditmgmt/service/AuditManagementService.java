package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.catalog.service.FrameworkLibraryService;
import com.a9.arbor.grc.catalog.service.FrameworkScoringSupport;
import com.a9.arbor.grc.catalog.web.dto.FrameworkDetailResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkSummaryResponse;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditGranteeType;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditMode;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditStandard;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistConclusion;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklist;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementTeamRole;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditScopeModule;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditScopeNode;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditTeamMember;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditOrgUnit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditStatus;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditChecklistItemRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditChecklistRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditFindingRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditOrgUnitRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.auditmgmt.domain.ExternalAccessStatus;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditExternalAccessRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditScopeModuleRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditScopeNodeRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditTeamMemberRepository;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AccessGrantResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditDomainNarrativeResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AddChecklistItemRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditChecklistResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditChecklistItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditDetailResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditFindingResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditSummaryResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateAuditRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateAuditChecklistRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.CreateFindingRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.ReturnAuditRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateAuditScopeRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateAuditSummaryRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateChecklistItemRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateFindingRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.FindingRegisterItemResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditTeamMemberResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateAuditEngagementRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpdateDomainNarrativesRequest;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.gap.service.GapLayerScoreJson;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.domain.CapaRegister;
import com.a9.arbor.grc.modules.capa.service.CapaService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.iam.service.ExternalAuditorUserService;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditManagementService {

    private final GrcAuditRepository auditRepository;
    private final GrcAuditOrgUnitRepository orgUnitLinkRepository;
    private final GrcAuditChecklistItemRepository checklistRepository;
    private final GrcAuditChecklistRepository auditChecklistRepository;
    private final AuditChecklistLibrary checklistLibrary;
    private final GrcAuditFindingRepository findingRepository;
    private final FrameworkLibraryService frameworkLibraryService;
    private final UserRepository userRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final CapaService capaService;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final AuditReportService auditReportService;
    private final AuditExternalAccessService externalAccessService;
    private final GapAssessmentRepository gapAssessmentRepository;
    private final GapItemRepository gapItemRepository;
    private final GrcAuditTeamMemberRepository teamMemberRepository;
    private final GrcAuditScopeNodeRepository scopeNodeRepository;
    private final GrcAuditScopeModuleRepository scopeModuleRepository;
    private final GrcAuditExternalAccessRepository externalAccessRepository;
    private final ExternalAuditorUserService externalAuditorUserService;
    private final AuditAccessGrantService accessGrantService;
    private final AuditRefGenerator auditRefGenerator;
    private final AuditChecklistLibraryService tenantChecklistLibrary;
    private final AuditChecklistLibraryExcel checklistExcel;

    public AuditManagementService(GrcAuditRepository auditRepository,
                                  GrcAuditOrgUnitRepository orgUnitLinkRepository,
                                  GrcAuditChecklistItemRepository checklistRepository,
                                  GrcAuditChecklistRepository auditChecklistRepository,
                                  AuditChecklistLibrary checklistLibrary,
                                  GrcAuditFindingRepository findingRepository,
                                  FrameworkLibraryService frameworkLibraryService,
                                  UserRepository userRepository,
                                  OrgUnitRepository orgUnitRepository,
                                  CapaService capaService,
                                  NotificationService notificationService,
                                  AuditTrailService auditTrailService,
                                  AuditReportService auditReportService,
                                  AuditExternalAccessService externalAccessService,
                                  GapAssessmentRepository gapAssessmentRepository,
                                  GapItemRepository gapItemRepository,
                                  GrcAuditTeamMemberRepository teamMemberRepository,
                                  GrcAuditScopeNodeRepository scopeNodeRepository,
                                  GrcAuditScopeModuleRepository scopeModuleRepository,
                                  GrcAuditExternalAccessRepository externalAccessRepository,
                                  ExternalAuditorUserService externalAuditorUserService,
                                  AuditAccessGrantService accessGrantService,
                                  AuditRefGenerator auditRefGenerator,
                                  AuditChecklistLibraryService tenantChecklistLibrary,
                                  AuditChecklistLibraryExcel checklistExcel) {
        this.auditRepository = auditRepository;
        this.orgUnitLinkRepository = orgUnitLinkRepository;
        this.checklistRepository = checklistRepository;
        this.auditChecklistRepository = auditChecklistRepository;
        this.checklistLibrary = checklistLibrary;
        this.findingRepository = findingRepository;
        this.frameworkLibraryService = frameworkLibraryService;
        this.userRepository = userRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.capaService = capaService;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
        this.auditReportService = auditReportService;
        this.externalAccessService = externalAccessService;
        this.gapAssessmentRepository = gapAssessmentRepository;
        this.gapItemRepository = gapItemRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.scopeNodeRepository = scopeNodeRepository;
        this.scopeModuleRepository = scopeModuleRepository;
        this.externalAccessRepository = externalAccessRepository;
        this.externalAuditorUserService = externalAuditorUserService;
        this.accessGrantService = accessGrantService;
        this.auditRefGenerator = auditRefGenerator;
        this.tenantChecklistLibrary = tenantChecklistLibrary;
        this.checklistExcel = checklistExcel;
    }

    @Transactional(readOnly = true)
    public List<FindingRegisterItemResponse> listOpenFindings(User viewer, FindingSeverity severity) {
        if (isExternalOnly(viewer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Findings register not available");
        }
        Map<Long, GrcAudit> auditsById = auditRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(a -> !a.isLocked())
                .collect(Collectors.toMap(GrcAudit::getId, a -> a, (x, y) -> x));
        return findingRepository.findOpenOnUnlockedAudits().stream()
                .filter(f -> severity == null || f.getSeverity() == severity)
                .filter(f -> auditsById.containsKey(f.getAuditId()))
                .map(f -> FindingRegisterItemResponse.from(f, auditsById.get(f.getAuditId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditSummaryResponse> list(User viewer) {
        List<GrcAudit> rows;
        if (isExternalOnly(viewer)) {
            LinkedHashSet<Long> ids = new LinkedHashSet<>();
            auditRepository.findByLeadAuditorUserIdOrderByUpdatedAtDesc(viewer.getId())
                    .forEach(a -> ids.add(a.getId()));
            externalAccessRepository.findByUserIdAndStatus(viewer.getId(), ExternalAccessStatus.ACTIVE)
                    .forEach(row -> ids.add(row.getAuditId()));
            rows = ids.isEmpty() ? List.of() : auditRepository.findByIdInOrderByUpdatedAtDesc(List.copyOf(ids));
        } else if (isAuditorOnly(viewer)) {
            LinkedHashSet<Long> ids = new LinkedHashSet<>();
            accessGrantService.activeAuditIdsForUser(viewer).forEach(ids::add);
            ids.addAll(teamMemberRepository.findAuditIdsForUser(viewer.getId()));
            auditRepository.findByLeadAuditorUserIdOrderByUpdatedAtDesc(viewer.getId())
                    .forEach(a -> ids.add(a.getId()));
            rows = ids.isEmpty() ? List.of() : auditRepository.findByIdInOrderByUpdatedAtDesc(List.copyOf(ids));
        } else {
            rows = auditRepository.findAllByOrderByUpdatedAtDesc();
        }
        return toSummaries(rows);
    }

    private List<AuditSummaryResponse> toSummaries(List<GrcAudit> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(GrcAudit::getId).toList();
        Map<Long, List<EngagementScopeModule>> modulesByAudit = new HashMap<>();
        for (GrcAuditScopeModule row : scopeModuleRepository.findByAuditIdIn(ids)) {
            modulesByAudit.computeIfAbsent(row.getAuditId(), k -> new ArrayList<>()).add(row.getModuleCode());
        }
        Set<Long> leadIds = rows.stream()
                .map(GrcAudit::getLeadAuditorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> leadNames = leadIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(leadIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getFullName, (x, y) -> x));
        Map<Long, Integer> findingCounts = new HashMap<>();
        for (Object[] row : findingRepository.countGroupedByAuditId(ids)) {
            findingCounts.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return rows.stream()
                .map(a -> AuditSummaryResponse.from(
                        a,
                        modulesByAudit.getOrDefault(a.getId(), List.of()),
                        a.getLeadAuditorUserId() == null ? null : leadNames.get(a.getLeadAuditorUserId()),
                        findingCounts.getOrDefault(a.getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditDetailResponse get(User viewer, long id) {
        GrcAudit a = requireVisible(viewer, id);
        return detail(a, viewer);
    }

    @Transactional
    public AuditDetailResponse create(User actor, CreateAuditRequest req) {
        AuditMode mode = req.auditMode() != null ? req.auditMode() : AuditMode.COMPLIANCE;
        if (!isAdmin(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin or Owner can create an audit");
        }

        String name = req.name().trim();
        if (auditRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An audit named \"" + name + "\" already exists");
        }

        if (req.auditType() == AuditType.EXTERNAL_ASSESSMENT
                && (req.auditorOrganisation() == null || req.auditorOrganisation().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Auditor organisation is required for an external assessment");
        }

        User lead = resolveLeadForCreate(req);
        assertLeadRole(lead);

        AuditStandard standard = req.standard() != null ? req.standard() : inferStandard(req);
        FrameworkDetailResponse controls = null;
        FrameworkSummaryResponse fw = null;
        if (req.frameworkId() != null) {
            String tenantId = TenantContext.getTenantId();
            controls = frameworkLibraryService.getActivatedControls(tenantId, req.frameworkId());
            fw = controls.framework();
            if (mode == AuditMode.ASSESSMENT
                    && !FrameworkScoringSupport.isAssessmentFramework(fw.code(), fw.name())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Assessment mode is only supported for UAE IA and NCEMA frameworks");
            }
        } else if (mode == AuditMode.ASSESSMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assessment audits require a linked framework");
        }
        if (mode == AuditMode.ASSESSMENT && req.gapAssessmentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assessment audits require a linked Gap assessment (gapAssessmentId)");
        }
        GapAssessment linkedGap = null;
        if (req.gapAssessmentId() != null) {
            if (fw == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked Gap requires a framework");
            }
            linkedGap = requireLinkableGap(req.gapAssessmentId(), fw.id());
        }

        GrcAudit a = new GrcAudit();
        a.setDisplayId(auditRefGenerator.nextAuditId());
        a.setName(name);
        a.setAuditType(req.auditType());
        a.setAuditMode(mode);
        a.setAuditStandard(standard);
        if (fw != null) {
            a.setFrameworkId(fw.id());
            a.setFrameworkCode(fw.code());
            a.setFrameworkName(fw.name());
            a.setFrameworkVersion(fw.version());
        } else {
            a.setFrameworkId(null);
            a.setFrameworkCode(standard.name());
            a.setFrameworkName(standard.displayName());
            a.setFrameworkVersion("-");
        }
        a.setStatus(GrcAuditStatus.DRAFT);
        a.setStartDate(req.startDate());
        a.setEndDate(req.endDate());
        a.setScopeNotes(blankToNull(req.scopeNotes()));
        a.setAuditorOrganisation(blankToNull(req.auditorOrganisation()));
        a.setAuditeeOrgUnitId(req.auditeeOrgUnitId() != null
                ? req.auditeeOrgUnitId()
                : firstOrgUnit(req.orgUnitIds()));
        a.setLeadAuditorUserId(lead.getId());
        a.setCreatedByUserId(actor.getId());
        a.setLocked(false);
        if (linkedGap != null) {
            a.setGapAssessmentId(linkedGap.getId());
            a.setDomainNarrativeJson(AuditDomainNarrativeJson.write(
                    initialDomainNarratives(linkedGap)));
        }
        validateEngagementDates(req.startDate(), req.endDate());
        auditRepository.save(a);

        replaceOrgUnits(a.getId(), req.orgUnitIds());
        persistEngagementTeam(a.getId(), req.auditType(), lead, req.teamMemberUserIds());
        saveScopeModules(a.getId(), fw != null ? fw.moduleCode() : "ISMS", req.scopeModules());
        grantInternalTeam(a, lead, req.teamMemberUserIds());
        Set<Long> scopeNodes = normalizeScopeNodeIds(req.scopeNodeIds());
        saveScopeNodes(a.getId(), scopeNodes);
        List<Long> libraryIds = checklistLibraryIds(req);
        if (standard == AuditStandard.MULTIPLE && libraryIds.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Select at least two checklists when Standard is Multiple");
        }
        if (standard != AuditStandard.MULTIPLE && libraryIds.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A single standard uses one checklist");
        }
        if (mode == AuditMode.ASSESSMENT && controls != null) {
            GrcAuditChecklist sheet = newChecklistHeader(a.getId(), ChecklistTemplateKey.CUSTOM,
                    "Controls to audit", 0);
            buildFrameworkChecklist(a, sheet, controls.nodes(), scopeNodes);
        } else if (!libraryIds.isEmpty()) {
            tenantChecklistLibrary.ensureSeeded(actor.getId());
            copyLibraryChecklists(a.getId(), libraryIds);
        } else {
            ChecklistTemplateKey seed = req.checklistTemplate() != null
                    && req.checklistTemplate() != ChecklistTemplateKey.CUSTOM
                    ? req.checklistTemplate()
                    : standard.defaultTemplate();
            seedLibraryChecklist(a.getId(), seed, actor.getId());
        }

        if (linkedGap != null) {
            syncGapScores(a);
        }

        Map<String, String> data = auditData(a);
        notificationService.trigger("AUDIT_PLANNED", String.valueOf(a.getId()),
                List.of(lead.getEmail()), data);
        trail(actor, AuditActionType.CREATED, a);
        return detail(a, actor);
    }

    @Transactional
    public AuditDetailResponse updateEngagement(User actor, long id, UpdateAuditEngagementRequest req) {
        GrcAudit a = require(id);
        if (!isAdmin(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin/Owner can edit engagement setup");
        }
        if (!a.getStatus().setupEditable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Engagement setup can only be edited before fieldwork starts");
        }
        String name = req.name().trim();
        if (auditRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An audit named \"" + name + "\" already exists");
        }
        validateEngagementDates(req.startDate(), req.endDate());

        User lead = userRepository.findById(req.leadAuditorUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lead auditor not found"));
        assertLeadRole(lead);

        boolean hasFindings = !findingRepository.findByAuditIdOrderByDisplayCodeAsc(id).isEmpty();
        Set<Long> scopeNodes = normalizeScopeNodeIds(req.scopeNodeIds());
        if (hasFindings && scopeNodesChanged(a.getId(), scopeNodes)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change control scope after findings exist");
        }

        a.setName(name);
        a.setStartDate(req.startDate());
        a.setEndDate(req.endDate());
        a.setScopeNotes(blankToNull(req.scopeNotes()));
        a.setLeadAuditorUserId(lead.getId());
        if (req.auditeeOrgUnitId() != null) {
            a.setAuditeeOrgUnitId(req.auditeeOrgUnitId());
        } else {
            a.setAuditeeOrgUnitId(firstOrgUnit(req.orgUnitIds()));
        }
        a.setAuditorOrganisation(blankToNull(req.auditorOrganisation()));
        auditRepository.save(a);

        replaceOrgUnits(a.getId(), req.orgUnitIds());
        persistEngagementTeam(a.getId(), a.getAuditType(), lead, req.teamMemberUserIds());
        grantInternalTeam(a, lead, req.teamMemberUserIds());
        saveScopeModules(a.getId(), frameworkModuleCode(a), req.scopeModules());
        accessGrantService.syncModulesFromAudit(a.getId());
        if (!hasFindings) {
            scopeNodeRepository.deleteByAuditId(a.getId());
            saveScopeNodes(a.getId(), scopeNodes);
        }

        trail(actor, AuditActionType.UPDATED, a);
        return detail(a, actor);
    }

    @Transactional
    public AuditDetailResponse start(User actor, long id) {
        GrcAudit a = requireVisible(actor, id);
        assertTeamOrAdmin(actor, a);
        assertNotLocked(a);
        if (a.getStatus() != GrcAuditStatus.DRAFT && a.getStatus() != GrcAuditStatus.ACCESS_GRANTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Audit cannot be started from " + a.getStatus());
        }
        a.setStatus(GrcAuditStatus.IN_PROGRESS);
        a.setReturnReason(null);
        a.setAuditorSignedAt(null);
        a.setAuditorSignedByUserId(null);
        auditRepository.save(a);
        if (a.getAuditMode() == AuditMode.ASSESSMENT && a.getGapAssessmentId() != null) {
            syncGapScores(a);
        }
        trail(actor, AuditActionType.UPDATED, a);
        return detail(a, actor);
    }

    @Transactional
    public AuditDetailResponse syncGapScores(User actor, long id) {
        GrcAudit a = requireVisible(actor, id);
        assertTeamOrAdmin(actor, a);
        assertEditable(a);
        if (a.getAuditMode() != AuditMode.ASSESSMENT || a.getGapAssessmentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Gap score sync is only for assessment audits with a linked Gap assessment");
        }
        syncGapScores(a);
        trail(actor, AuditActionType.UPDATED, a);
        return detail(a, actor);
    }

    @Transactional
    public AuditDetailResponse updateDomainNarratives(User actor, long id,
                                                      UpdateDomainNarrativesRequest req) {
        GrcAudit a = requireVisible(actor, id);
        assertTeamOrAdmin(actor, a);
        assertEditable(a);
        if (a.getAuditMode() != AuditMode.ASSESSMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Domain narratives apply to assessment audits only");
        }
        List<AuditDomainNarrativeResponse> existing =
                AuditDomainNarrativeJson.parse(a.getDomainNarrativeJson());
        List<AuditDomainNarrativeResponse> updates = req.narratives().stream()
                .map(n -> new AuditDomainNarrativeResponse(n.code(), nameForCode(existing, n.code()), n.narrative()))
                .toList();
        a.setDomainNarrativeJson(AuditDomainNarrativeJson.write(
                AuditDomainNarrativeJson.mergeNarratives(existing, updates)));
        auditRepository.save(a);
        return detail(a, actor);
    }

    @Transactional
    public AuditChecklistItemResponse updateChecklist(User actor, long auditId, long itemId,
                                                      UpdateChecklistItemRequest req) {
        GrcAudit a = requireVisible(actor, auditId);
        assertAuditorFieldwork(actor, a);
        assertEditable(a);
        GrcAuditChecklistItem item = checklistRepository.findByIdAndAuditId(itemId, auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist item not found"));
        if (req.assessment() != null
                && a.getAuditMode() == AuditMode.ASSESSMENT
                && a.getGapAssessmentId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Checklist scores are synced from the linked Gap assessment: use Refresh from Gap");
        }
        if (req.assessment() != null) {
            item.setAssessment(req.assessment());
        }
        if (req.notes() != null) {
            item.setNotes(blankToNull(req.notes()));
        }
        if (req.inScope() != null) {
            item.setInScope(req.inScope());
        }
        if (req.evidenceExamined() != null) {
            item.setEvidenceExamined(blankToNull(req.evidenceExamined()));
        }
        if (req.personsInterviewed() != null) {
            item.setPersonsInterviewed(blankToNull(req.personsInterviewed()));
        }
        if (req.reviewedDate() != null) {
            item.setReviewedDate(req.reviewedDate());
        }
        if (req.findingDescription() != null) {
            item.setFindingDescription(blankToNull(req.findingDescription()));
        }
        if (!item.isLibraryLocked()) {
            if (req.clauseRef() != null && !req.clauseRef().isBlank()) {
                item.setNodeCode(trimClause(req.clauseRef()));
            }
            if (req.section() != null) {
                item.setSection(blankToNull(req.section()));
            }
            if (req.title() != null && !req.title().isBlank()) {
                item.setTitle(req.title().trim());
            }
            if (req.testFocus() != null) {
                item.setTestFocus(blankToNull(req.testFocus()));
            }
            if (req.soaApplicability() != null) {
                item.setSoaApplicability(blankToNull(req.soaApplicability()));
            }
            if (req.evidenceModule() != null) {
                item.setEvidenceModule(blankToNull(req.evidenceModule()));
            }
        }
        if (req.conclusion() != null) {
            item.setConclusion(req.conclusion());
            if (item.getReviewedDate() == null) {
                item.setReviewedDate(java.time.LocalDate.now());
            }
        }
        if (severityFor(item.getConclusion()) != null
                && (item.getFindingDescription() == null || item.getFindingDescription().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the finding description");
        }
        String findingCode = syncFindingFromChecklist(actor, a, item);
        item.setUpdatedByUserId(actor.getId());
        checklistRepository.save(item);
        if (a.getStatus() == GrcAuditStatus.DRAFT || a.getStatus() == GrcAuditStatus.ACCESS_GRANTED) {
            a.setStatus(GrcAuditStatus.IN_PROGRESS);
            auditRepository.save(a);
        }
        return AuditChecklistItemResponse.from(item, true, findingCode);
    }

    @Transactional
    public AuditDetailResponse addChecklist(User actor, long auditId, CreateAuditChecklistRequest req) {
        GrcAudit a = requireVisible(actor, auditId);
        assertAuditorFieldwork(actor, a);
        assertEditable(a);
        ChecklistTemplateKey key = req.templateKey() != null ? req.templateKey() : ChecklistTemplateKey.CUSTOM;
        if (key != ChecklistTemplateKey.CUSTOM) {
            if (auditChecklistRepository.existsByAuditIdAndTemplateKey(auditId, key)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        key.defaultName() + " is already on this audit");
            }
            seedLibraryChecklist(auditId, key, actor.getId());
        } else {
            String sheetName = req.name() == null ? "" : req.name().trim();
            if (sheetName.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checklist name is required");
            }
            if (auditChecklistRepository.existsByAuditIdAndNameIgnoreCase(auditId, sheetName)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A checklist named \"" + sheetName + "\" already exists");
            }
            int order = auditChecklistRepository.countByAuditId(auditId);
            newChecklistHeader(auditId, ChecklistTemplateKey.CUSTOM, sheetName, order);
        }
        return detail(a, actor);
    }

    @Transactional
    public AuditDetailResponse uploadNewChecklist(User actor, long auditId, String name, MultipartFile file) {
        GrcAudit a = requireVisible(actor, auditId);
        assertAuditorFieldwork(actor, a);
        assertEditable(a);
        String sheetName = name == null ? "" : name.trim();
        if (sheetName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checklist name is required");
        }
        if (auditChecklistRepository.existsByAuditIdAndNameIgnoreCase(auditId, sheetName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A checklist named \"" + sheetName + "\" already exists");
        }
        List<AuditChecklistLibraryExcel.Parsed> parsed = parseChecklistUpload(file);
        int order = auditChecklistRepository.countByAuditId(auditId);
        GrcAuditChecklist sheet = newChecklistHeader(auditId, ChecklistTemplateKey.CUSTOM, sheetName, order);
        replaceCustomChecklistItems(a, sheet, parsed, actor);
        return detail(a, actor);
    }

    @Transactional
    public AuditDetailResponse uploadChecklist(User actor, long auditId, long checklistId, MultipartFile file) {
        GrcAudit a = requireVisible(actor, auditId);
        assertAuditorFieldwork(actor, a);
        assertEditable(a);
        GrcAuditChecklist sheet = auditChecklistRepository.findByIdAndAuditId(checklistId, auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist not found"));
        List<AuditChecklistLibraryExcel.Parsed> parsed = parseChecklistUpload(file);
        replaceCustomChecklistItems(a, sheet, parsed, actor);
        return detail(a, actor);
    }

    private List<AuditChecklistLibraryExcel.Parsed> parseChecklistUpload(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
        List<AuditChecklistLibraryExcel.Parsed> parsed = checklistExcel.parseTemplate(bytes);
        if (parsed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checklist has no rows");
        }
        return parsed;
    }

    private void replaceCustomChecklistItems(GrcAudit a, GrcAuditChecklist sheet,
                                             List<AuditChecklistLibraryExcel.Parsed> parsed, User actor) {
        if (sheet.getTemplateKey() != ChecklistTemplateKey.CUSTOM) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Upload is for a custom checklist. Add a blank checklist first.");
        }
        List<GrcAuditChecklistItem> existing =
                checklistRepository.findByChecklistIdOrderBySortOrderAscIdAsc(sheet.getId());
        if (existing.stream().anyMatch(item -> item.getFindingId() != null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This checklist has findings. Remove those lines before uploading a replacement");
        }
        checklistRepository.deleteByChecklistId(sheet.getId());
        int order = 0;
        for (AuditChecklistLibraryExcel.Parsed row : parsed) {
            GrcAuditChecklistItem item = new GrcAuditChecklistItem();
            item.setAuditId(a.getId());
            item.setChecklistId(sheet.getId());
            item.setSortOrder(order++);
            item.setFrameworkNodeId(null);
            item.setNodeCode(trimClause(row.clauseRef()));
            item.setSection(blankToNull(row.section()));
            item.setTitle(row.title() == null || row.title().isBlank() ? row.clauseRef() : row.title().trim());
            item.setTestFocus(blankToNull(row.testFocus()));
            item.setSoaApplicability(blankToNull(row.soaApplicability()));
            item.setEvidenceModule(blankToNull(row.evidenceLives()));
            item.setLibraryLocked(false);
            item.setInScope(true);
            item.setUpdatedByUserId(actor.getId());
            checklistRepository.save(item);
        }
        if (a.getStatus() == GrcAuditStatus.DRAFT || a.getStatus() == GrcAuditStatus.ACCESS_GRANTED) {
            a.setStatus(GrcAuditStatus.IN_PROGRESS);
            auditRepository.save(a);
        }
    }

    @Transactional
    public AuditDetailResponse deleteChecklist(User actor, long auditId, long checklistId) {
        GrcAudit a = requireVisible(actor, auditId);
        assertAuditorFieldwork(actor, a);
        assertEditable(a);
        GrcAuditChecklist sheet = auditChecklistRepository.findByIdAndAuditId(checklistId, auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist not found"));
        List<GrcAuditChecklistItem> lines =
                checklistRepository.findByChecklistIdOrderBySortOrderAscIdAsc(sheet.getId());
        for (GrcAuditChecklistItem line : lines) {
            assertChecklistLineRemovable(line);
        }
        checklistRepository.deleteByChecklistId(sheet.getId());
        auditChecklistRepository.delete(sheet);
        return detail(a, actor);
    }

    @Transactional
    public AuditChecklistItemResponse addChecklistItem(User actor, long auditId, long checklistId,
                                                       AddChecklistItemRequest req) {
        GrcAudit a = requireVisible(actor, auditId);
        assertAuditorFieldwork(actor, a);
        assertEditable(a);
        GrcAuditChecklist sheet = auditChecklistRepository.findByIdAndAuditId(checklistId, auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist not found"));
        int order = checklistRepository.countByChecklistId(sheet.getId());
        GrcAuditChecklistItem item = new GrcAuditChecklistItem();
        item.setAuditId(auditId);
        item.setChecklistId(sheet.getId());
        item.setSortOrder(order);
        item.setFrameworkNodeId(null);
        item.setNodeCode(trimClause(req.clauseRef()));
        item.setSection(blankToNull(req.section()));
        item.setTitle(req.title().trim());
        item.setTestFocus(blankToNull(req.testFocus()));
        item.setSoaApplicability(blankToNull(req.soaApplicability()));
        item.setEvidenceModule(blankToNull(req.evidenceModule()));
        item.setLibraryLocked(false);
        item.setInScope(true);
        item.setUpdatedByUserId(actor.getId());
        checklistRepository.save(item);
        if (a.getStatus() == GrcAuditStatus.DRAFT || a.getStatus() == GrcAuditStatus.ACCESS_GRANTED) {
            a.setStatus(GrcAuditStatus.IN_PROGRESS);
            auditRepository.save(a);
        }
        return AuditChecklistItemResponse.from(item);
    }

    @Transactional
    public AuditDetailResponse deleteChecklistItem(User actor, long auditId, long itemId) {
        GrcAudit a = requireVisible(actor, auditId);
        assertAuditorFieldwork(actor, a);
        assertEditable(a);
        GrcAuditChecklistItem item = checklistRepository.findByIdAndAuditId(itemId, auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist item not found"));
        assertChecklistLineRemovable(item);
        checklistRepository.delete(item);
        return detail(a, actor);
    }

    public List<com.a9.arbor.grc.modules.auditmgmt.web.dto.ChecklistTemplateResponse> listTemplates() {
        return checklistLibrary.all().stream()
                .map(com.a9.arbor.grc.modules.auditmgmt.web.dto.ChecklistTemplateResponse::from)
                .toList();
    }

    @Transactional
    public AuditFindingResponse addFinding(User actor, long auditId, CreateFindingRequest req) {
        GrcAudit a = requireVisible(actor, auditId);
        assertTeamOrAdmin(actor, a);
        // Admin may log offline findings after the auditor submits the report; auditors cannot.
        if (isAdmin(actor)) {
            assertNotLocked(a);
            if (a.getStatus() == GrcAuditStatus.CLOSED || a.getStatus() == GrcAuditStatus.CANCELLED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Audit is completed");
            }
        } else {
            assertEditable(a);
        }
        if (a.getAuditMode() == AuditMode.ASSESSMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Findings not used for assessment audits");
        }
        GrcAuditChecklistItem linkedItem = null;
        if (req.checklistItemId() != null) {
            linkedItem = checklistRepository.findByIdAndAuditId(req.checklistItemId(), auditId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checklist item not found"));
            if (linkedItem.getFindingId() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This checklist line already has " + findingDisplayCode(linkedItem.getFindingId()));
            }
        }
        long seq = findingRepository.countByAuditId(auditId) + 1;
        GrcAuditFinding f = new GrcAuditFinding();
        f.setAuditId(auditId);
        f.setChecklistItemId(req.checklistItemId());
        f.setDisplayCode(String.format("FND-%03d", seq));
        f.setSeverity(req.severity());
        f.setTitle(req.title().trim());
        f.setDescription(req.description().trim());
        f.setRecommendedAction(blankToNull(req.recommendedAction()));
        f.setControlRef(blankToNull(req.controlRef()));
        f.setCreatedByUserId(actor.getId());
        findingRepository.save(f);
        if (linkedItem != null) {
            linkedItem.setFindingId(f.getId());
            checklistRepository.save(linkedItem);
        }
        if (a.getStatus() == GrcAuditStatus.DRAFT || a.getStatus() == GrcAuditStatus.ACCESS_GRANTED) {
            a.setStatus(GrcAuditStatus.IN_PROGRESS);
            auditRepository.save(a);
        }
        notificationService.trigger(
                "AUDIT_FINDING_LOGGED",
                String.valueOf(f.getId()),
                List.of("ADMIN", "OWNER"),
                Map.of(
                        "displayCode", f.getDisplayCode(),
                        "severity", f.getSeverity().name(),
                        "auditName", a.getName(),
                        "title", f.getTitle()));
        trail(actor, AuditActionType.CREATED, a);
        return AuditFindingResponse.from(f);
    }

    @Transactional
    public AuditFindingResponse updateFinding(User actor, long auditId, long findingId, UpdateFindingRequest req) {
        GrcAudit a = requireVisible(actor, auditId);
        assertTeamOrAdmin(actor, a);
        assertEditable(a);
        if (a.getAuditMode() == AuditMode.ASSESSMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Findings not used for assessment audits");
        }
        GrcAuditFinding f = findingRepository.findByIdAndAuditId(findingId, auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));
        if (req.severity() != null) {
            f.setSeverity(req.severity());
        }
        if (req.title() != null && !req.title().isBlank()) {
            f.setTitle(req.title().trim());
        }
        if (req.description() != null && !req.description().isBlank()) {
            f.setDescription(req.description().trim());
        }
        if (req.recommendedAction() != null) {
            f.setRecommendedAction(blankToNull(req.recommendedAction()));
        }
        if (req.controlRef() != null) {
            f.setControlRef(blankToNull(req.controlRef()));
        }
        findingRepository.save(f);
        return AuditFindingResponse.from(f);
    }

    @Transactional
    public void deleteFinding(User actor, long auditId, long findingId) {
        GrcAudit a = requireVisible(actor, auditId);
        assertTeamOrAdmin(actor, a);
        assertEditable(a);
        if (a.getAuditMode() == AuditMode.ASSESSMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Findings not used for assessment audits");
        }
        GrcAuditFinding f = findingRepository.findByIdAndAuditId(findingId, auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));
        findingRepository.delete(f);
    }

    @Transactional
    public AuditDetailResponse updateSummary(User actor, long id, UpdateAuditSummaryRequest req) {
        GrcAudit a = requireVisible(actor, id);
        assertTeamOrAdmin(actor, a);
        assertEditable(a);
        a.setExecutiveSummary(blankToNull(req.executiveSummary()));
        auditRepository.save(a);
        return detail(a, actor);
    }

    /**
     * Lead, team, or Admin can change modules in scope and scope notes while fieldwork is open.
     * Does not add or remove checklist controls.
     */
    @Transactional
    public AuditDetailResponse updateScope(User actor, long id, UpdateAuditScopeRequest req) {
        GrcAudit a = requireVisible(actor, id);
        assertTeamOrAdmin(actor, a);
        assertNotLocked(a);
        if (a.getStatus() != GrcAuditStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Scope can be edited while the audit is in progress");
        }
        if (req.scopeModules() == null || req.scopeModules().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one module");
        }
        a.setScopeNotes(blankToNull(req.scopeNotes()));
        auditRepository.save(a);
        saveScopeModules(a.getId(), "ISMS", req.scopeModules());
        accessGrantService.syncModulesFromAudit(a.getId());
        trail(actor, AuditActionType.UPDATED, a);
        return detail(a, actor);
    }

    /** Auditor signs first (authoritative). */
    @Transactional
    public AuditDetailResponse auditorSign(User actor, long id) {
        GrcAudit a = requireVisible(actor, id);
        assertLeadAuditor(actor, a);
        assertNotLocked(a);
        if (a.getStatus() != GrcAuditStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Audit must be in progress to issue the report");
        }
        if (a.getExecutiveSummary() == null || a.getExecutiveSummary().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter the executive summary before submitting the report");
        }
        a.setAuditorSignedAt(Instant.now());
        a.setAuditorSignedByUserId(actor.getId());
        a.setStatus(GrcAuditStatus.REPORT_ISSUED);
        a.setReportIssuedDate(java.time.LocalDate.now());
        a.setReturnReason(null);
        auditRepository.save(a);

        List<String> admins = adminEmails();
        notificationService.trigger("AUDIT_SUBMITTED", String.valueOf(a.getId()), admins, auditData(a));
        trail(actor, AuditActionType.APPROVED, a);
        return detail(a, actor);
    }

    /**
     * Admin closes the engagement after the lead auditor has submitted the report.
     * Findings are optional. CAPA is raised by Admin afterwards.
     */
    @Transactional
    public AuditDetailResponse close(User actor, long id) {
        if (!isAdmin(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an administrator can close the audit. Submit the report as lead auditor first.");
        }
        GrcAudit a = requireVisible(actor, id);
        assertNotLocked(a);
        if (a.getStatus() == GrcAuditStatus.CLOSED || a.getStatus() == GrcAuditStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Audit is already closed");
        }
        if (a.getStatus() != GrcAuditStatus.REPORT_ISSUED || a.getAuditorSignedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The auditor must submit the report before the audit can be closed");
        }
        Instant now = Instant.now();
        if (a.getAdminSignedAt() == null) {
            a.setAdminSignedAt(now);
            a.setAdminSignedByUserId(actor.getId());
        }
        a.setStatus(GrcAuditStatus.CLOSED);
        a.setLocked(true);
        a.setReportIssuedDate(java.time.LocalDate.now());
        a.setReturnReason(null);
        auditRepository.save(a);

        List<String> recipients = new java.util.ArrayList<>(adminEmails());
        userRepository.findById(a.getLeadAuditorUserId()).ifPresent(u -> recipients.add(u.getEmail()));
        Map<String, String> data = auditData(a);
        data.put("findingCount", String.valueOf(findingRepository.countByAuditId(a.getId())));
        notificationService.trigger("AUDIT_COMPLETED", String.valueOf(a.getId()), recipients, data);
        auditReportService.generateOnLock(a);
        trail(actor, AuditActionType.APPROVED, a);
        return detail(a, actor);
    }

    /** Admin signs second: locks report and opens CAPAs (Register 2/3). */
    @Transactional
    public AuditDetailResponse adminSign(User admin, long id) {
        if (!isAdmin(admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        GrcAudit a = require(id);
        assertNotLocked(a);
        if (a.getStatus() != GrcAuditStatus.REPORT_ISSUED || a.getAuditorSignedAt() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report must be issued before Admin close");
        }
        a.setAdminSignedAt(Instant.now());
        a.setAdminSignedByUserId(admin.getId());
        a.setStatus(GrcAuditStatus.CLOSED);
        a.setLocked(true);
        auditRepository.save(a);

        if (a.getAuditMode() != AuditMode.ASSESSMENT) {
            CapaRegister register = a.getAuditType() == AuditType.INTERNAL
                    ? CapaRegister.INTERNAL_AUDIT
                    : CapaRegister.EXTERNAL_AUDIT;
            for (GrcAuditFinding f : findingRepository.findByAuditIdOrderByDisplayCodeAsc(id)) {
                if (f.getSeverity() == FindingSeverity.OFI || f.getCapaId() != null) {
                    continue;
                }
                Capa capa = capaService.createFromFinding(
                        admin, register, f.getId(), f.getTitle(), f.getDescription(),
                        f.getRecommendedAction(), f.getControlRef(), f.getSeverity());
                f.setCapaId(capa.getId());
                findingRepository.save(f);
            }
        }

        List<String> recipients = new java.util.ArrayList<>(adminEmails());
        userRepository.findById(a.getLeadAuditorUserId()).ifPresent(u -> recipients.add(u.getEmail()));
        notificationService.trigger("AUDIT_COMPLETED", String.valueOf(a.getId()), recipients, auditData(a));
        auditReportService.generateOnLock(a);
        trail(admin, AuditActionType.APPROVED, a);
        return detail(a, admin);
    }

    @Transactional
    public AuditDetailResponse returnToAuditor(User admin, long id, ReturnAuditRequest req) {
        if (!isAdmin(admin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        GrcAudit a = require(id);
        assertNotLocked(a);
        if (a.getStatus() != GrcAuditStatus.REPORT_ISSUED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only issued reports can be returned");
        }
        a.setStatus(GrcAuditStatus.IN_PROGRESS);
        a.setReturnReason(req.reason().trim());
        a.setAuditorSignedAt(null);
        a.setAuditorSignedByUserId(null);
        auditRepository.save(a);

        userRepository.findById(a.getLeadAuditorUserId()).ifPresent(u -> {
            Map<String, String> data = auditData(a);
            data.put("reason", req.reason().trim());
            notificationService.trigger("AUDIT_RETURNED", String.valueOf(a.getId()),
                    List.of(u.getEmail()), data);
        });
        trail(admin, AuditActionType.REJECTED, a);
        return detail(a, admin);
    }

    private AuditDetailResponse detail(GrcAudit a, User viewer) {
        List<Long> orgUnits = orgUnitLinkRepository.findByAuditId(a.getId()).stream()
                .map(GrcAuditOrgUnit::getOrgUnitId)
                .toList();
        List<AuditTeamMemberResponse> teamMembers = teamMemberRepository
                .findByAuditIdOrderByTeamRoleAscUserIdAsc(a.getId()).stream()
                .map(AuditTeamMemberResponse::from)
                .toList();
        List<EngagementScopeModule> scopeModules = scopeModuleRepository.findByAuditId(a.getId()).stream()
                .map(GrcAuditScopeModule::getModuleCode)
                .toList();
        String leadAuditorName = a.getLeadAuditorUserId() == null
                ? null
                : userRepository.findById(a.getLeadAuditorUserId()).map(User::getFullName).orElse(null);
        List<Long> scopeNodeIds = scopeNodeRepository.findByAuditId(a.getId()).stream()
                .map(GrcAuditScopeNode::getFrameworkNodeId)
                .toList();
        boolean showWorkingNotes = workingNotesVisible(a, viewer);
        List<GrcAuditChecklist> sheets = auditChecklistRepository.findByAuditIdOrderBySortOrderAscIdAsc(a.getId());
        List<GrcAuditChecklistItem> allItems =
                checklistRepository.findByAuditIdOrderByChecklistIdAscSortOrderAscIdAsc(a.getId());
        List<GrcAuditFinding> findingRows = findingRepository.findByAuditIdOrderByDisplayCodeAsc(a.getId());
        Map<Long, String> findingCodes = new HashMap<>();
        for (GrcAuditFinding f : findingRows) {
            findingCodes.put(f.getId(), f.getDisplayCode());
        }
        Map<Long, List<AuditChecklistItemResponse>> itemsBySheet = new HashMap<>();
        for (GrcAuditChecklistItem item : allItems) {
            itemsBySheet.computeIfAbsent(item.getChecklistId(), k -> new ArrayList<>())
                    .add(toChecklistItem(item, showWorkingNotes, findingCodes));
        }
        List<AuditChecklistResponse> checklists = sheets.stream()
                .map(s -> AuditChecklistResponse.from(s, itemsBySheet.getOrDefault(s.getId(), List.of())))
                .toList();
        List<AuditChecklistItemResponse> checklist = allItems.stream()
                .map(item -> toChecklistItem(item, showWorkingNotes, findingCodes))
                .toList();
        List<AuditFindingResponse> findings = findingRows.stream()
                .map(AuditFindingResponse::from)
                .toList();
        GapLayerScoreSummary gapLayers = null;
        if (a.getGapAssessmentId() != null) {
            gapLayers = gapAssessmentRepository.findById(a.getGapAssessmentId())
                    .map(g -> GapLayerScoreJson.parse(g.getLayerScoreJson()))
                    .orElse(null);
        }
        return new AuditDetailResponse(
                AuditSummaryResponse.from(a, scopeModules, leadAuditorName, findingRows.size()),
                a.getScopeNotes(),
                a.getExecutiveSummary(),
                a.getReturnReason(),
                orgUnits,
                teamMembers,
                scopeModules,
                scopeNodeIds,
                checklists,
                checklist,
                findings,
                a.getGapAssessmentId(),
                gapLayers,
                AuditDomainNarrativeJson.parse(a.getDomainNarrativeJson()),
                accessGrantService.listForAudit(a.getId()));
    }

    private void syncGapScores(GrcAudit audit) {
        Long gapId = audit.getGapAssessmentId();
        if (gapId == null) {
            return;
        }
        List<GapItem> gapItems = gapItemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(gapId);
        List<GrcAuditChecklistItem> checklist =
                checklistRepository.findByAuditIdOrderByNodeCodeAsc(audit.getId());
        AuditGapScoreSync.applyToChecklist(checklist, gapItems);
        checklistRepository.saveAll(checklist);
    }

    private GapAssessment requireLinkableGap(long gapId, long frameworkId) {
        GapAssessment gap = gapAssessmentRepository.findById(gapId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gap assessment not found"));
        if (!Objects.equals(gap.getFrameworkId(), frameworkId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Gap assessment framework must match the audit framework");
        }
        if (gap.getStatus() != GapAssessmentStatus.IN_PROGRESS
                && gap.getStatus() != GapAssessmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Gap assessment must be IN_PROGRESS or COMPLETED to link");
        }
        return gap;
    }

    private static List<AuditDomainNarrativeResponse> initialDomainNarratives(GapAssessment gap) {
        GapLayerScoreSummary layers = GapLayerScoreJson.parse(gap.getLayerScoreJson());
        if (layers == null || layers.domains() == null || layers.domains().isEmpty()) {
            return List.of();
        }
        List<AuditDomainNarrativeResponse> out = new ArrayList<>();
        for (GapLayerScoreSummary.DomainScore d : layers.domains()) {
            out.add(new AuditDomainNarrativeResponse(d.code(), d.name(), ""));
        }
        return out;
    }

    private static String nameForCode(List<AuditDomainNarrativeResponse> existing, String code) {
        if (existing == null) {
            return code;
        }
        return existing.stream()
                .filter(n -> n.code().equals(code))
                .map(AuditDomainNarrativeResponse::name)
                .findFirst()
                .orElse(code);
    }

    private GrcAudit require(long id) {
        return auditRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
    }

    private GrcAudit requireVisible(User viewer, long id) {
        if (isExternalOnly(viewer)) {
            GrcAudit a = auditRepository.findById(id).orElse(null);
            if (a == null
                    || (!viewer.getId().equals(a.getLeadAuditorUserId())
                    && !externalAccessService.hasActiveAccess(viewer.getId(), id))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Scoped to assigned audits only");
            }
            return a;
        }
        GrcAudit a = require(id);
        if (isAdmin(viewer) || viewer.getRoles().contains(Role.GLOBAL_READER)) {
            return a;
        }
        if (assignedOnEngagement(viewer, a)) {
            return a;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this engagement");
    }

    private User resolveLeadForCreate(CreateAuditRequest req) {
        if (req.leadAuditorUserId() != null) {
            User lead = userRepository.findById(req.leadAuditorUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lead auditor not found"));
            if (req.auditType() == AuditType.EXTERNAL
                    && hasExternalLeadInvite(req)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Provide either an existing lead auditor or external lead name and email, not both");
            }
            return lead;
        }
        if (req.auditType() == AuditType.EXTERNAL && hasExternalLeadInvite(req)) {
            return externalAuditorUserService.resolveOrCreate(req.externalLeadEmail(), req.externalLeadName());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                req.auditType() == AuditType.EXTERNAL
                        ? "Lead auditor or external lead name and email is required"
                        : "Lead auditor is required");
    }

    private static boolean hasExternalLeadInvite(CreateAuditRequest req) {
        return req.externalLeadEmail() != null && !req.externalLeadEmail().isBlank()
                && req.externalLeadName() != null && !req.externalLeadName().isBlank();
    }

    private void assertLeadRole(User lead) {
        if (!isAuditor(lead)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Audit lead must be an Auditor");
        }
    }

    private void assertAuditorFieldwork(User actor, GrcAudit a) {
        boolean auditor = actor.getRoles().contains(Role.INTERNAL_AUDITOR)
                || actor.getRoles().contains(Role.EXTERNAL_AUDITOR);
        if (!auditor || isAdmin(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned auditor can edit checklists");
        }
        assertTeamOrAdmin(actor, a);
    }

    private void assertTeamOrAdmin(User actor, GrcAudit a) {
        if (isAdmin(actor) || assignedOnEngagement(actor, a)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Engagement team or Admin required");
    }

    private boolean assignedOnEngagement(User actor, GrcAudit a) {
        if (actor.getId() != null && actor.getId().equals(a.getLeadAuditorUserId())) {
            return true;
        }
        if (accessGrantService.hasActiveAccess(actor, a.getId())) {
            return true;
        }
        if (teamMemberRepository.existsByAuditIdAndUserId(a.getId(), actor.getId())) {
            return true;
        }
        return externalAccessService.hasActiveAccess(actor.getId(), a.getId());
    }

    private void assertLeadAuditor(User actor, GrcAudit a) {
        teamMemberRepository.findByAuditIdAndTeamRole(a.getId(), EngagementTeamRole.LEAD)
                .filter(row -> row.getUserId().equals(actor.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the lead auditor can sign the report"));
    }

    private boolean workingNotesVisible(GrcAudit a, User viewer) {
        if (isAdmin(viewer)) {
            return true;
        }
        if (teamMemberRepository.existsByAuditIdAndUserId(a.getId(), viewer.getId())) {
            return true;
        }
        if (a.getLeadAuditorUserId().equals(viewer.getId())) {
            return true;
        }
        if (externalAccessService.hasActiveAccess(viewer.getId(), a.getId())) {
            return true;
        }
        return a.getStatus().notesVisibleToAdmin()
                || a.isLocked();
    }

    private void validateEngagementDates(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }
    }

    private Set<Long> normalizeScopeNodeIds(List<Long> scopeNodeIds) {
        if (scopeNodeIds == null || scopeNodeIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(scopeNodeIds);
    }

    private void replaceOrgUnits(long auditId, List<Long> orgUnitIds) {
        orgUnitLinkRepository.deleteByAuditId(auditId);
        if (orgUnitIds == null) {
            return;
        }
        for (Long ouId : new LinkedHashSet<>(orgUnitIds)) {
            if (!orgUnitRepository.existsById(ouId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org unit not found: " + ouId);
            }
            GrcAuditOrgUnit link = new GrcAuditOrgUnit();
            link.setAuditId(auditId);
            link.setOrgUnitId(ouId);
            orgUnitLinkRepository.save(link);
        }
    }

    private void persistEngagementTeam(long auditId, AuditType auditType, User lead, List<Long> memberIds) {
        teamMemberRepository.deleteByAuditId(auditId);
        GrcAuditTeamMember leadRow = new GrcAuditTeamMember();
        leadRow.setAuditId(auditId);
        leadRow.setUserId(lead.getId());
        leadRow.setTeamRole(EngagementTeamRole.LEAD);
        teamMemberRepository.save(leadRow);

        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (Long userId : memberIds) {
            if (userId == null || userId.equals(lead.getId()) || !seen.add(userId)) {
                continue;
            }
            User member = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Team member not found: " + userId));
            assertMemberRole(member);
            GrcAuditTeamMember row = new GrcAuditTeamMember();
            row.setAuditId(auditId);
            row.setUserId(userId);
            row.setTeamRole(EngagementTeamRole.MEMBER);
            teamMemberRepository.save(row);
        }
    }

    private void assertMemberRole(User member) {
        if (!isAuditorLead(member)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Engagement team must be Auditor, Admin, or Owner");
        }
    }

    /** Product role is Auditor. EXTERNAL_AUDITOR remains for invited portal leads. */
    private static boolean isAuditor(User user) {
        return user.getRoles().contains(Role.INTERNAL_AUDITOR)
                || user.getRoles().contains(Role.EXTERNAL_AUDITOR);
    }

    private static boolean isAuditorLead(User user) {
        return isAuditor(user)
                || user.getRoles().contains(Role.ADMIN)
                || user.getRoles().contains(Role.OWNER);
    }

    private void saveScopeModules(long auditId, String frameworkModuleCode, List<EngagementScopeModule> requested) {
        scopeModuleRepository.deleteByAuditId(auditId);
        List<EngagementScopeModule> modules = requested == null || requested.isEmpty()
                ? EngagementScopeDefaults.forFrameworkModule(frameworkModuleCode)
                : new ArrayList<>(new LinkedHashSet<>(requested));
        if (modules.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one scope module is required");
        }
        for (EngagementScopeModule module : modules) {
            GrcAuditScopeModule row = new GrcAuditScopeModule();
            row.setAuditId(auditId);
            row.setModuleCode(module);
            scopeModuleRepository.save(row);
        }
    }

    private void saveScopeNodes(long auditId, Set<Long> scopeNodeIds) {
        if (scopeNodeIds == null || scopeNodeIds.isEmpty()) {
            return;
        }
        for (Long nodeId : scopeNodeIds) {
            GrcAuditScopeNode row = new GrcAuditScopeNode();
            row.setAuditId(auditId);
            row.setFrameworkNodeId(nodeId);
            scopeNodeRepository.save(row);
        }
    }

    private void buildFrameworkChecklist(GrcAudit audit, GrcAuditChecklist sheet,
                                         List<FrameworkNodeResponse> nodes, Set<Long> scopeNodeIds) {
        Set<Long> allowed = scopeNodeIds == null || scopeNodeIds.isEmpty() ? null : scopeNodeIds;
        if (allowed != null) {
            Set<Long> valid = nodes.stream().map(FrameworkNodeResponse::id).collect(Collectors.toSet());
            for (Long nodeId : allowed) {
                if (!valid.contains(nodeId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Control not in framework: " + nodeId);
                }
            }
        }
        int order = 0;
        for (FrameworkNodeResponse node : nodes) {
            if (allowed != null && !allowed.contains(node.id())) {
                continue;
            }
            GrcAuditChecklistItem item = new GrcAuditChecklistItem();
            item.setAuditId(audit.getId());
            item.setChecklistId(sheet.getId());
            item.setSortOrder(order++);
            item.setFrameworkNodeId(node.id());
            item.setNodeCode(trimClause(node.code()));
            item.setTitle(node.title());
            item.setLibraryLocked(true);
            item.setInScope(true);
            checklistRepository.save(item);
        }
    }

    private GrcAuditChecklist newChecklistHeader(long auditId, ChecklistTemplateKey key, String name, int sortOrder) {
        GrcAuditChecklist sheet = new GrcAuditChecklist();
        sheet.setAuditId(auditId);
        sheet.setTemplateKey(key);
        sheet.setName(name);
        sheet.setSortOrder(sortOrder);
        return auditChecklistRepository.save(sheet);
    }

    private void seedLibraryChecklist(long auditId, ChecklistTemplateKey key, long actorUserId) {
        tenantChecklistLibrary.ensureSeeded(actorUserId);
        var lib = tenantChecklistLibrary.requireByTemplateKey(key);
        copyLibraryChecklist(auditId, lib.getId());
    }

    private List<Long> checklistLibraryIds(CreateAuditRequest req) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        if (req.checklistLibraryIds() != null) {
            for (Long id : req.checklistLibraryIds()) {
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        if (ids.isEmpty() && req.checklistLibraryId() != null) {
            ids.add(req.checklistLibraryId());
        }
        return new ArrayList<>(ids);
    }

    private void copyLibraryChecklists(long auditId, List<Long> libraryIds) {
        Set<ChecklistTemplateKey> keys = new LinkedHashSet<>();
        Set<String> names = new LinkedHashSet<>();
        for (Long libraryId : libraryIds) {
            var lib = tenantChecklistLibrary.require(libraryId);
            if (lib.getTemplateKey() != ChecklistTemplateKey.CUSTOM && !keys.add(lib.getTemplateKey())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        lib.getName() + " is already selected");
            }
            if (!names.add(lib.getName().trim().toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A checklist named \"" + lib.getName().trim() + "\" is already selected");
            }
        }
        for (Long libraryId : libraryIds) {
            copyLibraryChecklist(auditId, libraryId);
        }
    }

    private void copyLibraryChecklist(long auditId, long libraryId) {
        var lib = tenantChecklistLibrary.require(libraryId);
        int order = auditChecklistRepository.countByAuditId(auditId);
        GrcAuditChecklist sheet = newChecklistHeader(auditId, lib.getTemplateKey(), lib.getName(), order);
        int lineOrder = 0;
        for (var line : tenantChecklistLibrary.items(lib.getId())) {
            GrcAuditChecklistItem item = new GrcAuditChecklistItem();
            item.setAuditId(auditId);
            item.setChecklistId(sheet.getId());
            item.setSortOrder(lineOrder++);
            item.setFrameworkNodeId(null);
            item.setNodeCode(trimClause(line.getClauseRef()));
            item.setSection(blankToNull(line.getSection()));
            item.setTitle(line.getTitle() == null || line.getTitle().isBlank()
                    ? line.getClauseRef() : line.getTitle().trim());
            item.setTestFocus(blankToNull(line.getTestFocus()));
            item.setSoaApplicability(blankToNull(line.getSoaApplicability()));
            item.setEvidenceModule(blankToNull(line.getEvidenceLives()));
            item.setLibraryLocked(lib.isSystemSeeded());
            item.setInScope(true);
            checklistRepository.save(item);
        }
    }

    private static String trimClause(String clause) {
        String v = clause.trim();
        return v.length() <= 80 ? v : v.substring(0, 80);
    }

    private AuditStandard inferStandard(CreateAuditRequest req) {
        if (req.checklistTemplate() != null && req.checklistTemplate() != ChecklistTemplateKey.CUSTOM) {
            return switch (req.checklistTemplate()) {
                case ISO_20000 -> AuditStandard.ISO_20000;
                case UAE_IA -> AuditStandard.UAE_IA;
                default -> AuditStandard.ISO_27001;
            };
        }
        return AuditStandard.ISO_27001;
    }

    private boolean scopeNodesChanged(long auditId, Set<Long> requested) {
        Set<Long> existing = scopeNodeRepository.findByAuditId(auditId).stream()
                .map(GrcAuditScopeNode::getFrameworkNodeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return !existing.equals(requested);
    }

    private String frameworkModuleCode(GrcAudit audit) {
        if (audit.getFrameworkId() == null) {
            return "ISMS";
        }
        String tenantId = TenantContext.getTenantId();
        return frameworkLibraryService.getActivatedControls(tenantId, audit.getFrameworkId())
                .framework().moduleCode();
    }

    private void assertEditable(GrcAudit a) {
        assertNotLocked(a);
        if (a.getStatus() == GrcAuditStatus.REPORT_ISSUED || a.getStatus() == GrcAuditStatus.CLOSED
                || a.getStatus() == GrcAuditStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Audit is awaiting Admin or completed");
        }
    }

    private void assertNotLocked(GrcAudit a) {
        if (a.isLocked() || a.getStatus() == GrcAuditStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Audit report is locked");
        }
    }

    private boolean isAuditorOnly(User user) {
        if (isAdmin(user) || user.getRoles().contains(Role.GLOBAL_READER)) {
            return false;
        }
        return user.getRoles().contains(Role.INTERNAL_AUDITOR)
                || user.getRoles().contains(Role.EXTERNAL_AUDITOR);
    }

    private void grantInternalTeam(GrcAudit audit, User lead, List<Long> memberIds) {
        if (audit.getAuditType() != AuditType.INTERNAL
                && audit.getAuditType() != AuditType.FOLLOW_UP
                && audit.getAuditType() != AuditType.EXTERNAL_ASSESSMENT) {
            return;
        }
        accessGrantService.grantTeam(audit, lead, AuditGranteeType.INTERNAL_AUDITOR);
        if (memberIds == null) {
            return;
        }
        for (Long userId : memberIds) {
            if (userId == null || userId.equals(lead.getId())) {
                continue;
            }
            userRepository.findById(userId).ifPresent(member ->
                    accessGrantService.grantTeam(audit, member, AuditGranteeType.INTERNAL_AUDITOR));
        }
    }

    private static Long firstOrgUnit(List<Long> orgUnitIds) {
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            return null;
        }
        return orgUnitIds.get(0);
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
    }

    private boolean isExternalOnly(User user) {
        return user.getRoles().contains(Role.EXTERNAL_AUDITOR)
                && user.getRoles().stream().noneMatch(r ->
                r == Role.OWNER || r == Role.ADMIN || r == Role.INTERNAL_AUDITOR || r == Role.GLOBAL_READER);
    }

    private List<String> adminEmails() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .map(User::getEmail)
                .toList();
    }

    private Map<String, String> auditData(GrcAudit a) {
        Map<String, String> data = new HashMap<>();
        String name = a.getName() == null ? "" : a.getName();
        data.put("name", name);
        data.put("auditName", name);
        data.put("frameworkCode", auditFrameworkLabel(a));
        data.put("auditType", a.getAuditType() != null ? a.getAuditType().name() : "-");
        data.put("startDate", a.getStartDate() != null ? a.getStartDate().toString() : "-");
        data.put("endDate", a.getEndDate() != null ? a.getEndDate().toString() : "-");
        data.put("status", a.getStatus() != null ? a.getStatus().name() : "-");
        return data;
    }

    private static String auditFrameworkLabel(GrcAudit a) {
        if (a.getAuditStandard() == AuditStandard.MULTIPLE) {
            return "Multiple";
        }
        String frameworkName = a.getFrameworkName();
        if (frameworkName != null && !frameworkName.isBlank() && !"-".equals(frameworkName)) {
            return frameworkName;
        }
        String code = a.getFrameworkCode();
        return code == null || code.isBlank() ? "-" : code;
    }

    private void trail(User user, AuditActionType action, GrcAudit a) {
        String roles = user.getRoles().stream().map(Enum::name).sorted()
                .reduce((x, y) -> x + "," + y).orElse("");
        auditTrailService.record(
                action, "AUDIT", user.getId(), roles,
                "GRC_AUDIT", String.valueOf(a.getId()), a.getName(), null, null, null, null, null);
    }

    private AuditChecklistItemResponse toChecklistItem(GrcAuditChecklistItem item, boolean showWorkingNotes,
                                                      Map<Long, String> findingCodes) {
        String code = item.getFindingId() == null ? null : findingCodes.get(item.getFindingId());
        return AuditChecklistItemResponse.from(item, showWorkingNotes, code);
    }

    /**
     * Major/Minor NC, Observation, and OFI mint (or refresh) a Finding so CAPA has an ID.
     * Auditor or Admin can do this from the working paper.
     */
    private String syncFindingFromChecklist(User actor, GrcAudit a, GrcAuditChecklistItem item) {
        if (a.getAuditMode() == AuditMode.ASSESSMENT) {
            return item.getFindingId() == null ? null : findingDisplayCode(item.getFindingId());
        }
        FindingSeverity severity = severityFor(item.getConclusion());
        if (severity == null) {
            return item.getFindingId() == null ? null : findingDisplayCode(item.getFindingId());
        }
        String title = firstFilled(item.getTitle(), item.getNodeCode(), "Finding");
        if (title.length() > 512) {
            title = title.substring(0, 512);
        }
        String description = firstFilled(item.getFindingDescription(), item.getNotes(), title);
        GrcAuditFinding f = null;
        if (item.getFindingId() != null) {
            f = findingRepository.findByIdAndAuditId(item.getFindingId(), a.getId()).orElse(null);
            if (f == null) {
                item.setFindingId(null);
            }
        }
        if (f == null) {
            f = new GrcAuditFinding();
            f.setAuditId(a.getId());
            f.setChecklistItemId(item.getId());
            f.setDisplayCode(String.format("FND-%03d", findingRepository.countByAuditId(a.getId()) + 1));
            f.setCreatedByUserId(actor.getId());
            f.setSeverity(severity);
            f.setTitle(title);
            f.setDescription(description);
            f.setControlRef(blankToNull(item.getNodeCode()));
            findingRepository.save(f);
            item.setFindingId(f.getId());
            notifyFindingLogged(a, f);
            trail(actor, AuditActionType.CREATED, a);
        } else {
            f.setSeverity(severity);
            f.setTitle(title);
            f.setDescription(description);
            f.setControlRef(blankToNull(item.getNodeCode()));
            if (f.getChecklistItemId() == null) {
                f.setChecklistItemId(item.getId());
            }
            findingRepository.save(f);
        }
        return f.getDisplayCode();
    }

    private void notifyFindingLogged(GrcAudit a, GrcAuditFinding f) {
        notificationService.trigger(
                "AUDIT_FINDING_LOGGED",
                String.valueOf(f.getId()),
                List.of("ADMIN", "OWNER"),
                Map.of(
                        "displayCode", f.getDisplayCode(),
                        "severity", f.getSeverity().name(),
                        "auditName", a.getName(),
                        "title", f.getTitle()));
    }

    private void assertChecklistLineRemovable(GrcAuditChecklistItem item) {
        if (item.getFindingId() != null || findingRepository.existsByChecklistItemId(item.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This line has " + findingDisplayCode(item.getFindingId())
                            + ". Keep or close that finding first.");
        }
    }

    private String findingDisplayCode(Long findingId) {
        if (findingId == null) {
            return "a finding";
        }
        return findingRepository.findById(findingId).map(GrcAuditFinding::getDisplayCode).orElse("a finding");
    }

    private static FindingSeverity severityFor(ChecklistConclusion conclusion) {
        if (conclusion == null) {
            return null;
        }
        return switch (conclusion) {
            case MAJOR_NC -> FindingSeverity.MAJOR_NC;
            case MINOR_NC -> FindingSeverity.MINOR_NC;
            case OBSERVATION -> FindingSeverity.OBSERVATION;
            case OFI -> FindingSeverity.OFI;
            default -> null;
        };
    }

    private static String firstFilled(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "Finding";
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
