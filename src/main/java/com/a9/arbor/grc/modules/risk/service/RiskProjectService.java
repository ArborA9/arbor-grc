package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.FrameworkQueryService;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.risk.domain.RiskProject;
import com.a9.arbor.grc.modules.risk.domain.RiskProjectSeedStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskProjectStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.repo.RiskProjectRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskProjectRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskProjectResponse;
import com.a9.arbor.grc.modules.risk.web.dto.UpdateRiskProjectStatusRequest;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RiskProjectService {

    private static final Logger log = LoggerFactory.getLogger(RiskProjectService.class);

    private final RiskProjectRepository projectRepository;
    private final RiskRecordRepository riskRepository;
    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;
    private final FrameworkQueryService frameworkQueryService;
    private final AuditTrailService auditTrailService;
    private final RiskService riskService;
    private final ExecutorService seedExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "is-project-seed");
        t.setDaemon(true);
        return t;
    });

    public RiskProjectService(RiskProjectRepository projectRepository,
                              RiskRecordRepository riskRepository,
                              ModuleLicensingService moduleLicensingService,
                              AdminModuleAccessService adminModuleAccessService,
                              FrameworkQueryService frameworkQueryService,
                              AuditTrailService auditTrailService,
                              @Lazy RiskService riskService) {
        this.projectRepository = projectRepository;
        this.riskRepository = riskRepository;
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
        this.frameworkQueryService = frameworkQueryService;
        this.auditTrailService = auditTrailService;
        this.riskService = riskService;
    }

    @Transactional(readOnly = true)
    public List<RiskProjectResponse> list(RiskType type) {
        return list(null, type);
    }

    @Transactional(readOnly = true)
    public List<RiskProjectResponse> list(User user, RiskType type) {
        requireModuleForType(type);
        List<RiskProject> projects = projectRepository.findByProjectTypeOrderByYearDescUpdatedAtDesc(type);
        Map<Long, Integer> counts = countByProjectIds(projects.stream().map(RiskProject::getId).toList());
        List<RiskProjectResponse> rows = projects.stream()
                .map(p -> RiskProjectResponse.from(p, counts.getOrDefault(p.getId(), 0)))
                .toList();
        if (!RoleAccess.isContributorOnly(user)) {
            return rows;
        }
        List<RiskRecord> mineRows = riskRepository.findVisibleToContributor(type, user.getId());
        Set<Long> mine = mineRows.stream()
                .map(RiskRecord::getRiskProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return rows.stream()
                .filter(p -> mine.contains(p.id()))
                .map(p -> {
                    List<RiskRecord> assignedRows = mineRows.stream()
                            .filter(r -> p.id().equals(r.getRiskProjectId()))
                            .toList();
                    int needsAction = (int) assignedRows.stream()
                            .filter(r -> owesContributorAction(r, user.getId()))
                            .count();
                    return new RiskProjectResponse(
                            p.id(), p.projectType(), p.name(), p.year(), p.status(), p.frameworkId(),
                            p.createdByUserId(), assignedRows.size(), needsAction, p.seedTotal(), p.seedStatus(),
                            p.createdAt(), p.updatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public RiskProjectResponse get(long id) {
        RiskProject p = require(id);
        requireModuleForType(p.getProjectType());
        return RiskProjectResponse.from(p, riskRepository.countByRiskProjectId(p.getId()));
    }

    @Transactional
    public RiskProjectResponse create(User user, CreateRiskProjectRequest req) {
        RiskType type = req.projectType();
        requireWriteForType(type);
        if (type == RiskType.IS_RISK && req.frameworkId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "IS risk project requires a framework");
        }
        if (type == RiskType.IS_RISK) {
            Framework fw = frameworkQueryService.requireFramework(req.frameworkId());
            if (!IsRiskApplicableModules.includes(fw.getModuleCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This framework is not used for Information Security Risk. Choose an ISMS, ERM, ITSM, AIMS, or BCMS framework");
            }
        }
        if (type == RiskType.ERM && req.frameworkId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ERM projects do not take a framework");
        }
        String name = req.name().trim();
        if (projectRepository.existsByProjectTypeAndNameIgnoreCase(type, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A project named \"" + name + "\" already exists");
        }
        RiskProject p = new RiskProject();
        p.setProjectType(type);
        p.setName(name);
        p.setYear(req.year());
        p.setStatus(RiskProjectStatus.ACTIVE);
        p.setFrameworkId(type == RiskType.IS_RISK ? req.frameworkId() : null);
        p.setCreatedByUserId(user.getId());
        if (type == RiskType.IS_RISK) {
            int total = riskService.countSeedStubsForIsProject(req.frameworkId());
            p.setSeedTotal(total);
            p.setSeedStatus(total > 0 ? RiskProjectSeedStatus.RUNNING : RiskProjectSeedStatus.DONE);
        } else {
            p.setSeedTotal(0);
            p.setSeedStatus(RiskProjectSeedStatus.DONE);
        }
        projectRepository.save(p);

        String role = user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
        auditTrailService.record(
                AuditActionType.CREATED, "RISK", user.getId(), role,
                "RISK_PROJECT", String.valueOf(p.getId()), p.getName(),
                null, null, null, null, null);

        if (type == RiskType.IS_RISK && p.getSeedStatus() == RiskProjectSeedStatus.RUNNING) {
            String tenantId = TenantContext.getTenantId();
            long projectId = p.getId();
            long userId = user.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    seedExecutor.execute(() -> {
                        TenantContext.setTenantId(tenantId);
                        try {
                            riskService.seedStubsForIsProject(userId, projectId);
                        } catch (Exception e) {
                            log.error("IS project {} seed failed", projectId, e);
                            riskService.markIsProjectSeedFailed(projectId);
                        } finally {
                            TenantContext.clear();
                        }
                    });
                }
            });
        }

        return RiskProjectResponse.from(p, 0);
    }

    @Transactional
    public RiskProjectResponse updateStatus(User user, long id, UpdateRiskProjectStatusRequest req) {
        RiskProject p = require(id);
        requireWriteForType(p.getProjectType());
        p.setStatus(req.status());
        projectRepository.save(p);
        String role = user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
        auditTrailService.record(
                AuditActionType.UPDATED, "RISK", user.getId(), role,
                "RISK_PROJECT", String.valueOf(p.getId()), p.getName(),
                "status", p.getStatus().name(), req.status().name(), null, null);
        return RiskProjectResponse.from(p, riskRepository.countByRiskProjectId(p.getId()));
    }

    public RiskProject require(long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk project not found"));
    }

    private Map<Long, Integer> countByProjectIds(List<Long> ids) {
        Map<Long, Integer> counts = new HashMap<>();
        if (ids.isEmpty()) {
            return counts;
        }
        for (Object[] row : riskRepository.countGroupedByRiskProjectId(ids)) {
            if (row[0] instanceof Number projectId && row[1] instanceof Number n) {
                counts.put(projectId.longValue(), n.intValue());
            }
        }
        return counts;
    }

    /** Custodian still owes input, or the owner is on the acceptance turn. Viewing alone does not count. */
    private static boolean owesContributorAction(RiskRecord risk, Long userId) {
        if (userId.equals(risk.getAssignedContributorId()) && needsContributorAction(risk.getStatus())) {
            return true;
        }
        return userId.equals(risk.getRiskOwnerUserId())
                && !userId.equals(risk.getAssignedContributorId())
                && risk.getStatus() == RiskStatus.PENDING_ACCEPTANCE;
    }

    /** Contributor still owes input (not waiting on Admin, not closed). */
    private static boolean needsContributorAction(RiskStatus status) {
        return status == RiskStatus.ASSIGNED
                || status == RiskStatus.RE_OPENED
                || status == RiskStatus.MITIGATING
                || status == RiskStatus.PENDING_ACCEPTANCE
                || status == RiskStatus.OVERDUE
                || status == RiskStatus.DRAFT;
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
}
