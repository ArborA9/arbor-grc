package com.a9.arbor.grc.modules.incident.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.incident.domain.IncidentPriority;
import com.a9.arbor.grc.modules.incident.domain.IncidentRecord;
import com.a9.arbor.grc.modules.incident.domain.IncidentSla;
import com.a9.arbor.grc.modules.incident.domain.IncidentStatus;
import com.a9.arbor.grc.modules.incident.repo.IncidentRecordRepository;
import com.a9.arbor.grc.modules.incident.web.dto.AssignIncidentRequest;
import com.a9.arbor.grc.modules.incident.web.dto.CreateIncidentRequest;
import com.a9.arbor.grc.modules.incident.web.dto.IncidentResponse;
import com.a9.arbor.grc.modules.incident.web.dto.ReportIncidentRequest;
import com.a9.arbor.grc.modules.incident.web.dto.ResolveIncidentRequest;
import com.a9.arbor.grc.modules.incident.web.dto.SubmitIncidentTaskRequest;
import com.a9.arbor.grc.modules.incident.web.dto.UpdateIncidentRequest;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
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
import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    private final IncidentRecordRepository incidentRepository;
    private final IncidentIdGenerator idGenerator;
    private final UserRepository userRepository;
    private final RiskRecordRepository riskRepository;
    private final GapAssessmentRepository gapAssessmentRepository;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final ModuleLicensingService moduleLicensingService;

    public IncidentService(IncidentRecordRepository incidentRepository,
                           IncidentIdGenerator idGenerator,
                           UserRepository userRepository,
                           RiskRecordRepository riskRepository,
                           GapAssessmentRepository gapAssessmentRepository,
                           WorkTaskRepository workTaskRepository,
                           NotificationService notificationService,
                           AuditTrailService auditTrailService,
                           ModuleLicensingService moduleLicensingService) {
        this.incidentRepository = incidentRepository;
        this.idGenerator = idGenerator;
        this.userRepository = userRepository;
        this.riskRepository = riskRepository;
        this.gapAssessmentRepository = gapAssessmentRepository;
        this.workTaskRepository = workTaskRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
        this.moduleLicensingService = moduleLicensingService;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(User user) {
        requireErm();
        List<IncidentRecord> rows;
        if (RoleAccess.isContributorOnly(user)) {
            rows = incidentRepository.findByOwnerUserIdOrReporterUserIdOrderByUpdatedAtDesc(
                    user.getId(), user.getId());
        } else {
            rows = incidentRepository.findAllByOrderByUpdatedAtDesc();
        }
        return rows.stream().map(IncidentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(User user, long id) {
        requireErm();
        return IncidentResponse.from(requireVisible(user, id));
    }

    @Transactional
    public IncidentResponse report(User user, ReportIncidentRequest req) {
        requireErm();
        Instant now = Instant.now();
        Instant occurred = req.occurredAt() != null ? req.occurredAt() : now;
        IncidentPriority priority = IncidentPriority.P3;

        IncidentRecord inc = new IncidentRecord();
        inc.setDisplayId(idGenerator.nextDisplayId());
        inc.setTitle(req.title().trim());
        inc.setIncidentType(req.incidentType());
        inc.setDescription(blankToNull(req.description()));
        inc.setOccurredAt(occurred);
        inc.setAffectedArea(blankToNull(req.affectedArea()));
        inc.setPriority(priority);
        inc.setStatus(IncidentStatus.REPORTED);
        inc.setReporterUserId(user.getId());
        inc.setCreatedByUserId(user.getId());
        applySla(inc, priority, now);
        incidentRepository.save(inc);

        notifyReported(inc);
        audit(user, AuditActionType.CREATED, inc);
        return IncidentResponse.from(inc);
    }

    @Transactional
    public IncidentResponse create(User admin, CreateIncidentRequest req) {
        requireErm();
        requireAdmin(admin);
        Instant now = Instant.now();
        Instant occurred = req.occurredAt() != null ? req.occurredAt() : now;
        if (req.linkedRiskId() != null) {
            requireRisk(req.linkedRiskId());
        }
        if (req.linkedGapAssessmentId() != null) {
            requireGapAssessment(req.linkedGapAssessmentId());
        }
        if (req.ownerUserId() != null) {
            requireUser(req.ownerUserId());
        }

        IncidentRecord inc = new IncidentRecord();
        inc.setDisplayId(idGenerator.nextDisplayId());
        inc.setTitle(req.title().trim());
        inc.setIncidentType(req.incidentType());
        inc.setDescription(blankToNull(req.description()));
        inc.setOccurredAt(occurred);
        inc.setAffectedArea(blankToNull(req.affectedArea()));
        inc.setPriority(req.priority());
        inc.setLinkedRiskId(req.linkedRiskId());
        inc.setLinkedGapAssessmentId(req.linkedGapAssessmentId());
        inc.setOrgUnitId(req.orgUnitId());
        inc.setRootCause(blankToNull(req.rootCause()));
        inc.setContainmentActions(blankToNull(req.containmentActions()));
        inc.setReporterUserId(admin.getId());
        inc.setCreatedByUserId(admin.getId());
        applySla(inc, req.priority(), now);

        if (req.ownerUserId() != null) {
            inc.setOwnerUserId(req.ownerUserId());
            inc.setStatus(IncidentStatus.ASSIGNED);
            incidentRepository.save(inc);
            ensureOwnerTask(inc, admin.getId());
            notifyAssigned(inc);
        } else {
            inc.setStatus(IncidentStatus.REPORTED);
            incidentRepository.save(inc);
        }

        notifyReported(inc);
        audit(admin, AuditActionType.CREATED, inc);
        return IncidentResponse.from(inc);
    }

    @Transactional
    public IncidentResponse update(User admin, long id, UpdateIncidentRequest req) {
        requireErm();
        requireAdmin(admin);
        IncidentRecord inc = require(id);
        if (inc.getStatus() == IncidentStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed incident cannot be edited");
        }
        if (req.title() != null && !req.title().isBlank()) {
            inc.setTitle(req.title().trim());
        }
        if (req.incidentType() != null) {
            inc.setIncidentType(req.incidentType());
        }
        if (req.description() != null) {
            inc.setDescription(blankToNull(req.description()));
        }
        if (req.occurredAt() != null) {
            inc.setOccurredAt(req.occurredAt());
        }
        if (req.affectedArea() != null) {
            inc.setAffectedArea(blankToNull(req.affectedArea()));
        }
        if (req.priority() != null && req.priority() != inc.getPriority()) {
            inc.setPriority(req.priority());
            applySla(inc, req.priority(), inc.getCreatedAt() != null ? inc.getCreatedAt() : Instant.now());
            inc.setOverdueNotifiedAt(null);
        }
        if (req.linkedRiskId() != null) {
            if (req.linkedRiskId() == 0L) {
                inc.setLinkedRiskId(null);
            } else {
                requireRisk(req.linkedRiskId());
                inc.setLinkedRiskId(req.linkedRiskId());
            }
        }
        if (req.linkedGapAssessmentId() != null) {
            if (req.linkedGapAssessmentId() == 0L) {
                inc.setLinkedGapAssessmentId(null);
            } else {
                requireGapAssessment(req.linkedGapAssessmentId());
                inc.setLinkedGapAssessmentId(req.linkedGapAssessmentId());
            }
        }
        if (req.orgUnitId() != null) {
            inc.setOrgUnitId(req.orgUnitId() == 0L ? null : req.orgUnitId());
        }
        if (req.rootCause() != null) {
            inc.setRootCause(blankToNull(req.rootCause()));
        }
        if (req.containmentActions() != null) {
            inc.setContainmentActions(blankToNull(req.containmentActions()));
        }
        if (req.resolutionNotes() != null) {
            inc.setResolutionNotes(blankToNull(req.resolutionNotes()));
        }
        incidentRepository.save(inc);
        audit(admin, AuditActionType.UPDATED, inc);
        return IncidentResponse.from(inc);
    }

    @Transactional
    public IncidentResponse assign(User admin, long id, AssignIncidentRequest req) {
        requireErm();
        requireAdmin(admin);
        IncidentRecord inc = require(id);
        if (inc.getStatus() == IncidentStatus.CLOSED || inc.getStatus() == IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot assign a resolved/closed incident");
        }
        requireUser(req.userId());
        inc.setOwnerUserId(req.userId());
        inc.setStatus(IncidentStatus.ASSIGNED);
        ensureOwnerTask(inc, admin.getId());
        incidentRepository.save(inc);
        notifyAssigned(inc);
        audit(admin, AuditActionType.UPDATED, inc);
        return IncidentResponse.from(inc);
    }

    @Transactional
    public IncidentResponse resolve(User user, long id, ResolveIncidentRequest req) {
        requireErm();
        IncidentRecord inc = requireVisible(user, id);
        boolean admin = isAdmin(user);
        if (!admin && !user.getId().equals(inc.getOwnerUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner or Admin may resolve");
        }
        if (inc.getStatus() == IncidentStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incident already closed");
        }
        if (inc.getStatus() == IncidentStatus.RESOLVED) {
            return IncidentResponse.from(inc);
        }
        applyResolve(inc, user, req != null ? req.resolutionNotes() : null,
                req != null ? req.rootCause() : null,
                req != null ? req.containmentActions() : null);
        incidentRepository.save(inc);
        completeOpenTask(inc);
        notifyResolved(inc);
        audit(user, AuditActionType.UPDATED, inc);
        return IncidentResponse.from(inc);
    }

    @Transactional
    public IncidentResponse close(User admin, long id) {
        requireErm();
        requireAdmin(admin);
        IncidentRecord inc = require(id);
        if (inc.getStatus() == IncidentStatus.CLOSED) {
            return IncidentResponse.from(inc);
        }
        if (inc.getStatus() != IncidentStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resolve incident before closing");
        }
        inc.setStatus(IncidentStatus.CLOSED);
        inc.setClosedAt(Instant.now());
        inc.setClosedByUserId(admin.getId());
        completeOpenTask(inc);
        incidentRepository.save(inc);
        notifyClosed(inc);
        audit(admin, AuditActionType.UPDATED, inc);
        return IncidentResponse.from(inc);
    }

    /** Used by My Tasks submit. */
    @Transactional
    public IncidentResponse submitFromTask(User user, long incidentId, SubmitIncidentTaskRequest req) {
        requireErm();
        IncidentRecord inc = requireVisible(user, incidentId);
        if (!user.getId().equals(inc.getOwnerUserId()) && !isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only incident owner may update this task");
        }
        if (inc.getStatus() == IncidentStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Incident is closed");
        }
        if (inc.getRespondedAt() == null) {
            inc.setRespondedAt(Instant.now());
        }
        if (req.progressNotes() != null && !req.progressNotes().isBlank()) {
            String existing = inc.getResolutionNotes();
            String note = req.progressNotes().trim();
            inc.setResolutionNotes(existing == null || existing.isBlank() ? note : existing + "\n" + note);
        }
        if (req.containmentActions() != null) {
            inc.setContainmentActions(blankToNull(req.containmentActions()));
        }
        if (req.rootCause() != null) {
            inc.setRootCause(blankToNull(req.rootCause()));
        }
        boolean resolve = Boolean.TRUE.equals(req.markResolved());
        if (resolve) {
            applyResolve(inc, user, null, null, null);
            completeOpenTask(inc);
            incidentRepository.save(inc);
            notifyResolved(inc);
        } else {
            incidentRepository.save(inc);
            // keep task open for further updates
            if (inc.getWorkTaskId() != null) {
                workTaskRepository.findById(inc.getWorkTaskId()).ifPresent(task -> {
                    if (task.getStatus() == WorkTaskStatus.PENDING) {
                        task.setStatus(WorkTaskStatus.IN_PROGRESS);
                        workTaskRepository.save(task);
                    }
                });
            }
        }
        audit(user, AuditActionType.UPDATED, inc);
        return IncidentResponse.from(inc);
    }

    public void ensureOwnerTask(IncidentRecord inc, Long assignedByUserId) {
        if (inc.getOwnerUserId() == null) {
            return;
        }
        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_INCIDENT, MyTasksService.TYPE_INCIDENT, inc.getId())
                .orElseGet(WorkTask::new);
        if (task.getId() != null && task.getStatus() == WorkTaskStatus.PENDING) {
            task.setAssignedToUserId(inc.getOwnerUserId());
            task.setDueDate(LocalDate.ofInstant(inc.getResponseDueAt(), java.time.ZoneOffset.UTC));
            workTaskRepository.save(task);
            inc.setWorkTaskId(task.getId());
            return;
        }
        if (task.getId() != null && task.getStatus() == WorkTaskStatus.SUBMITTED) {
            task.setStatus(WorkTaskStatus.PENDING);
            task.setSubmittedAt(null);
        }
        task.setSourceModule(MyTasksService.MODULE_INCIDENT);
        task.setSourceRecordType(MyTasksService.TYPE_INCIDENT);
        task.setSourceRecordId(inc.getId());
        task.setTitle(inc.getDisplayId() + ": " + inc.getTitle());
        task.setDetail("Incident " + inc.getPriority() + ": respond by "
                + inc.getResponseDueAt());
        task.setAssignedToUserId(inc.getOwnerUserId());
        task.setAssignedByUserId(assignedByUserId != null ? assignedByUserId : inc.getCreatedByUserId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(LocalDate.ofInstant(inc.getResponseDueAt(), java.time.ZoneOffset.UTC));
        workTaskRepository.save(task);
        inc.setWorkTaskId(task.getId());
    }

    private void applyResolve(IncidentRecord inc, User user, String resolutionNotes,
                              String rootCause, String containmentActions) {
        if (resolutionNotes != null) {
            inc.setResolutionNotes(blankToNull(resolutionNotes));
        }
        if (rootCause != null) {
            inc.setRootCause(blankToNull(rootCause));
        }
        if (containmentActions != null) {
            inc.setContainmentActions(blankToNull(containmentActions));
        }
        if (inc.getRespondedAt() == null) {
            inc.setRespondedAt(Instant.now());
        }
        inc.setStatus(IncidentStatus.RESOLVED);
        inc.setResolvedAt(Instant.now());
        inc.setResolvedByUserId(user.getId());
    }

    private void applySla(IncidentRecord inc, IncidentPriority priority, Instant from) {
        inc.setResponseDueAt(IncidentSla.responseDue(priority, from));
        inc.setResolutionDueAt(IncidentSla.resolutionDue(priority, from));
    }

    private void notifyReported(IncidentRecord inc) {
        String trigger = IncidentSla.reportedTrigger(inc.getPriority());
        List<String> recipients = new ArrayList<>();
        recipients.add("ADMIN");
        if (inc.getPriority() == IncidentPriority.P1) {
            recipients.add("OWNER");
        }
        notificationService.trigger(
                trigger,
                String.valueOf(inc.getId()),
                recipients,
                Map.of(
                        "displayId", inc.getDisplayId(),
                        "title", inc.getTitle(),
                        "responseDue", inc.getResponseDueAt().toString()));
    }

    private void notifyAssigned(IncidentRecord inc) {
        notificationService.trigger(
                "INCIDENT_ASSIGNED",
                String.valueOf(inc.getId()),
                List.of(String.valueOf(inc.getOwnerUserId())),
                Map.of(
                        "displayId", inc.getDisplayId(),
                        "title", inc.getTitle(),
                        "priority", inc.getPriority().name(),
                        "responseDue", inc.getResponseDueAt().toString()));
    }

    private void notifyResolved(IncidentRecord inc) {
        List<String> recipients = new ArrayList<>();
        recipients.add(String.valueOf(inc.getReporterUserId()));
        recipients.add("ADMIN");
        notificationService.trigger(
                "INCIDENT_RESOLVED",
                String.valueOf(inc.getId()),
                recipients,
                Map.of("displayId", inc.getDisplayId(), "title", inc.getTitle()));
    }

    private void notifyClosed(IncidentRecord inc) {
        List<String> recipients = new ArrayList<>();
        recipients.add(String.valueOf(inc.getReporterUserId()));
        if (inc.getOwnerUserId() != null) {
            recipients.add(String.valueOf(inc.getOwnerUserId()));
        }
        if (inc.getResolvedByUserId() != null) {
            recipients.add(String.valueOf(inc.getResolvedByUserId()));
        }
        recipients.add("ADMIN");
        recipients.add("OWNER");
        notificationService.trigger(
                "INCIDENT_CLOSED",
                String.valueOf(inc.getId()),
                recipients,
                Map.of("displayId", inc.getDisplayId(), "title", inc.getTitle()));
    }

    private void completeOpenTask(IncidentRecord inc) {
        if (inc.getWorkTaskId() == null) {
            return;
        }
        workTaskRepository.findById(inc.getWorkTaskId()).ifPresent(task -> {
            if (task.getStatus() != WorkTaskStatus.SUBMITTED && task.getStatus() != WorkTaskStatus.CANCELLED) {
                task.setStatus(WorkTaskStatus.SUBMITTED);
                task.setSubmittedAt(Instant.now());
                workTaskRepository.save(task);
            }
        });
    }

    private void requireErm() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()
                || !moduleLicensingService.isEnabled(tenantId, ModuleCode.ERM)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Module ERM is not licensed for this tenant");
        }
    }

    private static void requireAdmin(User user) {
        if (!isAdmin(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private static boolean isAdmin(User user) {
        return user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.OWNER);
    }

    private void requireUser(Long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found");
        }
    }

    private void requireRisk(Long riskId) {
        if (riskRepository.findById(riskId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked risk not found");
        }
    }

    private void requireGapAssessment(Long gapAssessmentId) {
        if (gapAssessmentRepository.findById(gapAssessmentId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Linked Gap assessment not found");
        }
    }

    private IncidentRecord require(long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found"));
    }

    private IncidentRecord requireVisible(User user, long id) {
        IncidentRecord inc = require(id);
        if (RoleAccess.isContributorOnly(user)
                && !user.getId().equals(inc.getOwnerUserId())
                && !user.getId().equals(inc.getReporterUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incident not visible to you");
        }
        return inc;
    }

    private void audit(User user, AuditActionType action, IncidentRecord inc) {
        String role = user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
        auditTrailService.record(
                action, "INCIDENT", user.getId(), role,
                "INCIDENT", String.valueOf(inc.getId()), inc.getDisplayId(),
                null, null, null, null, null);
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
