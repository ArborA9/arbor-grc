package com.a9.arbor.grc.modules.bcms.service;

import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.bcms.domain.BcmsExercise;
import com.a9.arbor.grc.modules.bcms.domain.BcmsExerciseParticipant;
import com.a9.arbor.grc.modules.bcms.domain.BcmsExercisePlan;
import com.a9.arbor.grc.modules.bcms.domain.ExerciseStatus;
import com.a9.arbor.grc.modules.bcms.repo.BcmsExerciseParticipantRepository;
import com.a9.arbor.grc.modules.bcms.repo.BcmsExercisePlanRepository;
import com.a9.arbor.grc.modules.bcms.repo.BcmsExerciseRepository;
import com.a9.arbor.grc.modules.bcms.repo.RecoveryPlanRepository;
import com.a9.arbor.grc.modules.bcms.web.dto.BcmsExerciseParticipantResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BcmsExerciseResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.CompleteExerciseRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.CreateExerciseRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.ExerciseParticipantRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.UpdateExerciseRequest;
import com.a9.arbor.grc.modules.capa.service.CapaService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class ExerciseService {

    private final BcmsExerciseRepository exerciseRepository;
    private final BcmsExercisePlanRepository exercisePlanRepository;
    private final BcmsExerciseParticipantRepository participantRepository;
    private final RecoveryPlanRepository recoveryPlanRepository;
    private final CapaService capaService;
    private final AuditTrailService auditTrailService;

    public ExerciseService(BcmsExerciseRepository exerciseRepository,
                           BcmsExercisePlanRepository exercisePlanRepository,
                           BcmsExerciseParticipantRepository participantRepository,
                           RecoveryPlanRepository recoveryPlanRepository,
                           CapaService capaService,
                           AuditTrailService auditTrailService) {
        this.exerciseRepository = exerciseRepository;
        this.exercisePlanRepository = exercisePlanRepository;
        this.participantRepository = participantRepository;
        this.recoveryPlanRepository = recoveryPlanRepository;
        this.capaService = capaService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<BcmsExerciseResponse> list(Long projectId) {
        List<BcmsExercise> rows = projectId != null
                ? exerciseRepository.findByProjectIdOrderByUpdatedAtDesc(projectId)
                : exerciseRepository.findAllByOrderByUpdatedAtDesc();
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BcmsExerciseResponse get(long id) {
        return toResponse(require(id));
    }

    @Transactional
    public BcmsExerciseResponse create(User admin, CreateExerciseRequest req) {
        assertAdmin(admin);
        BcmsExercise e = new BcmsExercise();
        e.setProjectId(req.projectId());
        e.setName(req.name().trim());
        e.setExerciseType(req.exerciseType());
        e.setScope(blankToNull(req.scope()));
        e.setFacilitatorUserId(req.facilitatorUserId());
        e.setObjectives(blankToNull(req.objectives()));
        DateRules.requireActionDueDateIfPresent(req.scheduledDate(), "Scheduled date");
        e.setScheduledDate(req.scheduledDate());
        e.setStatus(ExerciseStatus.PLANNED);
        e.setCreatedByUserId(admin.getId());
        exerciseRepository.save(e);

        syncPlans(e.getId(), req.planIds());
        syncParticipants(e.getId(), req.participants());

        audit(admin, AuditActionType.CREATED, e);
        return toResponse(e);
    }

    @Transactional
    public BcmsExerciseResponse update(User admin, long id, UpdateExerciseRequest req) {
        assertAdmin(admin);
        BcmsExercise e = require(id);
        if (e.getStatus() != ExerciseStatus.PLANNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only planned exercises can be edited");
        }
        e.setName(req.name().trim());
        e.setExerciseType(req.exerciseType());
        e.setScope(blankToNull(req.scope()));
        e.setFacilitatorUserId(req.facilitatorUserId());
        e.setObjectives(blankToNull(req.objectives()));
        DateRules.requireActionDueDateIfChanged(e.getScheduledDate(), req.scheduledDate(), "Scheduled date");
        e.setScheduledDate(req.scheduledDate());
        exerciseRepository.save(e);

        syncPlans(e.getId(), req.planIds());
        syncParticipants(e.getId(), req.participants());

        audit(admin, AuditActionType.UPDATED, e);
        return toResponse(e);
    }

    @Transactional
    public BcmsExerciseResponse complete(User admin, long id, CompleteExerciseRequest req) {
        assertAdmin(admin);
        BcmsExercise e = require(id);
        if (e.getStatus() != ExerciseStatus.PLANNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only planned exercises can be completed");
        }
        e.setScenariosTested(blankToNull(req.scenariosTested()));
        e.setResults(blankToNull(req.results()));
        e.setGapsIdentified(blankToNull(req.gapsIdentified()));
        e.setLessonsLearned(blankToNull(req.lessonsLearned()));
        e.setStatus(ExerciseStatus.COMPLETED);
        e.setCompletedAt(Instant.now());
        exerciseRepository.save(e);

        if (e.getGapsIdentified() != null && !e.getGapsIdentified().isBlank()) {
            capaService.createFromExerciseGap(admin, e.getId(), e.getName(), e.getGapsIdentified());
        }

        audit(admin, AuditActionType.UPDATED, e);
        return toResponse(e);
    }

    @Transactional
    public BcmsExerciseResponse cancel(User admin, long id) {
        assertAdmin(admin);
        BcmsExercise e = require(id);
        if (e.getStatus() != ExerciseStatus.PLANNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only planned exercises can be cancelled");
        }
        e.setStatus(ExerciseStatus.CANCELLED);
        exerciseRepository.save(e);
        audit(admin, AuditActionType.UPDATED, e);
        return toResponse(e);
    }

    private void syncPlans(long exerciseId, List<Long> planIds) {
        exercisePlanRepository.deleteByExerciseId(exerciseId);
        if (planIds == null) {
            return;
        }
        for (Long planId : planIds) {
            if (!recoveryPlanRepository.existsById(planId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recovery plan " + planId + " not found");
            }
            BcmsExercisePlan link = new BcmsExercisePlan();
            link.setExerciseId(exerciseId);
            link.setPlanId(planId);
            exercisePlanRepository.save(link);
        }
    }

    private void syncParticipants(long exerciseId, List<ExerciseParticipantRequest> participants) {
        participantRepository.deleteByExerciseId(exerciseId);
        if (participants == null) {
            return;
        }
        for (ExerciseParticipantRequest req : participants) {
            BcmsExerciseParticipant p = new BcmsExerciseParticipant();
            p.setExerciseId(exerciseId);
            p.setUserId(req.userId());
            p.setParticipantName(blankToNull(req.participantName()));
            participantRepository.save(p);
        }
    }

    private BcmsExerciseResponse toResponse(BcmsExercise e) {
        List<Long> planIds = exercisePlanRepository.findByExerciseId(e.getId()).stream()
                .map(BcmsExercisePlan::getPlanId)
                .toList();
        List<BcmsExerciseParticipantResponse> participants = participantRepository.findByExerciseId(e.getId())
                .stream().map(BcmsExerciseParticipantResponse::from).toList();
        return BcmsExerciseResponse.from(e, planIds, participants);
    }

    private BcmsExercise require(long id) {
        return exerciseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
    }

    private void assertAdmin(User user) {
        if (user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private void audit(User user, AuditActionType action, BcmsExercise e) {
        auditTrailService.record(action, "BCMS", user.getId(), rolesSnap(user),
                "BCMS_EXERCISE", String.valueOf(e.getId()), e.getName(), null, null, null, null, null);
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
