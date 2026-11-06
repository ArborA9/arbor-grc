package com.a9.arbor.grc.modules.workflow.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.workflow.domain.WorkflowDefinition;
import com.a9.arbor.grc.modules.workflow.domain.WorkflowInstance;
import com.a9.arbor.grc.modules.workflow.domain.WorkflowInstanceStatus;
import com.a9.arbor.grc.modules.workflow.domain.WorkflowTransitionDef;
import com.a9.arbor.grc.modules.workflow.domain.WorkflowTransitionLog;
import com.a9.arbor.grc.modules.workflow.repo.WorkflowDefinitionRepository;
import com.a9.arbor.grc.modules.workflow.repo.WorkflowInstanceRepository;
import com.a9.arbor.grc.modules.workflow.repo.WorkflowTransitionDefRepository;
import com.a9.arbor.grc.modules.workflow.repo.WorkflowTransitionLogRepository;
import com.a9.arbor.grc.modules.workflow.web.dto.StartWorkflowRequest;
import com.a9.arbor.grc.modules.workflow.web.dto.TransitionWorkflowRequest;
import com.a9.arbor.grc.modules.workflow.web.dto.WorkflowDefinitionResponse;
import com.a9.arbor.grc.modules.workflow.web.dto.WorkflowInstanceResponse;
import com.a9.arbor.grc.modules.workflow.web.dto.WorkflowTransitionDefResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowTransitionDefRepository transitionDefRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTransitionLogRepository logRepository;
    private final AuditTrailService auditTrailService;
    private final AdminModuleAccessService adminModuleAccessService;

    public WorkflowService(WorkflowDefinitionRepository definitionRepository,
                           WorkflowTransitionDefRepository transitionDefRepository,
                           WorkflowInstanceRepository instanceRepository,
                           WorkflowTransitionLogRepository logRepository,
                           AuditTrailService auditTrailService,
                           AdminModuleAccessService adminModuleAccessService) {
        this.definitionRepository = definitionRepository;
        this.transitionDefRepository = transitionDefRepository;
        this.instanceRepository = instanceRepository;
        this.logRepository = logRepository;
        this.auditTrailService = auditTrailService;
        this.adminModuleAccessService = adminModuleAccessService;
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinitionResponse> listDefinitions() {
        return definitionRepository.findByActiveTrueOrderByCodeAsc().stream()
                .filter(d -> adminModuleAccessService.mayUseRaw(d.getModuleCode(), false))
                .map(d -> WorkflowDefinitionResponse.from(
                        d, transitionDefRepository.findByDefinitionId(d.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<WorkflowInstanceResponse> listInstances(String statusRaw, User user, Pageable pageable) {
        Page<WorkflowInstance> page;
        if (statusRaw == null || statusRaw.isBlank()) {
            page = instanceRepository.findAllByOrderByStartedAtDesc(pageable);
        } else {
            WorkflowInstanceStatus status = parseStatus(statusRaw);
            page = instanceRepository.findByStatusOrderByStartedAtDesc(status, pageable);
        }
        List<WorkflowInstanceResponse> visible = page.getContent().stream()
                .filter(i -> adminModuleAccessService.mayUseRaw(i.getModuleCode(), false))
                .map(i -> toResponse(i, user))
                .toList();
        return new PageImpl<>(visible, pageable, visible.size());
    }

    @Transactional(readOnly = true)
    public WorkflowInstanceResponse getInstance(long id, User user) {
        WorkflowInstance i = instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
        if (!adminModuleAccessService.mayUseRaw(i.getModuleCode(), false)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found");
        }
        return toResponse(i, user);
    }

    @Transactional
    public WorkflowInstanceResponse start(User user, StartWorkflowRequest req) {
        WorkflowDefinition def = definitionRepository
                .findFirstByCodeAndActiveTrueOrderByVersionDesc(req.definitionCode().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));

        String module = blankTo(req.moduleCode(), def.getModuleCode());
        if (!adminModuleAccessService.mayUseRaw(module, true)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this module");
        }
        String recordType = blankTo(req.recordType(), def.getRecordType());
        String recordId = req.recordId().trim();

        List<WorkflowInstance> open = instanceRepository.findByModuleCodeAndRecordTypeAndRecordIdAndStatus(
                module, recordType, recordId, WorkflowInstanceStatus.OPEN);
        if (!open.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An open workflow already exists for this record");
        }

        WorkflowInstance i = new WorkflowInstance();
        i.setDefinitionId(def.getId());
        i.setModuleCode(module);
        i.setRecordType(recordType);
        i.setRecordId(recordId);
        i.setRecordName(req.recordName() == null || req.recordName().isBlank() ? recordId : req.recordName().trim());
        i.setCurrentState(def.getInitialState());
        i.setStatus(WorkflowInstanceStatus.OPEN);
        i.setStartedByUserId(user.getId());
        i = instanceRepository.save(i);

        auditTrailService.record(
                AuditActionType.CREATED,
                "WORKFLOW",
                user.getId(),
                primaryRole(user),
                "WORKFLOW_INSTANCE",
                String.valueOf(i.getId()),
                i.getRecordName(),
                "status",
                null,
                i.getCurrentState(),
                null,
                null);

        return toResponse(i, user);
    }

    @Transactional
    public WorkflowInstanceResponse transition(User user, long id, TransitionWorkflowRequest req) {
        WorkflowInstance i = instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found"));
        if (i.getStatus() != WorkflowInstanceStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow is not open");
        }

        String action = req.action().trim().toLowerCase(Locale.ROOT);
        WorkflowTransitionDef transition = transitionDefRepository
                .findByDefinitionIdAndFromStateAndActionCode(i.getDefinitionId(), i.getCurrentState(), action)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Action '" + action + "' is not allowed from state " + i.getCurrentState()));

        if (!roleAllowed(user, transition.getAllowedRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your role cannot perform this action");
        }
        if (transition.isRequiresComment() && (req.comment() == null || req.comment().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment is required for this action");
        }

        String from = i.getCurrentState();
        String to = transition.getToState();

        WorkflowTransitionLog log = new WorkflowTransitionLog();
        log.setInstanceId(i.getId());
        log.setFromState(from);
        log.setToState(to);
        log.setActionCode(action);
        log.setActorUserId(user.getId());
        log.setCommentText(req.comment() == null || req.comment().isBlank() ? null : req.comment().trim());
        logRepository.save(log);

        i.setCurrentState(to);
        if (isTerminal(to)) {
            i.setStatus(WorkflowInstanceStatus.COMPLETED);
            i.setCompletedAt(Instant.now());
        }
        instanceRepository.save(i);

        AuditActionType auditAction = switch (action) {
            case "approve" -> AuditActionType.APPROVED;
            case "reject" -> AuditActionType.REJECTED;
            default -> AuditActionType.UPDATED;
        };
        auditTrailService.record(
                auditAction,
                "WORKFLOW",
                user.getId(),
                primaryRole(user),
                "WORKFLOW_INSTANCE",
                String.valueOf(i.getId()),
                i.getRecordName(),
                "state",
                from,
                to,
                null,
                null);

        return toResponse(i, user);
    }

    private WorkflowInstanceResponse toResponse(WorkflowInstance i, User user) {
        WorkflowDefinition def = definitionRepository.findById(i.getDefinitionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing definition"));
        List<WorkflowTransitionDefResponse> available = List.of();
        if (i.getStatus() == WorkflowInstanceStatus.OPEN) {
            available = transitionDefRepository.findByDefinitionId(i.getDefinitionId()).stream()
                    .filter(t -> t.getFromState().equals(i.getCurrentState()))
                    .filter(t -> roleAllowed(user, t.getAllowedRoles()))
                    .map(WorkflowTransitionDefResponse::from)
                    .toList();
        }
        return WorkflowInstanceResponse.from(
                i,
                def.getCode(),
                def.getName(),
                available,
                logRepository.findByInstanceIdOrderByCreatedAtAsc(i.getId()));
    }

    private static boolean isTerminal(String state) {
        return "APPROVED".equalsIgnoreCase(state) || "CANCELLED".equalsIgnoreCase(state);
    }

    private static boolean roleAllowed(User user, String allowedRolesCsv) {
        Set<String> allowed = Arrays.stream(allowedRolesCsv.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return user.getRoles().stream().map(Enum::name).anyMatch(allowed::contains);
    }

    private static String primaryRole(User user) {
        return user.getRoles().stream().findFirst().map(Enum::name).orElse("UNKNOWN");
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static WorkflowInstanceStatus parseStatus(String raw) {
        try {
            return WorkflowInstanceStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status", ex);
        }
    }
}
