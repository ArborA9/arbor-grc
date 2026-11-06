package com.a9.arbor.grc.modules.task.service;

import com.a9.arbor.grc.catalog.service.FrameworkScoringSupport;
import com.a9.arbor.grc.modules.bcms.domain.BiaCycle;
import com.a9.arbor.grc.modules.bcms.domain.BiaStatus;
import com.a9.arbor.grc.modules.bcms.repo.BiaCycleRepository;
import com.a9.arbor.grc.modules.bcms.service.BiaService;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaCycleResponse;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.service.CapaService;
import com.a9.arbor.grc.modules.capa.web.dto.CapaResponse;
import com.a9.arbor.grc.modules.capa.web.dto.UpdateCapaProgressRequest;
import com.a9.arbor.grc.modules.gap.pack.GapContributorText;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapItemReviewStatus;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.gap.service.GapAssessmentService;
import com.a9.arbor.grc.modules.gap.service.GapScoring;
import com.a9.arbor.grc.modules.gap.service.SamaGapScoring;
import com.a9.arbor.grc.modules.gap.service.SamaMaturityGates;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.project.domain.ComplianceProject;
import com.a9.arbor.grc.modules.project.repo.ComplianceProjectRepository;
import com.a9.arbor.grc.modules.privacy.service.PrivacyDpiaService;
import com.a9.arbor.grc.modules.privacy.service.PrivacyRopaService;
import com.a9.arbor.grc.modules.privacy.web.dto.DpiaResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaResponse;
import com.a9.arbor.grc.modules.risk.domain.RiskProject;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.TreatmentMilestone;
import com.a9.arbor.grc.modules.risk.domain.TreatmentPlan;
import com.a9.arbor.grc.modules.risk.repo.RiskProjectRepository;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.risk.repo.TreatmentMilestoneRepository;
import com.a9.arbor.grc.modules.risk.repo.TreatmentPlanRepository;
import com.a9.arbor.grc.modules.risk.service.RiskService;
import com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse;
import com.a9.arbor.grc.modules.risk.web.dto.SubmitRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.TreatmentMilestoneResponse;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.web.dto.SaveGapDraftRequest;
import com.a9.arbor.grc.modules.task.web.dto.SubmitCapaTaskRequest;
import com.a9.arbor.grc.modules.task.web.dto.SubmitGapTaskRequest;
import com.a9.arbor.grc.modules.task.web.dto.SubmitMilestoneTaskRequest;
import com.a9.arbor.grc.modules.task.web.dto.WorkTaskDetailResponse;
import com.a9.arbor.grc.modules.task.web.dto.WorkTaskSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MyTasksService {

    public static final String MODULE_GAP = "GAP";
    public static final String TYPE_GAP_ITEM = "GAP_ITEM";
    public static final String TYPE_GAP_QUESTION = "GAP_QUESTION";
    public static final String MODULE_RISK = "RISK";
    public static final String TYPE_RISK_RECORD = "RISK_RECORD";
    public static final String TYPE_RISK_ACCEPTANCE = "RISK_ACCEPTANCE";
    public static final String TYPE_RISK_MILESTONE = "RISK_MILESTONE";
    public static final String MODULE_CAPA = CapaService.MODULE_CAPA;
    public static final String TYPE_CAPA = CapaService.TYPE_CAPA;
    public static final String MODULE_BCMS = "BCMS";
    public static final String TYPE_BIA = "BIA";
    public static final String TYPE_BIA_HEAD = "BIA_HEAD";
    public static final String MODULE_POLICY = "POLICY";
    public static final String TYPE_POLICY_ACK = "POLICY_ACK";
    public static final String MODULE_KRI = "KRI";
    public static final String TYPE_KRI = "KRI";
    public static final String MODULE_INCIDENT = "INCIDENT";
    public static final String TYPE_INCIDENT = "INCIDENT";
    public static final String MODULE_PRIVACY = "PRIVACY";
    public static final String TYPE_PRIVACY_ROPA = "PRIVACY_ROPA";
    public static final String TYPE_PRIVACY_DPIA = "PRIVACY_DPIA";
    public static final String TYPE_ROPA_DEPARTMENT = "ROPA_DEPARTMENT";
    public static final String TYPE_ROPA_HEAD = "ROPA_HEAD";
    public static final String TYPE_DPIA_DEPARTMENT = "DPIA_DEPARTMENT";
    public static final String TYPE_DPIA_HEAD = "DPIA_HEAD";

    private final WorkTaskRepository workTaskRepository;
    private final GapItemRepository gapItemRepository;
    private final GapAssessmentRepository gapAssessmentRepository;
    private final RiskRecordRepository riskRecordRepository;
    private final RiskProjectRepository riskProjectRepository;
    private final TreatmentMilestoneRepository milestoneRepository;
    private final TreatmentPlanRepository treatmentPlanRepository;
    private final RiskService riskService;
    private final GapAssessmentService gapAssessmentService;
    private final CapaService capaService;
    private final BiaService biaService;
    private final BiaCycleRepository biaCycleRepository;
    private final com.a9.arbor.grc.modules.policy.service.PolicyService policyService;
    private final com.a9.arbor.grc.modules.kri.service.KriService kriService;
    private final com.a9.arbor.grc.modules.incident.service.IncidentService incidentService;
    private final PrivacyRopaService privacyRopaService;
    private final PrivacyDpiaService privacyDpiaService;
    private final UserRepository userRepository;
    private final ComplianceProjectRepository complianceProjectRepository;

    public MyTasksService(WorkTaskRepository workTaskRepository,
                          GapItemRepository gapItemRepository,
                          GapAssessmentRepository gapAssessmentRepository,
                          RiskRecordRepository riskRecordRepository,
                          RiskProjectRepository riskProjectRepository,
                          TreatmentMilestoneRepository milestoneRepository,
                          TreatmentPlanRepository treatmentPlanRepository,
                          RiskService riskService,
                          GapAssessmentService gapAssessmentService,
                          CapaService capaService,
                          BiaService biaService,
                          BiaCycleRepository biaCycleRepository,
                          com.a9.arbor.grc.modules.policy.service.PolicyService policyService,
                          com.a9.arbor.grc.modules.kri.service.KriService kriService,
                          com.a9.arbor.grc.modules.incident.service.IncidentService incidentService,
                          PrivacyRopaService privacyRopaService,
                          PrivacyDpiaService privacyDpiaService,
                          UserRepository userRepository,
                          ComplianceProjectRepository complianceProjectRepository) {
        this.workTaskRepository = workTaskRepository;
        this.gapItemRepository = gapItemRepository;
        this.gapAssessmentRepository = gapAssessmentRepository;
        this.riskRecordRepository = riskRecordRepository;
        this.riskProjectRepository = riskProjectRepository;
        this.milestoneRepository = milestoneRepository;
        this.treatmentPlanRepository = treatmentPlanRepository;
        this.riskService = riskService;
        this.gapAssessmentService = gapAssessmentService;
        this.capaService = capaService;
        this.biaService = biaService;
        this.biaCycleRepository = biaCycleRepository;
        this.policyService = policyService;
        this.kriService = kriService;
        this.incidentService = incidentService;
        this.privacyRopaService = privacyRopaService;
        this.privacyDpiaService = privacyDpiaService;
        this.userRepository = userRepository;
        this.complianceProjectRepository = complianceProjectRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkTaskSummaryResponse> listMine(User user) {
        riskService.ensureAcceptanceInboxForUser(user);
        List<WorkTask> tasks = workTaskRepository
                .findByAssignedToUserIdAndStatusNotOrderByDueDateAscUpdatedAtDesc(
                        user.getId(), WorkTaskStatus.CANCELLED);
        Map<Long, User> assigners = loadAssigners(tasks);
        Map<Long, BiaCycle> returnedBia = loadReturnedBiaCycles(tasks);
        Map<Long, GapItem> returnedGap = loadReturnedGapItems(tasks);
        Map<Long, RiskRecord> returnedRisk = loadReturnedRisks(tasks);
        Map<Long, TaskHint> taskHints = new HashMap<>();
        taskHints.putAll(loadRiskTaskHints(tasks));
        taskHints.putAll(loadGapProjectHints(tasks));
        return tasks.stream()
                .map(task -> toSummary(task, assigners, returnedBia, returnedGap, returnedRisk, taskHints))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkTaskDetailResponse getMine(User user, long taskId) {
        WorkTask task = requireMine(user, taskId);
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This task was cancelled");
        }
        if (MODULE_GAP.equals(task.getSourceModule()) && TYPE_GAP_ITEM.equals(task.getSourceRecordType())) {
            return gapDetail(task);
        }
        if (MODULE_RISK.equals(task.getSourceModule()) && TYPE_RISK_RECORD.equals(task.getSourceRecordType())) {
            return riskDetail(user, task);
        }
        if (MODULE_RISK.equals(task.getSourceModule()) && TYPE_RISK_ACCEPTANCE.equals(task.getSourceRecordType())) {
            return riskDetail(user, task);
        }
        if (MODULE_RISK.equals(task.getSourceModule()) && TYPE_RISK_MILESTONE.equals(task.getSourceRecordType())) {
            return milestoneDetail(user, task);
        }
        if (MODULE_CAPA.equals(task.getSourceModule()) && TYPE_CAPA.equals(task.getSourceRecordType())) {
            return capaDetail(task);
        }
        if (MODULE_BCMS.equals(task.getSourceModule())
                && (TYPE_BIA.equals(task.getSourceRecordType()) || TYPE_BIA_HEAD.equals(task.getSourceRecordType()))) {
            return biaDetail(task);
        }
        if (MODULE_POLICY.equals(task.getSourceModule()) && TYPE_POLICY_ACK.equals(task.getSourceRecordType())) {
            return policyDetail(user, task);
        }
        if (MODULE_KRI.equals(task.getSourceModule()) && TYPE_KRI.equals(task.getSourceRecordType())) {
            return kriDetail(user, task);
        }
        if (MODULE_INCIDENT.equals(task.getSourceModule()) && TYPE_INCIDENT.equals(task.getSourceRecordType())) {
            return incidentDetail(user, task);
        }
        if (MODULE_PRIVACY.equals(task.getSourceModule())
                && (TYPE_ROPA_DEPARTMENT.equals(task.getSourceRecordType())
                || TYPE_ROPA_HEAD.equals(task.getSourceRecordType())
                || TYPE_DPIA_DEPARTMENT.equals(task.getSourceRecordType())
                || TYPE_DPIA_HEAD.equals(task.getSourceRecordType()))) {
            return new WorkTaskDetailResponse(
                    WorkTaskSummaryResponse.from(task, assignerFor(task)),
                    null, null, null, null, null, null, null, null, null, null, null,
                    null, null);
        }
        if (MODULE_PRIVACY.equals(task.getSourceModule()) && TYPE_PRIVACY_ROPA.equals(task.getSourceRecordType())) {
            return ropaDetail(task);
        }
        if (MODULE_PRIVACY.equals(task.getSourceModule()) && TYPE_PRIVACY_DPIA.equals(task.getSourceRecordType())) {
            return dpiaDetail(task);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported task type");
    }

    @Transactional
    public WorkTaskDetailResponse submitGapTask(User user, long taskId, SubmitGapTaskRequest req) {
        if (!RoleAccess.isContributorOnly(user) && !user.getRoles().contains(Role.CONTRIBUTOR)
                && !RoleAccess.isElevated(user.getRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        WorkTask task = requireMine(user, taskId);
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            return getMine(user, taskId);
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        if (!MODULE_GAP.equals(task.getSourceModule()) || !TYPE_GAP_ITEM.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a gap task");
        }
        GapItem item = gapItemRepository.findById(task.getSourceRecordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        if (!user.getId().equals(item.getAssignedToUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this control");
        }
        if (item.getReviewStatus() == GapItemReviewStatus.SUBMITTED
                || item.getReviewStatus() == GapItemReviewStatus.FINALIZED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Already submitted: wait for Admin send-back to revise");
        }
        GapAssessment assessment = gapAssessmentRepository.findById(item.getGapAssessmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found"));
        if (assessment.getStatus() == GapAssessmentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gap assessment is complete");
        }
        if (item.getApplicability() == GapApplicability.NOT_APPLICABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Control is not applicable");
        }

        boolean sama = FrameworkScoringSupport.isSama(assessment.getFrameworkCode(), assessment.getFrameworkName());
        if (sama) {
            if (req.maturityGates() != null) {
                Boolean[] gates = SamaMaturityGates.fromList(req.maturityGates());
                if (!SamaMaturityGates.isComplete(gates)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "All five SAMA evidence gates (Yes/No) are required to submit");
                }
                Integer derived = SamaMaturityGates.maturityFromGates(gates);
                try {
                    SamaGapScoring.requireValid(derived, "maturity");
                } catch (IllegalArgumentException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
                }
                item.setMaturityGatesJson(SamaMaturityGates.toJson(gates));
                item.setMaturity(derived);
            } else if (req.maturity() != null) {
                try {
                    SamaGapScoring.requireValid(req.maturity(), "maturity");
                } catch (IllegalArgumentException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
                }
                item.setMaturity(req.maturity());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "maturityGates (five Yes/No) or maturity (0–5) is required for SAMA");
            }
            item.setImplementationStatus(null);
            item.setScore(SamaGapScoring.scoreOf(item.getMaturity()));
        } else if (item.getImplementationStatus() != null) {
            item.setScore(GapScoring.scoreOf(item.getImplementationStatus()));
        }
        if (req.contributorComments() != null) {
            String comments = req.contributorComments().trim();
            try {
                GapContributorText.requireAllowed(comments);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            item.setContributorComments(comments.isEmpty() ? null : comments);
        }
        boolean hasQuestions = !gapAssessmentService.listQuestionsForItem(user, assessment.getId(), item.getId()).isEmpty()
                || !gapAssessmentService.listCustomQuestions(user, assessment.getId(), item.getId()).isEmpty();
        if (!hasQuestions && (item.getContributorComments() == null || item.getContributorComments().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A response is required when no questions are assigned");
        }
        if (hasQuestions) {
            gapAssessmentService.requireContributorAnswers(user, assessment.getId(), item.getId());
        }
        item.setUpdatedByUserId(user.getId());
        gapItemRepository.save(item);

        gapAssessmentService.recordContributorSubmission(user, item, task.getId());

        if (assessment.getStatus() == GapAssessmentStatus.DRAFT) {
            assessment.setStatus(GapAssessmentStatus.IN_PROGRESS);
            gapAssessmentRepository.save(assessment);
        }
        gapAssessmentService.refreshAssessmentScores(assessment.getId());

        task.setStatus(WorkTaskStatus.SUBMITTED);
        task.setSubmittedAt(Instant.now());
        workTaskRepository.save(task);

        return getMine(user, taskId);
    }

    @Transactional
    public WorkTaskDetailResponse saveGapDraft(User user, long taskId, SaveGapDraftRequest req) {
        if (!RoleAccess.isContributorOnly(user) && !user.getRoles().contains(Role.CONTRIBUTOR)
                && !RoleAccess.isElevated(user.getRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        WorkTask task = requireMine(user, taskId);
        if (task.getStatus() == WorkTaskStatus.SUBMITTED || task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task is not editable");
        }
        if (!MODULE_GAP.equals(task.getSourceModule()) || !TYPE_GAP_ITEM.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a gap task");
        }
        GapItem item = gapItemRepository.findById(task.getSourceRecordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        if (!user.getId().equals(item.getAssignedToUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this control");
        }
        if (req.maturityGates() != null) {
            Boolean[] gates = SamaMaturityGates.fromList(req.maturityGates());
            item.setMaturityGatesJson(SamaMaturityGates.toJson(gates));
            Integer derived = SamaMaturityGates.maturityFromGates(gates);
            if (derived != null) {
                try {
                    SamaGapScoring.requireValid(derived, "maturity");
                } catch (IllegalArgumentException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
                }
                item.setMaturity(derived);
                item.setScore(SamaGapScoring.scoreOf(derived));
            } else {
                item.setMaturity(null);
            }
        } else if (req.maturity() != null) {
            try {
                SamaGapScoring.requireValid(req.maturity(), "maturity");
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            item.setMaturity(req.maturity());
            item.setScore(SamaGapScoring.scoreOf(req.maturity()));
        }
        if (req.contributorComments() != null) {
            String comments = req.contributorComments().trim();
            try {
                GapContributorText.requireAllowed(comments);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
            }
            item.setContributorComments(comments.isEmpty() ? null : comments);
        }
        item.setUpdatedByUserId(user.getId());
        gapItemRepository.save(item);
        if (task.getStatus() == WorkTaskStatus.PENDING) {
            task.setStatus(WorkTaskStatus.IN_PROGRESS);
            workTaskRepository.save(task);
        }
        return getMine(user, taskId);
    }

    @Transactional
    public WorkTaskDetailResponse submitMilestoneTask(User user, long taskId, SubmitMilestoneTaskRequest req) {
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_RISK.equals(task.getSourceModule()) || !TYPE_RISK_MILESTONE.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a milestone task");
        }
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            return getMine(user, taskId);
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        TreatmentMilestone milestone = milestoneRepository.findById(task.getSourceRecordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
        TreatmentPlan plan = treatmentPlanRepository.findById(milestone.getTreatmentPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Treatment plan not found"));
        RiskRecord risk = riskRecordRepository.findById(plan.getRiskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));
        riskService.completeMilestone(user, risk.getId(), milestone.getId(),
                req != null ? req.progressNotes() : null);
        task.setStatus(WorkTaskStatus.SUBMITTED);
        task.setSubmittedAt(Instant.now());
        workTaskRepository.save(task);
        return getMine(user, taskId);
    }

    @Transactional
    public WorkTaskDetailResponse submitRiskTask(User user, long taskId, SubmitRiskRequest req) {
        if (!RoleAccess.isContributorOnly(user) && !user.getRoles().contains(Role.CONTRIBUTOR)
                && !RoleAccess.isElevated(user.getRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_RISK.equals(task.getSourceModule()) || !TYPE_RISK_RECORD.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    TYPE_RISK_ACCEPTANCE.equals(task.getSourceRecordType())
                            ? "Approve or reject this from Risk Acceptance, not Submit"
                            : "Not a risk task");
        }
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            return getMine(user, taskId);
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        RiskRecord risk = riskRecordRepository.findById(task.getSourceRecordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));
        if (risk.getStatus() != RiskStatus.ASSIGNED && risk.getStatus() != RiskStatus.RE_OPENED
                && risk.getStatus() != RiskStatus.PENDING_ACCEPTANCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Risk is not awaiting submission");
        }
        riskService.submit(user, risk.getId(), req);
        return getMine(user, taskId);
    }

    @Transactional
    public WorkTaskDetailResponse submitCapaTask(User user, long taskId, SubmitCapaTaskRequest req) {
        if (!RoleAccess.isContributorOnly(user) && !user.getRoles().contains(Role.CONTRIBUTOR)
                && !RoleAccess.isElevated(user.getRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_CAPA.equals(task.getSourceModule()) || !TYPE_CAPA.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a CAPA task");
        }
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            return getMine(user, taskId);
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        capaService.updateProgress(user, task.getSourceRecordId(), new UpdateCapaProgressRequest(
                req.rootCause(), req.correctiveAction(), req.preventiveAction(), req.progressNotes()));
        capaService.submit(user, task.getSourceRecordId());
        return getMine(user, taskId);
    }

    @Transactional
    public WorkTaskDetailResponse submitBiaTask(User user, long taskId) {
        if (!RoleAccess.isContributorOnly(user) && !user.getRoles().contains(Role.CONTRIBUTOR)
                && !RoleAccess.isElevated(user.getRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_BCMS.equals(task.getSourceModule()) || !TYPE_BIA.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a BIA task");
        }
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            return getMine(user, taskId);
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        biaService.submit(user, task.getSourceRecordId());
        return getMine(user, taskId);
    }

    @Transactional
    public WorkTaskDetailResponse acknowledgePolicyTask(User user, long taskId) {
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_POLICY.equals(task.getSourceModule()) || !TYPE_POLICY_ACK.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a policy acknowledgement task");
        }
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            return getMine(user, taskId);
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        policyService.acknowledge(user, task.getSourceRecordId());
        return getMine(user, taskId);
    }

    @Transactional
    public WorkTaskDetailResponse submitKriTask(User user, long taskId,
                                                com.a9.arbor.grc.modules.kri.web.dto.RecordKriValueRequest req) {
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_KRI.equals(task.getSourceModule()) || !TYPE_KRI.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a KRI task");
        }
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task already submitted");
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        kriService.recordValueFromTask(user, task.getSourceRecordId(), req);
        return getMine(user, taskId);
    }

    @Transactional
    public WorkTaskDetailResponse submitIncidentTask(User user, long taskId,
                                                     com.a9.arbor.grc.modules.incident.web.dto.SubmitIncidentTaskRequest req) {
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_INCIDENT.equals(task.getSourceModule()) || !TYPE_INCIDENT.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not an incident task");
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        incidentService.submitFromTask(user, task.getSourceRecordId(), req);
        return getMine(user, taskId);
    }

    private WorkTaskDetailResponse gapDetail(WorkTask task) {
        GapItem item = gapItemRepository.findById(task.getSourceRecordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap item not found"));
        GapAssessment assessment = gapAssessmentRepository.findById(item.getGapAssessmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found"));
        boolean sentBack = item.getReviewStatus() == GapItemReviewStatus.SENT_BACK;
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(
                        task,
                        assignerFor(task),
                        sentBack,
                        sentBack ? item.getSendBackReason() : null),
                gapAssessmentService.toItemResponse(item),
                assessment.getId(),
                assessment.getName(),
                assessment.getFrameworkCode(),
                null, null, null, null, null, null, null, null, null);
    }

    private WorkTaskDetailResponse riskDetail(User user, WorkTask task) {
        RiskRecordResponse risk = riskService.get(user, task.getSourceRecordId());
        boolean returned = risk.status() == RiskStatus.RE_OPENED;
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(
                        task, assignerFor(task), returned, returned ? risk.rejectionReason() : null),
                null, null, null,
                risk.frameworkCode(),
                risk,
                null, null, null, null, null, null, null, null);
    }

    private WorkTaskDetailResponse milestoneDetail(User user, WorkTask task) {
        TreatmentMilestone milestone = milestoneRepository.findById(task.getSourceRecordId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone not found"));
        TreatmentPlan plan = treatmentPlanRepository.findById(milestone.getTreatmentPlanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Treatment plan not found"));
        RiskRecordResponse risk = riskService.get(user, plan.getRiskId());
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(task, assignerFor(task)),
                null, null, null,
                risk.frameworkCode(),
                risk,
                null, null, null, null, null,
                TreatmentMilestoneResponse.from(milestone),
                null, null);
    }

    private WorkTaskDetailResponse capaDetail(WorkTask task) {
        Capa capa = capaService.require(task.getSourceRecordId());
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(task, assignerFor(task)),
                null, null, null,
                capa.getControlRef(),
                null,
                CapaResponse.from(capa),
                null, null, null, null, null, null, null);
    }

    private WorkTaskDetailResponse biaDetail(WorkTask task) {
        BiaCycleResponse cycle = biaService.get(nullUserForCycle(task), task.getSourceRecordId());
        boolean returned = cycle != null && cycle.status() == BiaStatus.RETURNED;
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(task, assignerFor(task), returned,
                        returned ? cycle.returnReason() : null),
                null, null, null, null, null, null,
                cycle,
                null, null, null, null, null, null);
    }

    private com.a9.arbor.grc.modules.iam.domain.User nullUserForCycle(WorkTask task) {
        return userRepository.findById(task.getAssignedToUserId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Assignee not found"));
    }

    private WorkTaskDetailResponse policyDetail(User user, WorkTask task) {
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(task, assignerFor(task)),
                null, null, null, null, null, null, null,
                policyService.taskPayload(task.getSourceRecordId(), user),
                null, null, null, null, null);
    }

    private WorkTaskDetailResponse kriDetail(User user, WorkTask task) {
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(task, assignerFor(task)),
                null, null, null, null, null, null, null, null,
                kriService.get(user, task.getSourceRecordId()),
                null, null, null, null);
    }

    private WorkTaskDetailResponse incidentDetail(User user, WorkTask task) {
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(task, assignerFor(task)),
                null, null, null, null, null, null, null, null, null,
                incidentService.get(user, task.getSourceRecordId()),
                null, null, null);
    }

    @Transactional
    public WorkTaskDetailResponse submitRopaTask(User user, long taskId) {
        if (!RoleAccess.isContributorOnly(user) && !user.getRoles().contains(Role.CONTRIBUTOR)
                && !RoleAccess.isElevated(user.getRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_PRIVACY.equals(task.getSourceModule()) || !TYPE_PRIVACY_ROPA.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a Privacy RoPA task");
        }
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            return getMine(user, taskId);
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        privacyRopaService.submitFromContributor(user, task.getSourceRecordId());
        return getMine(user, taskId);
    }

    private WorkTaskDetailResponse ropaDetail(WorkTask task) {
        RopaResponse ropa = privacyRopaService.get(task.getSourceRecordId());
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(task, assignerFor(task)),
                null, null, null, null, null, null, null, null, null, null, null,
                ropa, null);
    }

    @Transactional
    public WorkTaskDetailResponse submitDpiaTask(User user, long taskId) {
        if (!RoleAccess.isContributorOnly(user) && !user.getRoles().contains(Role.CONTRIBUTOR)
                && !RoleAccess.isElevated(user.getRoles())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        WorkTask task = requireMine(user, taskId);
        if (!MODULE_PRIVACY.equals(task.getSourceModule()) || !TYPE_PRIVACY_DPIA.equals(task.getSourceRecordType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a Privacy DPIA task");
        }
        if (task.getStatus() == WorkTaskStatus.SUBMITTED) {
            return getMine(user, taskId);
        }
        if (task.getStatus() == WorkTaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task was cancelled");
        }
        privacyDpiaService.submitFromContributor(user, task.getSourceRecordId());
        return getMine(user, taskId);
    }

    private WorkTaskDetailResponse dpiaDetail(WorkTask task) {
        DpiaResponse dpia = privacyDpiaService.get(task.getSourceRecordId());
        return new WorkTaskDetailResponse(
                WorkTaskSummaryResponse.from(task, assignerFor(task)),
                null, null, null, null, null, null, null, null, null, null, null,
                null, dpia);
    }

    private WorkTask requireMine(User user, long taskId) {
        WorkTask task = workTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!user.getId().equals(task.getAssignedToUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your task");
        }
        return task;
    }

    private WorkTaskSummaryResponse toSummary(WorkTask task, Map<Long, User> assigners,
                                             Map<Long, BiaCycle> returnedBia,
                                             Map<Long, GapItem> returnedGap,
                                             Map<Long, RiskRecord> returnedRisk,
                                             Map<Long, TaskHint> taskHints) {
        User assigner = task.getAssignedByUserId() != null ? assigners.get(task.getAssignedByUserId()) : null;
        BiaCycle cycle = isBiaTask(task) ? returnedBia.get(task.getSourceRecordId()) : null;
        GapItem sentBackGap = isGapItemTask(task) ? returnedGap.get(task.getSourceRecordId()) : null;
        RiskRecord sentBackRisk = isRiskRecordTask(task) ? returnedRisk.get(task.getSourceRecordId()) : null;
        boolean returned = cycle != null || sentBackGap != null || sentBackRisk != null;
        String returnReason = cycle != null
                ? cycle.getReturnReason()
                : sentBackGap != null
                    ? sentBackGap.getSendBackReason()
                    : sentBackRisk != null ? sentBackRisk.getRejectionReason() : null;
        TaskHint hint = taskHints.get(task.getId());
        return WorkTaskSummaryResponse.from(task, assigner, returned, returnReason,
                hint != null ? hint.projectId() : null,
                hint != null ? hint.projectName() : null,
                hint != null ? hint.riskType() : null,
                hint != null ? hint.riskStatus() : null);
    }

    private record TaskHint(Long projectId, String projectName, String riskType, String riskStatus) {}

    /** Project grouping plus IS vs ERM so Contributor nav can hide unused Risk children. */
    private Map<Long, TaskHint> loadRiskTaskHints(List<WorkTask> tasks) {
        List<WorkTask> riskTasks = tasks.stream()
                .filter(t -> MODULE_RISK.equals(t.getSourceModule())
                        && (TYPE_RISK_RECORD.equals(t.getSourceRecordType())
                        || TYPE_RISK_MILESTONE.equals(t.getSourceRecordType())
                        || TYPE_RISK_ACCEPTANCE.equals(t.getSourceRecordType())))
                .toList();
        if (riskTasks.isEmpty()) {
            return Map.of();
        }

        Set<Long> riskIds = new HashSet<>();
        for (WorkTask t : riskTasks) {
            if (TYPE_RISK_RECORD.equals(t.getSourceRecordType())
                    || TYPE_RISK_ACCEPTANCE.equals(t.getSourceRecordType())) {
                riskIds.add(t.getSourceRecordId());
            }
        }

        Map<Long, Long> milestoneToRisk = new HashMap<>();
        List<Long> milestoneIds = riskTasks.stream()
                .filter(t -> TYPE_RISK_MILESTONE.equals(t.getSourceRecordType()))
                .map(WorkTask::getSourceRecordId)
                .distinct()
                .toList();
        if (!milestoneIds.isEmpty()) {
            List<TreatmentMilestone> milestones = milestoneRepository.findAllById(milestoneIds);
            Set<Long> planIds = new HashSet<>();
            for (TreatmentMilestone m : milestones) {
                planIds.add(m.getTreatmentPlanId());
            }
            Map<Long, Long> planToRisk = treatmentPlanRepository.findAllById(planIds).stream()
                    .collect(Collectors.toMap(TreatmentPlan::getId, TreatmentPlan::getRiskId));
            for (TreatmentMilestone m : milestones) {
                Long rid = planToRisk.get(m.getTreatmentPlanId());
                if (rid != null) {
                    milestoneToRisk.put(m.getId(), rid);
                    riskIds.add(rid);
                }
            }
        }
        if (riskIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> riskToProject = new HashMap<>();
        Map<Long, String> riskToType = new HashMap<>();
        Map<Long, String> riskToStatus = new HashMap<>();
        for (RiskRecord r : riskRecordRepository.findAllById(riskIds)) {
            if (r.getRiskType() != null) {
                riskToType.put(r.getId(), r.getRiskType().name());
            }
            if (r.getStatus() != null) {
                riskToStatus.put(r.getId(), r.getStatus().name());
            }
            if (r.getRiskProjectId() != null) {
                riskToProject.put(r.getId(), r.getRiskProjectId());
            }
        }

        Set<Long> projectIds = new HashSet<>(riskToProject.values());
        Map<Long, String> projectNames = projectIds.isEmpty()
                ? Map.of()
                : riskProjectRepository.findAllById(projectIds).stream()
                        .collect(Collectors.toMap(RiskProject::getId, RiskProject::getName));

        Map<Long, TaskHint> byTask = new HashMap<>();
        for (WorkTask t : riskTasks) {
            Long rid = TYPE_RISK_MILESTONE.equals(t.getSourceRecordType())
                    ? milestoneToRisk.get(t.getSourceRecordId())
                    : t.getSourceRecordId();
            if (rid == null) {
                continue;
            }
            Long pid = riskToProject.get(rid);
            String name = pid != null ? projectNames.get(pid) : null;
            if (name != null && name.isBlank()) {
                name = null;
            }
            String type = riskToType.get(rid);
            String status = riskToStatus.get(rid);
            if (pid == null && name == null && type == null && status == null) {
                continue;
            }
            byTask.put(t.getId(), new TaskHint(pid, name, type, status));
        }
        return byTask;
    }

    private Map<Long, TaskHint> loadGapProjectHints(List<WorkTask> tasks) {
        List<WorkTask> gapTasks = tasks.stream()
                .filter(t -> MODULE_GAP.equals(t.getSourceModule())
                        && TYPE_GAP_ITEM.equals(t.getSourceRecordType()))
                .toList();
        if (gapTasks.isEmpty()) {
            return Map.of();
        }
        Set<Long> itemIds = gapTasks.stream().map(WorkTask::getSourceRecordId).collect(Collectors.toSet());
        List<GapItem> items = gapItemRepository.findAllById(itemIds);
        if (items.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> itemToAssessment = new HashMap<>();
        for (GapItem item : items) {
            if (item.getGapAssessmentId() != null) {
                itemToAssessment.put(item.getId(), item.getGapAssessmentId());
            }
        }
        Set<Long> assessmentIds = new HashSet<>(itemToAssessment.values());
        Map<Long, Long> assessmentToProject = new HashMap<>();
        for (GapAssessment a : gapAssessmentRepository.findAllById(assessmentIds)) {
            if (a.getProjectId() != null) {
                assessmentToProject.put(a.getId(), a.getProjectId());
            }
        }
        if (assessmentToProject.isEmpty()) {
            return Map.of();
        }
        Set<Long> projectIds = new HashSet<>(assessmentToProject.values());
        Map<Long, String> projectNames = complianceProjectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(ComplianceProject::getId, ComplianceProject::getName));
        Map<Long, TaskHint> byTask = new HashMap<>();
        for (WorkTask t : gapTasks) {
            Long aid = itemToAssessment.get(t.getSourceRecordId());
            if (aid == null) {
                continue;
            }
            Long pid = assessmentToProject.get(aid);
            if (pid == null) {
                continue;
            }
            String name = projectNames.get(pid);
            if (name == null || name.isBlank()) {
                continue;
            }
            byTask.put(t.getId(), new TaskHint(pid, name, null, null));
        }
        return byTask;
    }

    private Map<Long, BiaCycle> loadReturnedBiaCycles(List<WorkTask> tasks) {
        List<Long> ids = tasks.stream()
                .filter(MyTasksService::isBiaTask)
                .map(WorkTask::getSourceRecordId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return biaCycleRepository.findAllById(ids).stream()
                .filter(c -> c.getStatus() == BiaStatus.RETURNED)
                .collect(Collectors.toMap(BiaCycle::getId, c -> c));
    }

    private Map<Long, RiskRecord> loadReturnedRisks(List<WorkTask> tasks) {
        List<Long> ids = tasks.stream()
                .filter(MyTasksService::isRiskRecordTask)
                .map(WorkTask::getSourceRecordId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, RiskRecord> out = new HashMap<>();
        for (RiskRecord risk : riskRecordRepository.findAllById(ids)) {
            if (risk.getStatus() == RiskStatus.RE_OPENED) {
                out.put(risk.getId(), risk);
            }
        }
        return out;
    }

    private Map<Long, GapItem> loadReturnedGapItems(List<WorkTask> tasks) {
        List<Long> ids = tasks.stream()
                .filter(MyTasksService::isGapItemTask)
                .map(WorkTask::getSourceRecordId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, GapItem> out = new HashMap<>();
        for (GapItem item : gapItemRepository.findAllById(ids)) {
            if (item.getReviewStatus() == GapItemReviewStatus.SENT_BACK) {
                out.put(item.getId(), item);
            }
        }
        return out;
    }

    private static boolean isBiaTask(WorkTask task) {
        return MODULE_BCMS.equals(task.getSourceModule())
                && (TYPE_BIA.equals(task.getSourceRecordType()) || TYPE_BIA_HEAD.equals(task.getSourceRecordType()));
    }

    private static boolean isGapItemTask(WorkTask task) {
        return MODULE_GAP.equals(task.getSourceModule()) && TYPE_GAP_ITEM.equals(task.getSourceRecordType());
    }

    private static boolean isRiskRecordTask(WorkTask task) {
        return MODULE_RISK.equals(task.getSourceModule()) && TYPE_RISK_RECORD.equals(task.getSourceRecordType());
    }

    private User assignerFor(WorkTask task) {
        if (task.getAssignedByUserId() == null) {
            return null;
        }
        return userRepository.findById(task.getAssignedByUserId()).orElse(null);
    }

    private Map<Long, User> loadAssigners(Collection<WorkTask> tasks) {
        Set<Long> ids = tasks.stream()
                .map(WorkTask::getAssignedByUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
