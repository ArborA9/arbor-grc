package com.a9.arbor.grc.modules.project.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.FrameworkLibraryService;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.catalog.service.RiskLibraryService;
import com.a9.arbor.grc.catalog.web.dto.FrameworkDetailResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkSummaryResponse;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapItemReviewStatus;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.gap.web.dto.GapAssessmentSummaryResponse;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.project.domain.ComplianceProject;
import com.a9.arbor.grc.modules.project.domain.ComplianceProjectFramework;
import com.a9.arbor.grc.modules.project.domain.ComplianceProjectStatus;
import com.a9.arbor.grc.modules.project.repo.ComplianceProjectFrameworkRepository;
import com.a9.arbor.grc.modules.project.repo.ComplianceProjectRepository;
import com.a9.arbor.grc.modules.project.web.dto.ComplianceProjectDetailResponse;
import com.a9.arbor.grc.modules.project.web.dto.ComplianceProjectSummaryResponse;
import com.a9.arbor.grc.modules.project.web.dto.CreateComplianceProjectRequest;
import com.a9.arbor.grc.modules.project.web.dto.ProjectFrameworkResponse;
import com.a9.arbor.grc.modules.project.web.dto.UpdateComplianceProjectStatusRequest;
import com.a9.arbor.grc.modules.risk.service.RiskService;
import com.a9.arbor.grc.modules.soa.repo.SoaRepository;
import com.a9.arbor.grc.modules.soa.service.SoaService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComplianceProjectService {

    private final ComplianceProjectRepository projectRepository;
    private final ComplianceProjectFrameworkRepository frameworkLinkRepository;
    private final FrameworkLibraryService frameworkLibraryService;
    private final RiskLibraryService riskLibraryService;
    private final RiskService riskService;
    private final SoaService soaService;
    private final SoaRepository soaRepository;
    private final GapAssessmentRepository gapAssessmentRepository;
    private final GapItemRepository gapItemRepository;
    private final AuditTrailService auditTrailService;
    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;

    public ComplianceProjectService(ComplianceProjectRepository projectRepository,
                                    ComplianceProjectFrameworkRepository frameworkLinkRepository,
                                    FrameworkLibraryService frameworkLibraryService,
                                    RiskLibraryService riskLibraryService,
                                    @Lazy RiskService riskService,
                                    @Lazy SoaService soaService,
                                    SoaRepository soaRepository,
                                    GapAssessmentRepository gapAssessmentRepository,
                                    GapItemRepository gapItemRepository,
                                    AuditTrailService auditTrailService,
                                    ModuleLicensingService moduleLicensingService,
                                    AdminModuleAccessService adminModuleAccessService) {
        this.projectRepository = projectRepository;
        this.frameworkLinkRepository = frameworkLinkRepository;
        this.frameworkLibraryService = frameworkLibraryService;
        this.riskLibraryService = riskLibraryService;
        this.riskService = riskService;
        this.soaService = soaService;
        this.soaRepository = soaRepository;
        this.gapAssessmentRepository = gapAssessmentRepository;
        this.gapItemRepository = gapItemRepository;
        this.auditTrailService = auditTrailService;
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    @Transactional(readOnly = true)
    public List<ComplianceProjectSummaryResponse> list() {
        return list(null);
    }

    @Transactional(readOnly = true)
    public List<ComplianceProjectSummaryResponse> list(User user) {
        List<ComplianceProjectSummaryResponse> rows = projectRepository.findAllByOrderByYearDescUpdatedAtDesc()
                .stream()
                .map(this::toSummary)
                .toList();
        if (!RoleAccess.isContributorOnly(user)) {
            return rows.stream().filter(p -> projectVisible(p.id())).toList();
        }
        List<GapItem> mineItems = gapItemRepository.findByAssignedToUserId(user.getId()).stream()
                .filter(GapItem::isReleasedToContributor)
                .toList();
        if (mineItems.isEmpty()) {
            return List.of();
        }
        Set<Long> assessmentIds = mineItems.stream().map(GapItem::getGapAssessmentId).collect(Collectors.toSet());
        Map<Long, Long> assessmentToProject = new HashMap<>();
        for (GapAssessment assessment : gapAssessmentRepository.findAllById(assessmentIds)) {
            assessmentToProject.put(assessment.getId(), assessment.getProjectId());
        }
        Map<Long, List<GapItem>> byProject = new HashMap<>();
        for (GapItem item : mineItems) {
            Long projectId = assessmentToProject.get(item.getGapAssessmentId());
            if (projectId == null) {
                continue;
            }
            byProject.computeIfAbsent(projectId, k -> new ArrayList<>()).add(item);
        }
        return rows.stream()
                .filter(p -> byProject.containsKey(p.id()))
                .map(p -> withAssignedCounts(p, byProject.get(p.id())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ComplianceProjectDetailResponse get(long id) {
        return get(null, id);
    }

    @Transactional(readOnly = true)
    public ComplianceProjectDetailResponse get(User user, long id) {
        ComplianceProject project = require(id);
        if (!projectVisible(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        List<GapAssessmentSummaryResponse> gaps = gapAssessmentRepository.findByProjectIdOrderByUpdatedAtDesc(id)
                .stream()
                .map(GapAssessmentSummaryResponse::from)
                .toList();
        if (RoleAccess.isContributorOnly(user)) {
            Set<Long> assessmentIds = assignedAssessmentIds(user.getId());
            if (!assignedProjectIds(user.getId()).contains(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
            }
            gaps = gaps.stream().filter(g -> assessmentIds.contains(g.id())).toList();
            return new ComplianceProjectDetailResponse(toSummary(project), List.of(), gaps);
        }
        return new ComplianceProjectDetailResponse(
                toSummary(project),
                soaService.listByProject(id),
                gaps);
    }

    private static ComplianceProjectSummaryResponse withAssignedCounts(
            ComplianceProjectSummaryResponse p, List<GapItem> items) {
        int assigned = items.size();
        int needs = (int) items.stream().filter(ComplianceProjectService::needsContributorGapAction).count();
        return new ComplianceProjectSummaryResponse(
                p.id(), p.name(), p.year(), p.status(), p.frameworks(), p.includeCloud(),
                p.createdAt(), p.updatedAt(), p.soaCount(), p.gapAssessmentCount(), assigned, needs);
    }

    private static boolean needsContributorGapAction(GapItem item) {
        GapItemReviewStatus review = item.getReviewStatus();
        return review == null
                || review == GapItemReviewStatus.OPEN
                || review == GapItemReviewStatus.SENT_BACK;
    }

    private Set<Long> assignedAssessmentIds(long userId) {
        return gapItemRepository.findByAssignedToUserId(userId).stream()
                .filter(GapItem::isReleasedToContributor)
                .map(GapItem::getGapAssessmentId)
                .collect(Collectors.toSet());
    }

    private Set<Long> assignedProjectIds(long userId) {
        Set<Long> assessmentIds = assignedAssessmentIds(userId);
        if (assessmentIds.isEmpty()) {
            return Set.of();
        }
        return gapAssessmentRepository.findAllById(assessmentIds).stream()
                .map(GapAssessment::getProjectId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public ComplianceProjectDetailResponse create(User user, CreateComplianceProjectRequest req) {
        String tenantId = requireTenant();
        Set<Long> uniqueIds = new LinkedHashSet<>(req.frameworkIds());
        if (uniqueIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one framework");
        }

        String name = req.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A project named \"" + name + "\" already exists");
        }
        ComplianceProject project = new ComplianceProject();
        project.setName(name);
        project.setYear(req.year());
        project.setStatus(ComplianceProjectStatus.ACTIVE);
        project.setCreatedByUserId(user.getId());
        project.setIncludeCloud(Boolean.TRUE.equals(req.includeCloud()));
        projectRepository.save(project);

        String projectModule = null;
        for (Long frameworkId : uniqueIds) {
            FrameworkDetailResponse detail = frameworkLibraryService.getActivatedControls(tenantId, frameworkId);
            FrameworkSummaryResponse fw = detail.framework();
            if (!isAssessmentModule(fw.moduleCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Compliance projects support ISMS, BCMS, QMS, OHSMS, SAMA, ITSM, AIMS, or PRIVACY frameworks only: "
                                + fw.code());
            }
            if (projectModule == null) {
                projectModule = fw.moduleCode();
                try {
                    moduleLicensingService.requireWritable(tenantId, ModuleCode.valueOf(projectModule));
                    adminModuleAccessService.assertMayUse(ModuleCode.valueOf(projectModule), true);
                } catch (IllegalArgumentException ignored) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Unknown module for framework: " + fw.code());
                }
            } else if (!projectModule.equals(fw.moduleCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "All frameworks in a compliance project must share the same module");
            }
            ComplianceProjectFramework link = new ComplianceProjectFramework();
            link.setProjectId(project.getId());
            link.setFrameworkId(fw.id());
            link.setFrameworkCode(fw.code());
            link.setFrameworkName(fw.name());
            link.setFrameworkVersion(fw.version());
            frameworkLinkRepository.save(link);
        }

        int seeded = 0;
        for (Long frameworkId : uniqueIds) {
            FrameworkDetailResponse detail = frameworkLibraryService.getActivatedControls(tenantId, frameworkId);
            FrameworkSummaryResponse fw = detail.framework();
            if (!ModuleCode.ISMS.name().equals(fw.moduleCode())) {
                continue;
            }
            var templates = riskLibraryService.listActiveEntities(fw.id());
            seeded += riskService.seedFromCuratedLibrary(user, project.getId(), fw.id(), fw.code(), templates);
        }

        audit(user, AuditActionType.CREATED, String.valueOf(project.getId()), project.getName(),
                null, null, project.getName());
        ComplianceProjectDetailResponse detail = get(project.getId());
        return new ComplianceProjectDetailResponse(
                detail.project(), detail.soas(), detail.gapAssessments(), seeded);
    }

    @Transactional
    public ComplianceProjectDetailResponse updateStatus(User user, long id, UpdateComplianceProjectStatusRequest req) {
        ComplianceProject project = require(id);
        assertProjectWritable(project.getId());
        String old = project.getStatus().name();
        project.setStatus(req.status());
        projectRepository.save(project);
        audit(user, AuditActionType.UPDATED, String.valueOf(project.getId()), project.getName(),
                "status", old, req.status().name());
        return get(id);
    }

    @Transactional(readOnly = true)
    public ComplianceProject requireActive(long projectId) {
        ComplianceProject project = require(projectId);
        if (project.getStatus() != ComplianceProjectStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Project is " + project.getStatus() + ": only ACTIVE projects accept SoA/Gap work");
        }
        return project;
    }

    @Transactional(readOnly = true)
    public void assertFrameworkInProject(long projectId, long frameworkId) {
        if (!frameworkLinkRepository.existsByProjectIdAndFrameworkId(projectId, frameworkId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Framework is not part of this compliance project");
        }
    }

    @Transactional(readOnly = true)
    private boolean projectVisible(long projectId) {
        var links = frameworkLinkRepository.findByProjectIdOrderByFrameworkCodeAsc(projectId);
        if (links.isEmpty()) {
            return !adminModuleAccessService.restrictsReads();
        }
        return links.stream().allMatch(link -> adminModuleAccessService.mayUseFramework(link.getFrameworkId(), false));
    }

    public ComplianceProject require(long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compliance project not found"));
    }

    private ComplianceProjectSummaryResponse toSummary(ComplianceProject project) {
        List<ProjectFrameworkResponse> frameworks = frameworkLinkRepository
                .findByProjectIdOrderByFrameworkCodeAsc(project.getId()).stream()
                .map(l -> new ProjectFrameworkResponse(
                        l.getFrameworkId(), l.getFrameworkCode(), l.getFrameworkName(), l.getFrameworkVersion()))
                .toList();
        return new ComplianceProjectSummaryResponse(
                project.getId(),
                project.getName(),
                project.getYear(),
                project.getStatus(),
                frameworks,
                project.isIncludeCloud(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                (int) soaRepository.countByProjectId(project.getId()),
                (int) gapAssessmentRepository.countByProjectId(project.getId()),
                0,
                0);
    }

    private void assertProjectWritable(long projectId) {
        List<ComplianceProjectFramework> links =
                frameworkLinkRepository.findByProjectIdOrderByFrameworkCodeAsc(projectId);
        if (links.isEmpty()) {
            return;
        }
        moduleLicensingService.requireWritableFramework(requireTenant(), links.get(0).getFrameworkId());
    }

    private static String requireTenant() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant context required");
        }
        return tenantId;
    }

    private static boolean isAssessmentModule(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            return false;
        }
        return ModuleCode.ISMS.name().equals(moduleCode)
                || ModuleCode.BCMS.name().equals(moduleCode)
                || ModuleCode.QMS.name().equals(moduleCode)
                || ModuleCode.OHSMS.name().equals(moduleCode)
                || ModuleCode.SAMA.name().equals(moduleCode)
                || ModuleCode.ITSM.name().equals(moduleCode)
                || ModuleCode.AIMS.name().equals(moduleCode)
                || ModuleCode.PRIVACY.name().equals(moduleCode);
    }

    private void audit(User user, AuditActionType action, String recordId, String recordName,
                       String field, String oldVal, String newVal) {
        auditTrailService.record(
                action, "COMPLIANCE_PROJECT", user.getId(), primaryRole(user),
                "COMPLIANCE_PROJECT", recordId, recordName, field, oldVal, newVal, null, null);
    }

    private static String primaryRole(User user) {
        return user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
    }
}
