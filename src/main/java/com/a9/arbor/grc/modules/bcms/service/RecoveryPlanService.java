package com.a9.arbor.grc.modules.bcms.service;

import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.bcms.domain.BiaReviewOutcome;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlan;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanBia;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanProcedure;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanStatus;
import com.a9.arbor.grc.modules.bcms.repo.BiaProcessRepository;
import com.a9.arbor.grc.modules.bcms.repo.RecoveryPlanBiaRepository;
import com.a9.arbor.grc.modules.bcms.repo.RecoveryPlanProcedureRepository;
import com.a9.arbor.grc.modules.bcms.repo.RecoveryPlanRepository;
import com.a9.arbor.grc.modules.bcms.web.dto.CreateRecoveryPlanRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.RecoveryPlanProcedureRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.RecoveryPlanProcedureResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.RecoveryPlanResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.UpdateRecoveryPlanRequest;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecoveryPlanService {

    private final RecoveryPlanRepository planRepository;
    private final RecoveryPlanBiaRepository planBiaRepository;
    private final RecoveryPlanProcedureRepository procedureRepository;
    private final BiaProcessRepository biaProcessRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final RecoveryPlanPdfWriter pdfWriter;

    public RecoveryPlanService(RecoveryPlanRepository planRepository,
                               RecoveryPlanBiaRepository planBiaRepository,
                               RecoveryPlanProcedureRepository procedureRepository,
                               BiaProcessRepository biaProcessRepository,
                               UserRepository userRepository,
                               TenantRepository tenantRepository,
                               NotificationService notificationService,
                               AuditTrailService auditTrailService,
                               RecoveryPlanPdfWriter pdfWriter) {
        this.planRepository = planRepository;
        this.planBiaRepository = planBiaRepository;
        this.procedureRepository = procedureRepository;
        this.biaProcessRepository = biaProcessRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
        this.pdfWriter = pdfWriter;
    }

    @Transactional(readOnly = true)
    public List<RecoveryPlanResponse> list(Long projectId) {
        List<RecoveryPlan> rows = projectId != null
                ? planRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)
                : planRepository.findAllByOrderByUpdatedAtDesc();
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RecoveryPlanResponse get(long id) {
        return toResponse(require(id));
    }

    @Transactional
    public RecoveryPlanResponse create(User admin, CreateRecoveryPlanRequest req) {
        assertAdmin(admin);
        RecoveryPlan p = new RecoveryPlan();
        p.setProjectId(req.projectId());
        p.setName(req.name().trim());
        p.setPlanType(req.planType());
        p.setScope(blankToNull(req.scope()));
        p.setCrisisTeam(blankToNull(req.crisisTeam()));
        p.setEscalationContacts(blankToNull(req.escalationContacts()));
        p.setCommunicationTree(blankToNull(req.communicationTree()));
        p.setResourceRequirements(blankToNull(req.resourceRequirements()));
        p.setAlternateSiteDetails(blankToNull(req.alternateSiteDetails()));
        p.setOwnerUserId(req.ownerUserId());
        p.setStatus(RecoveryPlanStatus.DRAFT);
        p.setVersionNum(1);
        p.setCreatedByUserId(admin.getId());
        planRepository.save(p);

        syncBiaLinks(p.getId(), req.biaIds());
        syncProcedures(p.getId(), req.procedures());

        audit(admin, AuditActionType.CREATED, p);
        return toResponse(p);
    }

    @Transactional
    public RecoveryPlanResponse update(User admin, long id, UpdateRecoveryPlanRequest req) {
        assertAdmin(admin);
        RecoveryPlan p = require(id);
        if (p.getStatus() == RecoveryPlanStatus.EXPIRED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Expired plans cannot be edited: create a new one");
        }
        p.setName(req.name().trim());
        p.setPlanType(req.planType());
        p.setScope(blankToNull(req.scope()));
        p.setCrisisTeam(blankToNull(req.crisisTeam()));
        p.setEscalationContacts(blankToNull(req.escalationContacts()));
        p.setCommunicationTree(blankToNull(req.communicationTree()));
        p.setResourceRequirements(blankToNull(req.resourceRequirements()));
        p.setAlternateSiteDetails(blankToNull(req.alternateSiteDetails()));
        p.setOwnerUserId(req.ownerUserId());
        DateRules.requireActionDueDateIfChanged(p.getReviewDate(), req.reviewDate(), "Review date");
        p.setReviewDate(req.reviewDate());
        if (p.getStatus() == RecoveryPlanStatus.APPROVED || p.getStatus() == RecoveryPlanStatus.PENDING_OWNER) {
            // Content changed after approval/submission was requested: needs a fresh Owner approval cycle.
            p.setStatus(RecoveryPlanStatus.DRAFT);
        }
        planRepository.save(p);

        syncBiaLinks(p.getId(), req.biaIds());
        syncProcedures(p.getId(), req.procedures());

        audit(admin, AuditActionType.UPDATED, p);
        return toResponse(p);
    }

    @Transactional
    public RecoveryPlanResponse submit(User admin, long id) {
        assertAdmin(admin);
        RecoveryPlan p = require(id);
        if (p.getStatus() != RecoveryPlanStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft plans can be submitted");
        }
        if (p.getOwnerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assign an Owner before submitting");
        }
        p.setStatus(RecoveryPlanStatus.PENDING_OWNER);
        p.setSubmittedAt(Instant.now());
        planRepository.save(p);

        userRepository.findById(p.getOwnerUserId()).ifPresent(owner ->
                notificationService.trigger("BCP_APPROVAL_NEEDED", String.valueOf(p.getId()),
                        List.of(owner.getId().toString()), baseData(p)));
        audit(admin, AuditActionType.UPDATED, p);
        return toResponse(p);
    }

    @Transactional
    public RecoveryPlanResponse approve(User user, long id) {
        RecoveryPlan p = require(id);
        boolean admin = user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
        boolean owner = p.getOwnerUserId() != null && p.getOwnerUserId().equals(user.getId());
        if (!admin && !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the plan Owner (or Admin) can approve");
        }
        if (p.getStatus() != RecoveryPlanStatus.PENDING_OWNER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plan is not awaiting Owner approval");
        }
        boolean wasApprovedBefore = p.getApprovedAt() != null;
        if (wasApprovedBefore) {
            p.setVersionNum(p.getVersionNum() + 1);
        }
        p.setStatus(RecoveryPlanStatus.APPROVED);
        p.setApprovedAt(Instant.now());
        p.setApprovedByUserId(user.getId());
        planRepository.save(p);

        notificationService.trigger("BCP_APPROVED", String.valueOf(p.getId()),
                List.of("OWNER", "ADMIN"), baseData(p));
        audit(user, AuditActionType.APPROVED, p);
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public ExportFile pdf(long id) {
        RecoveryPlan p = require(id);
        User owner = p.getOwnerUserId() != null ? userRepository.findById(p.getOwnerUserId()).orElse(null) : null;
        String orgName = tenantRepository.findByTenantId(TenantContext.getTenantId())
                .map(Tenant::getName)
                .orElse(TenantContext.getTenantId());
        byte[] pdf = pdfWriter.write(orgName, p, owner, linkedBias(p.getId()), procedures(p.getId()));
        String filename = sanitizeFilename(p.getName()) + "-v" + p.getVersionNum() + ".pdf";
        return new ExportFile(pdf, filename, "application/pdf");
    }

    private static String sanitizeFilename(String name) {
        String base = name == null || name.isBlank() ? "recovery-plan" : name.trim();
        return base.replaceAll("[^a-zA-Z0-9._-]+", "-");
    }

    public RecoveryPlan require(long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recovery plan not found"));
    }

    public List<BiaProcess> linkedBias(long planId) {
        List<Long> biaIds = planBiaRepository.findByPlanId(planId).stream()
                .map(RecoveryPlanBia::getBiaId)
                .toList();
        return biaProcessRepository.findAllById(biaIds);
    }

    public List<RecoveryPlanProcedure> procedures(long planId) {
        return procedureRepository.findByPlanIdOrderBySortOrderAscIdAsc(planId);
    }

    private void syncBiaLinks(long planId, List<Long> biaIds) {
        planBiaRepository.deleteByPlanId(planId);
        if (biaIds == null) {
            return;
        }
        for (Long biaId : biaIds) {
            BiaProcess process = biaProcessRepository.findById(biaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "BIA process " + biaId + " not found"));
            if (!process.getReviewOutcome().specialistDone()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only verified BIA processes can be linked to a recovery plan (process " + biaId
                                + " is " + process.getReviewOutcome() + ")");
            }
            RecoveryPlanBia link = new RecoveryPlanBia();
            link.setPlanId(planId);
            link.setBiaId(biaId);
            planBiaRepository.save(link);
        }
    }

    private void syncProcedures(long planId, List<RecoveryPlanProcedureRequest> procedures) {
        procedureRepository.deleteByPlanId(planId);
        if (procedures == null) {
            return;
        }
        for (RecoveryPlanProcedureRequest req : procedures) {
            RecoveryPlanProcedure proc = new RecoveryPlanProcedure();
            proc.setPlanId(planId);
            proc.setBiaId(req.biaId());
            proc.setProcessName(req.processName().trim());
            proc.setProcedureText(blankToNull(req.procedureText()));
            proc.setSortOrder(req.sortOrder());
            procedureRepository.save(proc);
        }
    }

    private RecoveryPlanResponse toResponse(RecoveryPlan p) {
        List<Long> biaIds = planBiaRepository.findByPlanId(p.getId()).stream()
                .map(RecoveryPlanBia::getBiaId)
                .toList();
        List<RecoveryPlanProcedureResponse> procs = procedureRepository.findByPlanIdOrderBySortOrderAscIdAsc(p.getId())
                .stream().map(RecoveryPlanProcedureResponse::from).toList();
        return RecoveryPlanResponse.from(p, biaIds, procs);
    }

    private void assertAdmin(User user) {
        if (user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private Map<String, String> baseData(RecoveryPlan p) {
        Map<String, String> data = new HashMap<>();
        data.put("planName", p.getName());
        data.put("version", String.valueOf(p.getVersionNum()));
        return data;
    }

    private void audit(User user, AuditActionType action, RecoveryPlan p) {
        auditTrailService.record(action, "BCMS", user.getId(), rolesSnap(user),
                "RECOVERY_PLAN", String.valueOf(p.getId()), p.getName(), null, null, null, null, null);
    }

    private static String rolesSnap(User user) {
        return user.getRoles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
