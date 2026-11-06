package com.a9.arbor.grc.modules.capa.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditFindingRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.domain.CapaPriority;
import com.a9.arbor.grc.modules.capa.domain.CapaRegister;
import com.a9.arbor.grc.modules.capa.domain.CapaReturnEvent;
import com.a9.arbor.grc.modules.capa.domain.CapaSourceType;
import com.a9.arbor.grc.modules.capa.domain.CapaStatus;
import com.a9.arbor.grc.modules.capa.repo.CapaRepository;
import com.a9.arbor.grc.modules.capa.repo.CapaReturnEventRepository;
import com.a9.arbor.grc.modules.capa.web.dto.AssignCapaRequest;
import com.a9.arbor.grc.modules.capa.web.dto.CapaResponse;
import com.a9.arbor.grc.modules.capa.web.dto.CapaReturnHistoryItem;
import com.a9.arbor.grc.modules.capa.web.dto.CreateCapaRequest;
import com.a9.arbor.grc.modules.capa.web.dto.RejectCapaRequest;
import com.a9.arbor.grc.modules.capa.web.dto.SubmitCapaResponseRequest;
import com.a9.arbor.grc.modules.capa.web.dto.UpdateCapaProgressRequest;
import com.a9.arbor.grc.modules.capa.web.dto.UpdateCapaTrackerRequest;
import com.a9.arbor.grc.modules.evidence.service.EvidenceService;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CapaService {

    public static final String MODULE_CAPA = "CAPA";
    public static final String TYPE_CAPA = "CAPA";

    private final CapaRepository capaRepository;
    private final CapaReturnEventRepository capaReturnEventRepository;
    private final UserRepository userRepository;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;
    private final GrcAuditFindingRepository auditFindingRepository;
    private final GrcAuditRepository auditRepository;
    private final EvidenceService evidenceService;
    private final CapaRegisterService capaRegisterService;

    public CapaService(CapaRepository capaRepository,
                       CapaReturnEventRepository capaReturnEventRepository,
                       UserRepository userRepository,
                       WorkTaskRepository workTaskRepository,
                       NotificationService notificationService,
                       AuditTrailService auditTrailService,
                       ModuleLicensingService moduleLicensingService,
                       AdminModuleAccessService adminModuleAccessService,
                       GrcAuditFindingRepository auditFindingRepository,
                       GrcAuditRepository auditRepository,
                       EvidenceService evidenceService,
                       CapaRegisterService capaRegisterService) {
        this.capaRepository = capaRepository;
        this.capaReturnEventRepository = capaReturnEventRepository;
        this.userRepository = userRepository;
        this.workTaskRepository = workTaskRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
        this.auditFindingRepository = auditFindingRepository;
        this.auditRepository = auditRepository;
        this.evidenceService = evidenceService;
        this.capaRegisterService = capaRegisterService;
    }

    @Transactional(readOnly = true)
    public List<CapaResponse> list(User viewer, CapaRegister register) {
        return list(viewer, register, null);
    }

    @Transactional(readOnly = true)
    public List<CapaResponse> list(User viewer, CapaRegister register, Long capaRegisterId) {
        List<Capa> rows;
        if (capaRegisterId != null) {
            capaRegisterService.require(capaRegisterId);
            rows = capaRepository.findByCapaRegisterIdOrderByIdAsc(capaRegisterId);
            if (RoleAccess.isContributorOnly(viewer)) {
                rows = rows.stream().filter(c -> viewer.getId().equals(c.getOwnerUserId())).toList();
            }
        } else if (RoleAccess.isContributorOnly(viewer)) {
            rows = capaRepository.findAllByOrderByUpdatedAtDesc().stream()
                    .filter(c -> viewer.getId().equals(c.getOwnerUserId()))
                    .toList();
        } else if (isExternalOnly(viewer)) {
            rows = capaRepository.findByRegisterCodeOrderByUpdatedAtDesc(CapaRegister.EXTERNAL_AUDIT);
        } else if (register == null) {
            rows = capaRepository.findAllByOrderByUpdatedAtDesc();
        } else {
            rows = capaRepository.findByRegisterCodeOrderByUpdatedAtDesc(register);
        }
        return toResponses(rows.stream().filter(this::capaVisible).toList());
    }

    @Transactional(readOnly = true)
    public CapaResponse get(User viewer, long id) {
        Capa c = require(id);
        if (!capaVisible(c)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CAPA not found");
        }
        if (RoleAccess.isContributorOnly(viewer) && !viewer.getId().equals(c.getOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your CAPA");
        }
        if (isExternalOnly(viewer) && c.getRegisterCode() != CapaRegister.EXTERNAL_AUDIT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "External Auditor CAPA scope only");
        }
        return toResponse(c);
    }

    @Transactional
    public CapaResponse createManual(User admin, CreateCapaRequest req) {
        assertAdmin(admin);
        if (req.capaRegisterId() != null) {
            return createOnRegister(admin, req);
        }
        if (req.auditId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Create a CAPA register for this audit first");
        }
        if (req.registerCode() == CapaRegister.BCMS) {
            requireBcmsLicensed();
        }
        String title = req.title().trim();
        Capa c = new Capa();
        applyManualSource(c, req);
        c.setSourceType(CapaSourceType.MANUAL);
        c.setTitle(title);
        c.setDescription(blankToNull(req.description()));
        c.setRecommendedAction(blankToNull(req.recommendedAction()));
        c.setControlRef(blankToNull(req.controlRef()));
        c.setSeverity(blankToNull(req.severity()));
        c.setPriority(req.priority());
        c.setStatus(CapaStatus.OPEN);
        DateRules.requireActionDueDateIfPresent(req.targetDate(), "Agreed Target Date");
        c.setTargetDate(req.targetDate());
        c.setCorrection(blankToNull(req.correction()));
        c.setRootCause(blankToNull(req.rootCause()));
        c.setCorrectiveAction(blankToNull(req.correctiveActionPlan()));
        c.setEvidenceRequired(blankToNull(req.evidenceRequired()));
        c.setContributorDepartment(blankToNull(req.contributorDepartment()));
        User owner = null;
        if (req.assignedContributorUserId() != null) {
            owner = requireAssignableContributor(req.assignedContributorUserId());
            c.setOwnerUserId(owner.getId());
            c.setAssignedByUserId(admin.getId());
            c.setAssignedAt(Instant.now());
        }
        if (req.findingId() == null) {
            applyExternalFindingCode(c, req.externalFindingCode(), null);
        }
        capaRepository.save(c);
        if (owner != null) {
            openContributorWork(admin, c, owner);
        }

        notifyAdmins("CAPA_CREATED", c);
        audit(admin, AuditActionType.CREATED, c);
        return toResponse(c);
    }

    private CapaResponse createOnRegister(User admin, CreateCapaRequest req) {
        var register = capaRegisterService.require(req.capaRegisterId());
        requireFilled(req.correction(), "Correction (the immediate fix)");
        requireFilled(req.rootCause(), "Root Cause");
        requireFilled(req.correctiveActionPlan(), "Corrective Action Plan");
        requireFilled(req.contributorDepartment(), "Contributor Department");
        if (req.targetDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agreed Target Date is required");
        }
        if (req.assignedContributorUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned Contributor is required");
        }
        DateRules.requireActionDueDateIfPresent(req.targetDate(), "Agreed Target Date");

        GrcAuditFinding finding = null;
        GrcAudit audit = null;
        if (req.findingId() != null) {
            finding = auditFindingRepository.findById(req.findingId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding not found"));
            audit = auditRepository.findById(finding.getAuditId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
            if (finding.getCapaId() != null
                    || capaRepository.existsByCapaRegisterIdAndSourceRecordId(register.getId(), finding.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Finding \"" + finding.getDisplayCode() + "\" already has a CAPA");
            }
        }

        String title = req.title() == null ? "" : req.title().trim();
        if (finding != null && (title.isEmpty() || title.equals("CAPA"))) {
            title = finding.getTitle();
        }
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding Title is required");
        }

        AuditType type = audit != null ? audit.getAuditType() : null;
        Capa c = new Capa();
        c.setCapaRegisterId(register.getId());
        if (audit != null) {
            c.setAuditId(audit.getId());
        }
        c.setConductedBy(register.getConductedBy());
        if (finding != null) {
            CapaRegister requested = req.registerCode() != null ? req.registerCode() : CapaRegister.INTERNAL_AUDIT;
            if (type == AuditType.EXTERNAL || type == AuditType.EXTERNAL_ASSESSMENT) {
                c.setRegisterCode(CapaRegister.EXTERNAL_AUDIT);
            } else if (requested == CapaRegister.OTHER) {
                c.setRegisterCode(CapaRegister.INTERNAL_AUDIT);
            } else {
                c.setRegisterCode(requested);
            }
            c.setSourceOther(null);
        } else {
            applyManualSource(c, req);
        }
        if (finding != null) {
            if (blankToNull(req.externalFindingCode()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Choose a finding or Other, not both");
            }
            c.setSourceType(CapaSourceType.AUDIT_FINDING);
            c.setSourceRecordId(finding.getId());
            c.setDescription(finding.getDescription());
            c.setRecommendedAction(finding.getRecommendedAction());
            c.setControlRef(blankToNull(req.controlRef()) != null ? req.controlRef().trim() : finding.getControlRef());
            c.setSeverity(finding.getSeverity().name());
            c.setPriority(req.priority() != null ? req.priority() : priorityFor(finding.getSeverity()));
            c.setExternalFindingCode(null);
        } else {
            c.setSourceType(CapaSourceType.MANUAL);
            c.setDescription(blankToNull(req.description()));
            c.setControlRef(blankToNull(req.controlRef()));
            c.setSeverity(blankToNull(req.severity()));
            c.setPriority(req.priority() != null ? req.priority() : CapaPriority.MEDIUM);
            applyExternalFindingCode(c, req.externalFindingCode(), register.getId());
        }
        c.setTitle(title);
        c.setStatus(CapaStatus.OPEN);
        c.setTargetDate(req.targetDate());
        c.setCorrection(req.correction().trim());
        c.setRootCause(req.rootCause().trim());
        c.setCorrectiveAction(req.correctiveActionPlan().trim());
        c.setEvidenceRequired(blankToNull(req.evidenceRequired()));
        c.setContributorDepartment(req.contributorDepartment().trim());
        User owner = requireAssignableContributor(req.assignedContributorUserId());
        c.setOwnerUserId(owner.getId());
        c.setAssignedByUserId(admin.getId());
        c.setAssignedAt(Instant.now());
        capaRepository.save(c);
        if (finding != null) {
            finding.setCapaId(c.getId());
            auditFindingRepository.save(finding);
        }
        openContributorWork(admin, c, owner);
        notifyAdmins("CAPA_CREATED", c);
        audit(admin, AuditActionType.CREATED, c);
        return toResponse(c);
    }

    /** Called when an audit is locked: auto-open CAPA for non-OFI findings. */
    @Transactional
    public Capa createFromFinding(User actor,
                                  CapaRegister register,
                                  long findingId,
                                  String title,
                                  String description,
                                  String recommendedAction,
                                  String controlRef,
                                  FindingSeverity severity) {
        Capa c = new Capa();
        c.setRegisterCode(register);
        c.setSourceType(CapaSourceType.AUDIT_FINDING);
        c.setSourceRecordId(findingId);
        c.setTitle(title);
        c.setDescription(description);
        c.setRecommendedAction(recommendedAction);
        c.setControlRef(controlRef);
        c.setSeverity(severity.name());
        c.setPriority(priorityFor(severity));
        c.setStatus(CapaStatus.OPEN);
        auditFindingRepository.findById(findingId).ifPresent(f -> {
            c.setAuditId(f.getAuditId());
            auditRepository.findById(f.getAuditId()).ifPresent(audit -> {
                var header = capaRegisterService.requireOrCreateForAudit(actor, audit);
                c.setCapaRegisterId(header.getId());
                c.setConductedBy(header.getConductedBy());
            });
        });
        capaRepository.save(c);
        notifyAdmins("CAPA_CREATED", c);
        audit(actor, AuditActionType.CREATED, c);
        return c;
    }

    /** Admin raises CAPA Register 1 from a Gap item with an identified gap (req §16.4). */
    @Transactional
    public CapaResponse createFromGapFinding(User admin, GapAssessment assessment, GapItem item) {
        assertAdmin(admin);
        if (item.getApplicability() == GapApplicability.NOT_APPLICABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot raise CAPA for N/A controls");
        }
        GapImplementationStatus effective = item.effectiveImplementationStatus();
        if (effective == null || effective == GapImplementationStatus.FULLY_IMPLEMENTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Control has no gap: implementation must be below Fully Implemented");
        }
        capaRepository.findFirstBySourceTypeAndSourceRecordIdAndStatusNot(
                        CapaSourceType.GAP_FINDING, item.getId(), CapaStatus.CLOSED)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "An open CAPA already exists for this gap item (id " + existing.getId() + ")");
                });

        Capa c = new Capa();
        c.setRegisterCode(CapaRegister.GAP);
        c.setSourceType(CapaSourceType.GAP_FINDING);
        c.setSourceRecordId(item.getId());
        c.setTitle("Gap: " + item.getNodeCode() + ": " + item.getTitle());
        c.setDescription(buildGapDescription(item));
        c.setRecommendedAction(blankToNull(item.getGapDescription()));
        c.setControlRef(item.getNodeCode());
        c.setSeverity(effective.name());
        c.setPriority(priorityForGap(effective));
        c.setStatus(CapaStatus.OPEN);
        capaRepository.save(c);

        notifyAdmins("CAPA_CREATED", c);
        audit(admin, AuditActionType.CREATED, c);
        return toResponse(c);
    }

    /** Called when a BCMS exercise is completed with gaps identified (req workflow). */
    @Transactional
    public Capa createFromExerciseGap(User actor, long exerciseId, String exerciseName, String gapsIdentified) {
        Capa c = new Capa();
        c.setRegisterCode(CapaRegister.BCMS);
        c.setSourceType(CapaSourceType.BCMS_EXERCISE);
        c.setSourceRecordId(exerciseId);
        c.setTitle("Exercise gap: " + exerciseName);
        c.setDescription(gapsIdentified);
        c.setSeverity(null);
        c.setPriority(CapaPriority.MEDIUM);
        c.setStatus(CapaStatus.OPEN);
        capaRepository.save(c);
        notifyAdmins("CAPA_CREATED", c);
        audit(actor, AuditActionType.CREATED, c);
        return c;
    }

    /**
     * Admin raises CAPA Register 1 from a risk finding (§14.3 ERM-sourced issues).
     * Works for IS Risk and ERM (both share the risk engine).
     */
    @Transactional
    public CapaResponse createFromRisk(User admin, com.a9.arbor.grc.modules.risk.domain.RiskRecord risk) {
        assertAdmin(admin);
        capaRepository.findFirstBySourceTypeAndSourceRecordIdAndStatusNot(
                        CapaSourceType.RISK_FINDING, risk.getId(), CapaStatus.CLOSED)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "An open CAPA already exists for this risk (id " + existing.getId() + ")");
                });

        Capa c = new Capa();
        c.setRegisterCode(CapaRegister.GAP);
        c.setSourceType(CapaSourceType.RISK_FINDING);
        c.setSourceRecordId(risk.getId());
        c.setTitle("Risk: " + risk.getDisplayId() + ": " + risk.getTitle());
        c.setDescription(buildRiskDescription(risk));
        c.setRecommendedAction(blankToNull(risk.getTreatmentPlan()));
        c.setControlRef(blankToNull(risk.getNodeCode()));
        c.setSeverity(risk.getResidualRisk() != null ? risk.getResidualRisk().name() : null);
        c.setPriority(priorityForRisk(
                risk.getResidualRisk() != null
                        ? com.a9.arbor.grc.modules.risk.domain.PriorityLevel.fromResidual(risk.getResidualRisk())
                        : null));
        c.setStatus(CapaStatus.OPEN);
        capaRepository.save(c);

        notifyAdmins("CAPA_CREATED", c);
        audit(admin, AuditActionType.CREATED, c);
        return toResponse(c);
    }

    private static String buildRiskDescription(com.a9.arbor.grc.modules.risk.domain.RiskRecord risk) {
        StringBuilder sb = new StringBuilder();
        if (risk.getDescription() != null && !risk.getDescription().isBlank()) {
            sb.append(risk.getDescription().trim());
        }
        if (risk.getObservedGap() != null && !risk.getObservedGap().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("Observed gap: ").append(risk.getObservedGap().trim());
        }
        if (risk.getCategory() != null) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("Category: ").append(risk.getCategory().name());
        }
        if (risk.getInherentRisk() != null || risk.getResidualRisk() != null) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append("Inherent: ")
                    .append(risk.getInherentRisk() != null ? risk.getInherentRisk().name() : "-")
                    .append(" · Residual: ")
                    .append(risk.getResidualRisk() != null ? risk.getResidualRisk().name() : "-");
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static CapaPriority priorityForRisk(com.a9.arbor.grc.modules.risk.domain.PriorityLevel level) {
        if (level == null) {
            return CapaPriority.MEDIUM;
        }
        return switch (level) {
            case CRITICAL -> CapaPriority.CRITICAL;
            case HIGH -> CapaPriority.HIGH;
            case MEDIUM -> CapaPriority.MEDIUM;
            case LOW -> CapaPriority.LOW;
        };
    }

    @Transactional
    public CapaResponse assign(User admin, long id, AssignCapaRequest req) {
        assertAdmin(admin);
        Capa c = require(id);
        if (c.getStatus() == CapaStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CAPA is closed");
        }
        User owner = userRepository.findById(req.ownerUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner user not found"));
        if (owner.getRoles().contains(Role.EXTERNAL_AUDITOR)
                && owner.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN || r == Role.CONTRIBUTOR
                || r == Role.INTERNAL_AUDITOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "External Auditor cannot be CAPA action owner");
        }
        if (!owner.getRoles().contains(Role.CONTRIBUTOR)
                && !owner.getRoles().contains(Role.ADMIN)
                && !owner.getRoles().contains(Role.OWNER)
                && !owner.getRoles().contains(Role.INTERNAL_AUDITOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CAPA owner must be Contributor, Admin, Owner, or Internal Auditor");
        }

        c.setOwnerUserId(owner.getId());
        c.setAssignedByUserId(admin.getId());
        c.setAssignedAt(Instant.now()); // Excel col J: Date Assigned
        if (req.contributorDepartment() != null) {
            c.setContributorDepartment(blankToNull(req.contributorDepartment()));
        }
        if (req.targetDate() != null) {
            DateRules.requireActionDueDateIfPresent(req.targetDate(), "Target date");
            c.setTargetDate(req.targetDate());
        }
        c.setStatus(CapaStatus.OPEN);
        c.setRejectionReason(null);
        openContributorWork(admin, c, owner);
        audit(admin, AuditActionType.UPDATED, c);
        return toResponse(c);
    }

    @Transactional
    public CapaResponse updateTracker(User admin, long id, UpdateCapaTrackerRequest req) {
        assertAdmin(admin);
        Capa c = require(id);
        if (c.getStatus() == CapaStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed CAPA cannot be edited");
        }
        boolean trackerCore = req.correction() != null || req.rootCause() != null
                || req.correctiveActionPlan() != null || req.agreedTargetDate() != null
                || req.contributorDepartment() != null
                || req.clauseRef() != null || req.evidenceRequired() != null
                || req.assignedContributorUserId() != null;
        if (trackerCore && c.getStatus() == CapaStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CAPA plan cannot be edited after the contributor submitted a response");
        }
        // Excel E: Clause Ref
        if (req.clauseRef() != null) {
            c.setControlRef(blankToNull(req.clauseRef()));
        }
        // Excel G: Correction (immediate fix)
        if (req.correction() != null) {
            c.setCorrection(blankToNull(req.correction()));
            c.setRecommendedAction(blankToNull(req.correction()));
        }
        // Excel H: Root Cause
        if (req.rootCause() != null) {
            c.setRootCause(blankToNull(req.rootCause()));
        }
        // Excel I: Corrective Action Plan
        if (req.correctiveActionPlan() != null) {
            c.setCorrectiveAction(blankToNull(req.correctiveActionPlan()));
        }
        // Excel K: Agreed Target Date
        if (req.agreedTargetDate() != null) {
            DateRules.requireActionDueDateIfPresent(req.agreedTargetDate(), "Agreed target date");
            c.setTargetDate(req.agreedTargetDate());
        }
        // Excel L: Assigned Contributor. A changed assignee gets a new contributor work item.
        boolean openedWork = false;
        if (req.assignedContributorUserId() != null) {
            User owner = requireAssignableContributor(req.assignedContributorUserId());
            boolean ownerChanged = c.getOwnerUserId() == null || !c.getOwnerUserId().equals(owner.getId());
            if (ownerChanged || c.getWorkTaskId() == null) {
                c.setOwnerUserId(owner.getId());
                c.setAssignedByUserId(admin.getId());
                c.setAssignedAt(Instant.now());
                openContributorWork(admin, c, owner);
                openedWork = true;
            }
        }
        // Excel M: Contributor Department
        if (req.contributorDepartment() != null) {
            c.setContributorDepartment(blankToNull(req.contributorDepartment()));
        }
        // Excel N: Evidence Required
        if (req.evidenceRequired() != null) {
            c.setEvidenceRequired(blankToNull(req.evidenceRequired()));
        }
        // Excel Q: Review Remarks (Admin)
        if (req.reviewRemarks() != null) {
            c.setReviewRemarks(blankToNull(req.reviewRemarks()));
        }
        // Excel R: Evidence of Completion (Admin)
        if (req.evidenceOfCompletion() != null) {
            c.setEvidenceOfCompletion(blankToNull(req.evidenceOfCompletion()));
        }
        // Excel S: Verification Date (Admin)
        if (req.verificationDate() != null) {
            c.setVerificationDate(req.verificationDate());
        }
        // Excel T: Verification Result (Admin)
        if (req.verificationResult() != null) {
            c.setVerificationResult(blankToNull(req.verificationResult()));
        }
        if (trackerCore) {
            requireAdminTrackerFields(c);
        }
        if (!openedWork && c.getWorkTaskId() != null && req.agreedTargetDate() != null) {
            workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                if (t.getStatus() != WorkTaskStatus.CANCELLED && t.getStatus() != WorkTaskStatus.SUBMITTED) {
                    t.setDueDate(c.getTargetDate());
                    workTaskRepository.save(t);
                }
            });
        }
        capaRepository.save(c);
        audit(admin, AuditActionType.UPDATED, c);
        return toResponse(c);
    }

    @Transactional
    public CapaResponse updateProgress(User user, long id, UpdateCapaProgressRequest req) {
        Capa c = require(id);
        assertAssignee(user, c);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CAPA not editable in current status");
        }
        if (c.getOwnerUserId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Assign an owner before progress updates");
        }
        if (req.rootCause() != null) {
            c.setRootCause(blankToNull(req.rootCause()));
        }
        if (req.correctiveAction() != null) {
            c.setCorrectiveAction(blankToNull(req.correctiveAction()));
        }
        if (req.preventiveAction() != null) {
            c.setPreventiveAction(blankToNull(req.preventiveAction()));
        }
        if (req.progressNotes() != null) {
            c.setProgressNotes(blankToNull(req.progressNotes()));
        }
        c.setStatus(CapaStatus.IN_PROGRESS);
        capaRepository.save(c);
        if (c.getWorkTaskId() != null) {
            workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                if (t.getStatus() != WorkTaskStatus.SUBMITTED && t.getStatus() != WorkTaskStatus.CANCELLED) {
                    t.setStatus(WorkTaskStatus.IN_PROGRESS);
                    workTaskRepository.save(t);
                }
            });
        }
        return toResponse(c);
    }

    /**
     * Contributor submits their response (Excel cols O-P).
     */
    @Transactional
    public CapaResponse submitContributorResponse(User user, long id, SubmitCapaResponseRequest req) {
        Capa c = require(id);
        assertAssignee(user, c);
        if (c.getStatus() == CapaStatus.CLOSED || c.getStatus() == CapaStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CAPA not editable in current status");
        }
        // Excel O: Response Description (Contributor)
        if (req.responseDescription() != null) {
            c.setResponseDescription(blankToNull(req.responseDescription()));
        }
        // Excel P: Upload Evidence note (Contributor)
        if (req.contributorEvidenceNote() != null) {
            c.setContributorEvidenceNote(blankToNull(req.contributorEvidenceNote()));
        }
        c.setStatus(CapaStatus.IN_PROGRESS);
        capaRepository.save(c);
        audit(user, AuditActionType.UPDATED, c);
        return toResponse(c);
    }

    @Transactional
    public CapaResponse submit(User user, long id) {
        Capa c = require(id);
        assertAssignee(user, c);
        if (c.getStatus() == CapaStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CAPA is closed");
        }
        if (c.getRootCause() == null || c.getRootCause().isBlank()
                || c.getCorrectiveAction() == null || c.getCorrectiveAction().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Root cause and corrective action are required before submit");
        }
        requireLinkedEvidence(c.getId());
        c.setStatus(CapaStatus.PENDING_VERIFICATION);
        c.setRejectionReason(null);
        capaRepository.save(c);
        if (c.getWorkTaskId() != null) {
            workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                t.setStatus(WorkTaskStatus.SUBMITTED);
                t.setSubmittedAt(Instant.now());
                workTaskRepository.save(t);
            });
        }
        notificationService.trigger("CAPA_SUBMITTED", String.valueOf(c.getId()),
                adminRecipientIds(), baseData(c));
        audit(user, AuditActionType.UPDATED, c);
        return toResponse(c);
    }

    @Transactional
    public CapaResponse close(User admin, long id) {
        assertAdmin(admin);
        Capa c = require(id);
        if (c.getStatus() != CapaStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CAPA can be closed only after the contributor submits a response");
        }
        requireFilled(c.getReviewRemarks(), "Corrective Action plan review remarks");
        if (c.getVerificationDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification Date is required");
        }
        if (!isVerificationEffective(c.getVerificationResult())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verification Result must be Effective to close");
        }
        c.setStatus(CapaStatus.CLOSED);
        c.setClosedAt(Instant.now());
        c.setClosedByUserId(admin.getId());
        c.setRejectionReason(null);
        capaRepository.save(c);
        if (c.getWorkTaskId() != null) {
            workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                t.setStatus(WorkTaskStatus.CANCELLED);
                workTaskRepository.save(t);
            });
        }
        List<String> recipients = new java.util.ArrayList<>(adminRecipientIds());
        if (c.getOwnerUserId() != null) {
            String ownerId = String.valueOf(c.getOwnerUserId());
            if (!recipients.contains(ownerId)) {
                recipients.add(ownerId);
            }
        }
        notificationService.trigger("CAPA_CLOSED", String.valueOf(c.getId()), recipients, baseData(c));
        audit(admin, AuditActionType.APPROVED, c);
        return toResponse(c);
    }

    @Transactional
    public CapaResponse reject(User admin, long id, RejectCapaRequest req) {
        assertAdmin(admin);
        Capa c = require(id);
        if (c.getStatus() != CapaStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending verification CAPAs can be rejected");
        }
        c.setStatus(CapaStatus.IN_PROGRESS);
        c.setRejectionReason(req.reason().trim());
        // Clear Q-T so Status does not keep a stale Effective after return.
        c.setReviewRemarks(null);
        c.setEvidenceOfCompletion(null);
        c.setVerificationDate(null);
        c.setVerificationResult(null);
        capaRepository.save(c);

        CapaReturnEvent event = new CapaReturnEvent();
        event.setCapaId(c.getId());
        event.setRejectedAt(Instant.now());
        event.setRejectedByUserId(admin.getId());
        event.setReason(req.reason().trim());
        capaReturnEventRepository.save(event);

        if (c.getWorkTaskId() != null) {
            workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                t.setStatus(WorkTaskStatus.IN_PROGRESS);
                t.setSubmittedAt(null);
                workTaskRepository.save(t);
            });
        }
        if (c.getOwnerUserId() != null) {
            Map<String, String> data = baseData(c);
            data.put("reason", req.reason().trim());
            notificationService.trigger("CAPA_REJECTED", String.valueOf(c.getId()),
                    List.of(String.valueOf(c.getOwnerUserId())), data);
        }
        audit(admin, AuditActionType.REJECTED, c);
        return toResponse(c);
    }

    public Capa require(long id) {
        return capaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CAPA not found"));
    }

    public static CapaPriority priorityFor(FindingSeverity severity) {
        return switch (severity) {
            case MAJOR_NC -> CapaPriority.CRITICAL;
            case MINOR_NC -> CapaPriority.HIGH;
            case OBSERVATION -> CapaPriority.MEDIUM;
            case OFI -> CapaPriority.LOW;
        };
    }

    public static CapaPriority priorityForGap(GapImplementationStatus status) {
        return switch (status) {
            case NOT_IMPLEMENTED -> CapaPriority.HIGH;
            case PARTIALLY_IMPLEMENTED, PLANNED -> CapaPriority.MEDIUM;
            case FULLY_IMPLEMENTED -> CapaPriority.LOW;
        };
    }

    private void requireBcmsLicensed() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()
                || !moduleLicensingService.isEnabled(tenantId, ModuleCode.BCMS)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BCMS module is not licensed for this tenant");
        }
    }

    private static String buildGapDescription(GapItem item) {
        StringBuilder sb = new StringBuilder();
        if (item.getGapDescription() != null && !item.getGapDescription().isBlank()) {
            sb.append(item.getGapDescription().trim());
        }
        if (item.getObservations() != null && !item.getObservations().isBlank()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("Observations: ").append(item.getObservations().trim());
        }
        if (sb.isEmpty()) {
            GapImplementationStatus effective = item.effectiveImplementationStatus();
            return effective != null ? "Gap identified: " + effective.name().replace('_', ' ') : null;
        }
        return sb.toString();
    }

    /** BCMS and audit registers follow those modules. Gap and other CAPA stay with the register shell. */
    private boolean capaVisible(Capa capa) {
        if (capa.getRegisterCode() == CapaRegister.BCMS) {
            return adminModuleAccessService.mayUse(ModuleCode.BCMS, false);
        }
        if (capa.getRegisterCode() == CapaRegister.INTERNAL_AUDIT
                || capa.getRegisterCode() == CapaRegister.EXTERNAL_AUDIT) {
            return adminModuleAccessService.mayUse(ModuleCode.AUDIT, false);
        }
        return true;
    }

    private boolean isExternalOnly(User user) {
        return user.getRoles().contains(Role.EXTERNAL_AUDITOR)
                && user.getRoles().stream().noneMatch(r ->
                r == Role.OWNER || r == Role.ADMIN || r == Role.INTERNAL_AUDITOR || r == Role.GLOBAL_READER);
    }

    private void requireLinkedEvidence(long capaId) {
        int n = evidenceService.countLinksForRecord(MODULE_CAPA, TYPE_CAPA, String.valueOf(capaId));
        if (n < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CAPA submit requires at least one linked evidence file");
        }
    }

    private void assertAdmin(User user) {
        if (user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private User requireAssignableContributor(Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contributor user not found"));
        if (owner.getRoles().contains(Role.EXTERNAL_AUDITOR)
                && owner.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN || r == Role.CONTRIBUTOR
                || r == Role.INTERNAL_AUDITOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "External Auditor cannot be CAPA action owner");
        }
        if (!owner.getRoles().contains(Role.CONTRIBUTOR)
                && !owner.getRoles().contains(Role.ADMIN)
                && !owner.getRoles().contains(Role.OWNER)
                && !owner.getRoles().contains(Role.INTERNAL_AUDITOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CAPA owner must be Contributor, Admin, Owner, or Internal Auditor");
        }
        return owner;
    }

    /** Opens the contributor queue item. Cancels a previous item when the assignee changes. */
    private void openContributorWork(User admin, Capa c, User owner) {
        if (c.getWorkTaskId() != null) {
            workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                t.setStatus(WorkTaskStatus.CANCELLED);
                workTaskRepository.save(t);
            });
        }
        WorkTask task = new WorkTask();
        task.setSourceModule(MODULE_CAPA);
        task.setSourceRecordType(TYPE_CAPA);
        task.setSourceRecordId(c.getId());
        task.setTitle("CAPA: " + c.getTitle());
        task.setDetail(c.getControlRef() != null ? "Control " + c.getControlRef() : c.getRegisterCode().name());
        task.setAssignedToUserId(owner.getId());
        task.setAssignedByUserId(admin.getId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(c.getTargetDate());
        workTaskRepository.save(task);
        c.setWorkTaskId(task.getId());
        capaRepository.save(c);

        Map<String, String> data = baseData(c);
        data.put("targetDate", c.getTargetDate() != null ? c.getTargetDate().toString() : "-");
        notificationService.trigger("CAPA_ASSIGNED", String.valueOf(c.getId()),
                List.of(String.valueOf(owner.getId())), data);
    }

    private void applyExternalFindingCode(Capa c, String raw, Long registerId) {
        String code = blankToNull(raw);
        if (code == null) {
            c.setExternalFindingCode(null);
            return;
        }
        if (code.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finding ID must be 80 characters or less");
        }
        boolean taken = capaRepository.findByExternalFindingCodeIgnoreCase(code).stream()
                .anyMatch(other -> (c.getId() == null || !other.getId().equals(c.getId()))
                        && Objects.equals(other.getCapaRegisterId(), registerId));
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A CAPA with finding ID \"" + code + "\" already exists");
        }
        c.setExternalFindingCode(code);
    }

    private void assertAssignee(User user, Capa c) {
        boolean owner = c.getOwnerUserId() != null && c.getOwnerUserId().equals(user.getId());
        if (!owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned contributor can respond");
        }
    }

    private void assertOwnerOrAdmin(User user, Capa c) {
        boolean admin = user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
        boolean owner = c.getOwnerUserId() != null && c.getOwnerUserId().equals(user.getId());
        if (!admin && !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not CAPA owner");
        }
        if (!admin && RoleAccess.isContributorOnly(user) && !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not CAPA owner");
        }
    }

    private void notifyAdmins(String trigger, Capa c) {
        notificationService.trigger(trigger, String.valueOf(c.getId()), adminRecipientIds(), baseData(c));
    }

    /** Notification engine resolves a user id or a role name, not an email address. */
    private List<String> adminRecipientIds() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN))
                .map(u -> String.valueOf(u.getId()))
                .distinct()
                .toList();
    }

    private Map<String, String> baseData(Capa c) {
        Map<String, String> data = new HashMap<>();
        data.put("title", c.getTitle());
        data.put("register", c.getRegisterCode().name());
        data.put("registerCode", registerLabel(c));
        data.put("controlRef", c.getControlRef() != null ? c.getControlRef() : "-");
        return data;
    }

    private static String registerLabel(Capa c) {
        if (c == null || c.getRegisterCode() == null) return "-";
        if (c.getRegisterCode() == CapaRegister.OTHER
                && c.getSourceOther() != null
                && !c.getSourceOther().isBlank()) {
            return c.getSourceOther().trim();
        }
        return switch (c.getRegisterCode()) {
            case GAP -> "Gap";
            case INTERNAL_AUDIT -> "Internal audit";
            case EXTERNAL_AUDIT -> "External audit";
            case BCMS -> "BCMS";
            case OTHER -> "Other";
        };
    }

    /** Internal, External, or a typed Other source. Other requires the text. */
    private static void applyManualSource(Capa c, CreateCapaRequest req) {
        CapaRegister register = req.registerCode() != null ? req.registerCode() : CapaRegister.INTERNAL_AUDIT;
        if (register != CapaRegister.OTHER) {
            c.setRegisterCode(register);
            c.setSourceOther(null);
            return;
        }
        String other = req.sourceOther() == null ? "" : req.sourceOther().trim();
        if (other.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a source");
        }
        if (other.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source must be 120 characters or fewer");
        }
        c.setRegisterCode(CapaRegister.OTHER);
        c.setSourceOther(other);
    }

    private void audit(User user, AuditActionType action, Capa c) {
        auditTrailService.record(
                action, "CAPA", user.getId(), rolesSnap(user),
                "CAPA", String.valueOf(c.getId()), c.getTitle(), null, null, null, null, null);
    }

    private static String rolesSnap(User user) {
        return user.getRoles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
    }

    private static void requireAdminTrackerFields(Capa c) {
        requireFilled(c.getCorrection(), "Correction (the immediate fix)");
        requireFilled(c.getRootCause(), "Root Cause");
        requireFilled(c.getCorrectiveAction(), "Corrective Action Plan");
        if (c.getTargetDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agreed Target Date is required");
        }
        requireFilled(c.getContributorDepartment(), "Contributor Department");
    }

    private static boolean isVerificationEffective(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String n = value.trim().toUpperCase().replace('_', ' ');
        return "EFFECTIVE".equals(n) || "PASS".equals(n);
    }

    private static void requireFilled(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " is required");
        }
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private List<CapaResponse> toResponses(List<Capa> rows) {
        Set<Long> findingIds = rows.stream()
                .filter(c -> c.getSourceType() == CapaSourceType.AUDIT_FINDING && c.getSourceRecordId() != null)
                .map(Capa::getSourceRecordId)
                .collect(Collectors.toSet());
        Map<Long, GrcAuditFinding> findings = findingIds.isEmpty()
                ? Map.of()
                : auditFindingRepository.findAllById(findingIds).stream()
                        .collect(Collectors.toMap(GrcAuditFinding::getId, f -> f));
        Set<Long> auditIds = findings.values().stream()
                .map(GrcAuditFinding::getAuditId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(java.util.HashSet::new));
        rows.stream().map(Capa::getAuditId).filter(Objects::nonNull).forEach(auditIds::add);
        Map<Long, GrcAudit> audits = auditIds.isEmpty()
                ? Map.of()
                : auditRepository.findAllById(auditIds).stream()
                        .collect(Collectors.toMap(GrcAudit::getId, a -> a));
        Map<Long, List<CapaReturnHistoryItem>> historyByCapa = returnHistoryByCapaIds(
                rows.stream().map(Capa::getId).filter(Objects::nonNull).toList());
        return rows.stream()
                .map(c -> toResponse(c, findings, audits, historyByCapa.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    private CapaResponse toResponse(Capa c) {
        List<CapaReturnHistoryItem> history = returnHistoryFor(c.getId());
        if (c.getAuditId() != null && (c.getSourceType() != CapaSourceType.AUDIT_FINDING || c.getSourceRecordId() == null)) {
            GrcAudit audit = auditRepository.findById(c.getAuditId()).orElse(null);
            return CapaResponse.from(c, null, c.getAuditId(), audit != null ? audit.getName() : null, history);
        }
        if (c.getSourceType() != CapaSourceType.AUDIT_FINDING || c.getSourceRecordId() == null) {
            return CapaResponse.from(c, null, null, null, history);
        }
        return auditFindingRepository.findById(c.getSourceRecordId())
                .map(f -> {
                    GrcAudit audit = auditRepository.findById(f.getAuditId()).orElse(null);
                    return CapaResponse.from(
                            c,
                            f.getDisplayCode(),
                            f.getAuditId(),
                            audit != null ? audit.getName() : null,
                            history);
                })
                .orElseGet(() -> CapaResponse.from(c, null, null, null, history));
    }

    private static CapaResponse toResponse(Capa c,
                                             Map<Long, GrcAuditFinding> findings,
                                             Map<Long, GrcAudit> audits,
                                             List<CapaReturnHistoryItem> history) {
        if (c.getSourceType() != CapaSourceType.AUDIT_FINDING || c.getSourceRecordId() == null) {
            GrcAudit linked = c.getAuditId() != null ? audits.get(c.getAuditId()) : null;
            return CapaResponse.from(
                    c,
                    null,
                    c.getAuditId(),
                    linked != null ? linked.getName() : null,
                    history);
        }
        GrcAuditFinding finding = findings.get(c.getSourceRecordId());
        if (finding == null) {
            return CapaResponse.from(c, null, null, null, history);
        }
        GrcAudit audit = audits.get(finding.getAuditId());
        return CapaResponse.from(
                c,
                finding.getDisplayCode(),
                finding.getAuditId(),
                audit != null ? audit.getName() : null,
                history);
    }

    private List<CapaReturnHistoryItem> returnHistoryFor(Long capaId) {
        if (capaId == null) {
            return List.of();
        }
        return toHistoryItems(capaReturnEventRepository.findByCapaIdOrderByRejectedAtDescIdDesc(capaId));
    }

    private Map<Long, List<CapaReturnHistoryItem>> returnHistoryByCapaIds(List<Long> capaIds) {
        if (capaIds == null || capaIds.isEmpty()) {
            return Map.of();
        }
        List<CapaReturnEvent> events = capaReturnEventRepository.findByCapaIdInOrderByRejectedAtDescIdDesc(capaIds);
        Map<Long, String> names = userNamesFor(events.stream().map(CapaReturnEvent::getRejectedByUserId).toList());
        Map<Long, List<CapaReturnHistoryItem>> out = new HashMap<>();
        for (CapaReturnEvent e : events) {
            out.computeIfAbsent(e.getCapaId(), k -> new java.util.ArrayList<>())
                    .add(toHistoryItem(e, names.get(e.getRejectedByUserId())));
        }
        return out;
    }

    private List<CapaReturnHistoryItem> toHistoryItems(List<CapaReturnEvent> events) {
        Map<Long, String> names = userNamesFor(events.stream().map(CapaReturnEvent::getRejectedByUserId).toList());
        return events.stream()
                .map(e -> toHistoryItem(e, names.get(e.getRejectedByUserId())))
                .toList();
    }

    private Map<Long, String> userNamesFor(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));
    }

    private static CapaReturnHistoryItem toHistoryItem(CapaReturnEvent e, String name) {
        return new CapaReturnHistoryItem(
                e.getId(),
                e.getRejectedAt(),
                e.getRejectedByUserId(),
                name != null && !name.isBlank() ? name : ("User #" + e.getRejectedByUserId()),
                e.getReason());
    }
}
