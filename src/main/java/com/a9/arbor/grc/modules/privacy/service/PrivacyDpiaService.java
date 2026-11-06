package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;
import com.a9.arbor.grc.modules.evidence.domain.PrivacyEvidenceStages;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceLinkRepository;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.privacy.domain.DpiaIntakeMode;
import com.a9.arbor.grc.modules.privacy.domain.DpiaStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDpia;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyRopa;
import com.a9.arbor.grc.modules.privacy.domain.RopaDpiaStatus;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyDpiaRepository;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.AssignDpiaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateDpiaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.DpiaResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.RejectDpiaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateDpiaRequest;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PrivacyDpiaService {

    private final PrivacyDpiaRepository dpiaRepository;
    private final PrivacyRopaRepository ropaRepository;
    private final UserRepository userRepository;
    private final WorkTaskRepository workTaskRepository;
    private final EvidenceLinkRepository evidenceLinkRepository;
    private final PrivacyDpiaWorkbookTemplate workbookTemplate;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    public PrivacyDpiaService(PrivacyDpiaRepository dpiaRepository,
                              PrivacyRopaRepository ropaRepository,
                              UserRepository userRepository,
                              WorkTaskRepository workTaskRepository,
                              EvidenceLinkRepository evidenceLinkRepository,
                              PrivacyDpiaWorkbookTemplate workbookTemplate,
                              NotificationService notificationService,
                              AuditTrailService auditTrailService) {
        this.dpiaRepository = dpiaRepository;
        this.ropaRepository = ropaRepository;
        this.userRepository = userRepository;
        this.workTaskRepository = workTaskRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.workbookTemplate = workbookTemplate;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<DpiaResponse> list() {
        return dpiaRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(DpiaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DpiaResponse get(long id) {
        return DpiaResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public ExportFile downloadTemplate() {
        return workbookTemplate.buildTemplate();
    }

    @Transactional(readOnly = true)
    public Object methodologyBank() {
        return Map.of(
                "screeningQ4toQ7", DpiaMethodologyBank.SCREENING_Q4_TO_Q7,
                "questions", DpiaMethodologyBank.QUESTIONS,
                "principles", DpiaMethodologyBank.PRINCIPLES,
                "likelihood", DpiaMethodologyBank.LIKELIHOOD,
                "impact", DpiaMethodologyBank.IMPACT,
                "treatment", DpiaMethodologyBank.TREATMENT);
    }

    @Transactional
    public DpiaResponse create(User user, CreateDpiaRequest req) {
        validateRopa(req.ropaId());
        DpiaIntakeMode mode = req.intakeMode() != null ? req.intakeMode() : DpiaIntakeMode.FORM;
        PrivacyDpia row = new PrivacyDpia();
        row.setDisplayId(nextDisplayId());
        row.setTitle(req.title().trim());
        row.setRopaId(req.ropaId());
        row.setIntakeMode(mode);
        applyScreening(row, req.preScreeningRequired(), req.preScreeningOutcome(), req.preScreeningNotes(),
                req.screeningQ1(), req.screeningQ2(), req.screeningQ3(),
                req.screeningQ4(), req.screeningQ5(), req.screeningQ6(), req.screeningQ7());
        row.setQuestionsJson(blankToNull(req.questionsJson()));
        row.setAssessmentJson(blankToNull(req.assessmentJson()));
        row.setOverviewRisksJson(blankToNull(req.overviewRisksJson()));
        row.setProcessingDescription(blankToNull(req.processingDescription()));
        row.setNecessityAssessment(blankToNull(req.necessityAssessment()));
        row.setRisksIdentified(blankToNull(req.risksIdentified()));
        row.setMitigationMeasures(blankToNull(req.mitigationMeasures()));
        row.setResidualRisk(req.residualRisk());
        row.setStatus(DpiaStatus.DRAFT);
        row.setCreatedByUserId(user.getId());
        row.recomputeFullDpiaMandatory();
        dpiaRepository.save(row);
        syncLinkedRopa(row);
        audit(user, AuditActionType.CREATED, row, null, null, null);
        return DpiaResponse.from(row);
    }

    @Transactional
    public DpiaResponse update(User user, long id, UpdateDpiaRequest req) {
        PrivacyDpia row = require(id);
        assertEditable(row);
        if (req.title() != null && !req.title().isBlank()) {
            row.setTitle(req.title().trim());
        }
        if (req.ropaId() != null) {
            validateRopa(req.ropaId());
            row.setRopaId(req.ropaId());
        }
        applyScreening(row,
                req.preScreeningRequired() != null ? req.preScreeningRequired() : row.isPreScreeningRequired(),
                req.preScreeningOutcome() != null ? req.preScreeningOutcome() : row.getPreScreeningOutcome(),
                req.preScreeningNotes() != null ? req.preScreeningNotes() : row.getPreScreeningNotes(),
                req.screeningQ1() != null ? req.screeningQ1() : row.getScreeningQ1(),
                req.screeningQ2() != null ? req.screeningQ2() : row.getScreeningQ2(),
                req.screeningQ3() != null ? req.screeningQ3() : row.getScreeningQ3(),
                req.screeningQ4() != null ? req.screeningQ4() : row.getScreeningQ4(),
                req.screeningQ5() != null ? req.screeningQ5() : row.getScreeningQ5(),
                req.screeningQ6() != null ? req.screeningQ6() : row.getScreeningQ6(),
                req.screeningQ7() != null ? req.screeningQ7() : row.getScreeningQ7());
        if (req.questionsJson() != null) {
            row.setQuestionsJson(blankToNull(req.questionsJson()));
        }
        if (req.assessmentJson() != null) {
            row.setAssessmentJson(blankToNull(req.assessmentJson()));
        }
        if (req.overviewRisksJson() != null) {
            row.setOverviewRisksJson(blankToNull(req.overviewRisksJson()));
        }
        if (req.processingDescription() != null) {
            row.setProcessingDescription(blankToNull(req.processingDescription()));
        }
        if (req.necessityAssessment() != null) {
            row.setNecessityAssessment(blankToNull(req.necessityAssessment()));
        }
        if (req.risksIdentified() != null) {
            row.setRisksIdentified(blankToNull(req.risksIdentified()));
        }
        if (req.mitigationMeasures() != null) {
            row.setMitigationMeasures(blankToNull(req.mitigationMeasures()));
        }
        if (req.residualRisk() != null) {
            row.setResidualRisk(req.residualRisk());
        }
        if (row.getStatus() == DpiaStatus.REJECTED) {
            row.setStatus(DpiaStatus.DRAFT);
            row.setReviewNotes(null);
            row.setReviewedByUserId(null);
            row.setReviewedAt(null);
        }
        row.recomputeFullDpiaMandatory();
        dpiaRepository.save(row);
        syncLinkedRopa(row);
        audit(user, AuditActionType.UPDATED, row, null, null, null);
        return DpiaResponse.from(row);
    }

    @Transactional
    public DpiaResponse assign(User admin, long id, AssignDpiaRequest req) {
        PrivacyDpia row = require(id);
        if (row.getIntakeMode() != DpiaIntakeMode.WORKBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only workbook-intake DPIA can be assigned to a Contributor");
        }
        if (row.getStatus() != DpiaStatus.DRAFT && row.getStatus() != DpiaStatus.REJECTED
                && row.getStatus() != DpiaStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "DPIA must be DRAFT, REJECTED, or ASSIGNED to assign or reassign");
        }
        if (row.getStatus() == DpiaStatus.ASSIGNED && row.getWorkTaskId() != null) {
            WorkTask existing = workTaskRepository.findById(row.getWorkTaskId()).orElse(null);
            if (existing != null && existing.getStatus() == WorkTaskStatus.SUBMITTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot reassign while Contributor submission is in review");
            }
        }
        User assignee = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        if (!assignee.getRoles().contains(Role.CONTRIBUTOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must have CONTRIBUTOR role");
        }

        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_PRIVACY, MyTasksService.TYPE_PRIVACY_DPIA, row.getId())
                .orElseGet(WorkTask::new);
        task.setSourceModule(MyTasksService.MODULE_PRIVACY);
        task.setSourceRecordType(MyTasksService.TYPE_PRIVACY_DPIA);
        task.setSourceRecordId(row.getId());
        task.setTitle(row.getDisplayId() + ": " + row.getTitle());
        task.setDetail("DPIA workbook · attach filled sheet + approval evidence");
        task.setAssignedToUserId(assignee.getId());
        task.setAssignedByUserId(admin.getId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(req.dueDate());
        task.setSubmittedAt(null);
        workTaskRepository.save(task);

        row.setAssignedContributorId(assignee.getId());
        row.setWorkTaskId(task.getId());
        row.setAssignedAt(Instant.now());
        row.setStatus(DpiaStatus.ASSIGNED);
        row.setReviewNotes(null);
        row.setReviewedByUserId(null);
        row.setReviewedAt(null);
        dpiaRepository.save(row);

        notificationService.trigger(
                "DPIA_ASSIGNED",
                String.valueOf(row.getId()),
                List.of(assignee.getId().toString()),
                Map.of(
                        "displayId", row.getDisplayId(),
                        "title", row.getTitle(),
                        "dueDate", req.dueDate() != null ? req.dueDate().toString() : "-"));

        audit(admin, AuditActionType.UPDATED, row, "assignedTo", null, assignee.getEmail());
        return DpiaResponse.from(row);
    }

    @Transactional
    public DpiaResponse submitFromContributor(User user, long id) {
        PrivacyDpia row = require(id);
        if (row.getIntakeMode() != DpiaIntakeMode.WORKBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a workbook DPIA");
        }
        if (row.getStatus() != DpiaStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DPIA is not awaiting Contributor submission");
        }
        if (!Objects.equals(user.getId(), row.getAssignedContributorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this DPIA");
        }
        requireModeBEvidence(row);

        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_PRIVACY, MyTasksService.TYPE_PRIVACY_DPIA, row.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Work task missing"));
        task.setStatus(WorkTaskStatus.SUBMITTED);
        task.setSubmittedAt(Instant.now());
        workTaskRepository.save(task);

        String previous = row.getStatus().name();
        row.setStatus(DpiaStatus.UNDER_REVIEW);
        dpiaRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, "status", previous, DpiaStatus.UNDER_REVIEW.name());
        return DpiaResponse.from(row);
    }

    @Transactional
    public DpiaResponse submitForReview(User user, long id) {
        PrivacyDpia row = require(id);
        if (row.getIntakeMode() == DpiaIntakeMode.WORKBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Workbook DPIA is submitted by the assigned Contributor via My Tasks");
        }
        if (row.getStatus() != DpiaStatus.DRAFT && row.getStatus() != DpiaStatus.REJECTED
                && row.getStatus() != DpiaStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT, IN_PROGRESS or REJECTED DPIA can be submitted");
        }
        String previous = row.getStatus().name();
        row.setStatus(DpiaStatus.UNDER_REVIEW);
        row.setReviewNotes(null);
        row.setReviewedByUserId(null);
        row.setReviewedAt(null);
        dpiaRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, "status", previous, DpiaStatus.UNDER_REVIEW.name());
        return DpiaResponse.from(row);
    }

    @Transactional
    public DpiaResponse approve(User user, long id) {
        PrivacyDpia row = require(id);
        if (row.getStatus() != DpiaStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only UNDER_REVIEW DPIA can be approved");
        }
        String previous = row.getStatus().name();
        row.setStatus(DpiaStatus.APPROVED);
        row.setReviewedByUserId(user.getId());
        row.setReviewedAt(Instant.now());
        dpiaRepository.save(row);
        if (row.getRopaId() != null) {
            ropaRepository.findById(row.getRopaId()).ifPresent(ropa -> {
                ropa.setLinkedDpiaId(row.getId());
                ropaRepository.save(ropa);
            });
        }
        audit(user, AuditActionType.APPROVED, row, "status", previous, DpiaStatus.APPROVED.name());
        return DpiaResponse.from(row);
    }

    @Transactional
    public DpiaResponse reject(User user, long id, RejectDpiaRequest req) {
        PrivacyDpia row = require(id);
        if (row.getStatus() != DpiaStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only UNDER_REVIEW DPIA can be rejected");
        }
        String previous = row.getStatus().name();
        row.setStatus(DpiaStatus.REJECTED);
        row.setReviewNotes(blankToNull(req != null ? req.notes() : null));
        row.setReviewedByUserId(user.getId());
        row.setReviewedAt(Instant.now());
        dpiaRepository.save(row);
        reopenContributorTask(row);
        audit(user, AuditActionType.REJECTED, row, "status", previous, DpiaStatus.REJECTED.name());
        return DpiaResponse.from(row);
    }

    private void reopenContributorTask(PrivacyDpia row) {
        if (row.getIntakeMode() != DpiaIntakeMode.WORKBOOK || row.getAssignedContributorId() == null) {
            return;
        }
        workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_PRIVACY, MyTasksService.TYPE_PRIVACY_DPIA, row.getId())
                .ifPresent(task -> {
                    task.setStatus(WorkTaskStatus.PENDING);
                    task.setSubmittedAt(null);
                    workTaskRepository.save(task);
                });
        row.setStatus(DpiaStatus.ASSIGNED);
        dpiaRepository.save(row);
    }

    private void requireModeBEvidence(PrivacyDpia row) {
        List<EvidenceLink> links = evidenceLinkRepository
                .findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(
                        PrivacyEvidenceStages.MODULE,
                        PrivacyEvidenceStages.RECORD_DPIA,
                        String.valueOf(row.getId()));
        boolean hasWorkbook = links.stream().anyMatch(l ->
                PrivacyEvidenceStages.WORKBOOK_SUBMITTED.equalsIgnoreCase(normStage(l.getWorkflowStage())));
        boolean hasApproval = links.stream().anyMatch(l ->
                PrivacyEvidenceStages.APPROVAL_EVIDENCE.equalsIgnoreCase(normStage(l.getWorkflowStage())));
        if (!hasWorkbook || !hasApproval) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Submit requires linked evidence stages WORKBOOK_SUBMITTED and APPROVAL_EVIDENCE");
        }
    }

    private void syncLinkedRopa(PrivacyDpia row) {
        if (!row.isFullDpiaMandatory() || row.getRopaId() == null) {
            return;
        }
        PrivacyRopa ropa = ropaRepository.findById(row.getRopaId()).orElse(null);
        if (ropa == null) {
            return;
        }
        if (ropa.getDpiaStatus() != RopaDpiaStatus.REQUIRED) {
            ropa.setDpiaStatus(RopaDpiaStatus.REQUIRED);
            ropaRepository.save(ropa);
        }
    }

    private static void applyScreening(PrivacyDpia row,
                                       Boolean preRequired,
                                       String outcome,
                                       String notes,
                                       String q1, String q2, Boolean q3,
                                       Boolean q4, Boolean q5, Boolean q6, Boolean q7) {
        row.setPreScreeningRequired(Boolean.TRUE.equals(preRequired));
        row.setPreScreeningOutcome(blankToNull(outcome));
        row.setPreScreeningNotes(blankToNull(notes));
        row.setScreeningQ1(blankToNull(q1));
        row.setScreeningQ2(blankToNull(q2));
        row.setScreeningQ3(q3);
        row.setScreeningQ4(q4);
        row.setScreeningQ5(q5);
        row.setScreeningQ6(q6);
        row.setScreeningQ7(q7);
    }

    private static String normStage(String stage) {
        return stage == null ? "" : stage.trim().toUpperCase(Locale.ROOT);
    }

    private PrivacyDpia require(long id) {
        return dpiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DPIA not found"));
    }

    private void validateRopa(Long ropaId) {
        if (ropaId != null && ropaRepository.findById(ropaId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ROPA not found");
        }
    }

    private static void assertEditable(PrivacyDpia row) {
        if (row.getStatus() == DpiaStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled DPIA cannot be edited");
        }
        if (row.getStatus() == DpiaStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approved DPIA cannot be edited");
        }
        if (row.getStatus() == DpiaStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DPIA under review cannot be edited");
        }
        if (row.getStatus() == DpiaStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Assigned workbook DPIA cannot be edited until Contributor submits or Admin rejects");
        }
    }

    /**
     * Opens (or reuses) an In Progress DPIA for a ROPA activity the Admin marked DPIA required.
     * Prefill runs once at create; later ROPA edits never overwrite the DPIA.
     */
    @Transactional
    public PrivacyDpia spawnForRopa(User admin, PrivacyRopa ropa, String orgUnitName) {
        if (ropa.getLinkedDpiaId() != null) {
            PrivacyDpia existing = dpiaRepository.findById(ropa.getLinkedDpiaId()).orElse(null);
            if (existing != null && existing.getStatus() != DpiaStatus.CANCELLED) {
                return existing;
            }
        }
        PrivacyDpia existing = dpiaRepository.findFirstByRopaIdOrderByUpdatedAtDesc(ropa.getId())
                .filter(d -> d.getStatus() != DpiaStatus.CANCELLED)
                .orElse(null);
        if (existing != null) {
            ropa.setLinkedDpiaId(existing.getId());
            ropaRepository.save(ropa);
            return existing;
        }
        PrivacyDpia row = new PrivacyDpia();
        row.setDisplayId(nextDisplayId());
        row.setTitle(ropa.getActivityName());
        row.setRopaId(ropa.getId());
        row.setIntakeMode(DpiaIntakeMode.FORM);
        row.setPreScreeningRequired(true);
        row.setPreScreeningOutcome("DPIA_REQUIRED");
        row.setScreeningQ1(ropa.getActivityName());
        row.setScreeningQ2(orgUnitName);
        row.setScreeningQ3(true);
        row.setProcessingDescription(ropa.getProcessingDescription());
        row.setQuestionsJson(buildPrefillQuestionsJson(ropa));
        row.setStatus(DpiaStatus.IN_PROGRESS);
        row.setCreatedByUserId(admin.getId());
        dpiaRepository.save(row);
        ropa.setLinkedDpiaId(row.getId());
        ropaRepository.save(ropa);
        audit(admin, AuditActionType.CREATED, row, null, null, null);
        return row;
    }

    /**
     * Admin switched DPIA required to No. Cancels a still-untouched spawned DPIA; 409 once the DPO has continued it.
     */
    @Transactional
    public void cancelSpawnedForRopa(User admin, PrivacyRopa ropa) {
        if (ropa.getLinkedDpiaId() == null) {
            return;
        }
        PrivacyDpia dpia = dpiaRepository.findById(ropa.getLinkedDpiaId()).orElse(null);
        if (dpia == null) {
            ropa.setLinkedDpiaId(null);
            ropaRepository.save(ropa);
            return;
        }
        if (dpia.getStatus() == DpiaStatus.CANCELLED) {
            ropa.setLinkedDpiaId(null);
            ropaRepository.save(ropa);
            return;
        }
        boolean untouched = dpia.getStatus() == DpiaStatus.IN_PROGRESS
                && !hasProgressBeyondPrefill(dpia, ropa);
        if (!untouched) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "DPIA " + dpia.getDisplayId() + " is already in progress. Finish or reject it before marking DPIA not required");
        }
        dpia.setStatus(DpiaStatus.CANCELLED);
        dpiaRepository.save(dpia);
        ropa.setLinkedDpiaId(null);
        ropaRepository.save(ropa);
    }

    private static boolean hasProgressBeyondPrefill(PrivacyDpia dpia, PrivacyRopa ropa) {
        if (dpia.getScreeningQ4() != null || dpia.getScreeningQ5() != null
                || dpia.getScreeningQ6() != null || dpia.getScreeningQ7() != null) {
            return true;
        }
        if (dpia.getAssessmentJson() != null && !dpia.getAssessmentJson().isBlank()) {
            return true;
        }
        if (dpia.getNecessityAssessment() != null || dpia.getRisksIdentified() != null
                || dpia.getMitigationMeasures() != null || dpia.getResidualRisk() != null) {
            return true;
        }
        String expected = buildPrefillQuestionsJson(ropa);
        String actual = dpia.getQuestionsJson();
        if (actual == null || actual.isBlank()) {
            return false;
        }
        return !actual.equals(expected);
    }

    static String buildPrefillQuestionsJson(PrivacyRopa ropa) {
        java.util.List<String> rows = new java.util.ArrayList<>();
        addPrefill(rows, 1, ropa.getProcessingDescription());
        addPrefill(rows, 2, ropa.getPurpose() != null ? ropa.getPurpose() : ropa.getProcessingDescription());
        String dataTypes = joinNonBlank(ropa.getDataCategories(), ropa.getSensitiveDataCategories());
        addPrefill(rows, 3, dataTypes);
        addPrefill(rows, 4, ropa.getStorageSystems());
        addPrefill(rows, 5, "Yes: " + ropa.getDisplayId() + " " + ropa.getActivityName());
        String legal = ropa.getLegalBasesJson() != null && !ropa.getLegalBasesJson().isBlank()
                ? ropa.getLegalBasesJson() : (ropa.getLegalBasis() != null ? ropa.getLegalBasis().name() : null);
        addPrefill(rows, 10, legal);
        String sharing = joinNonBlank(
                ropa.isShareExternal() ? ropa.getShareExternalDetails() : null,
                ropa.getExternalEntityLocation(),
                ropa.getSafeguardsImplemented());
        addPrefill(rows, 12, sharing);
        addPrefill(rows, 13, ropa.getRetentionPeriod());
        addPrefill(rows, 14, ropa.getSecurityMeasures());
        return "[" + String.join(",", rows) + "]";
    }

    private static void addPrefill(java.util.List<String> rows, int number, String answer) {
        if (answer == null || answer.isBlank()) {
            return;
        }
        rows.add("{\"number\":" + number + ",\"answer\":\"" + jsonEscape(answer.trim()) + "\"}");
    }

    private static String joinNonBlank(String... values) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        if (values != null) {
            for (String v : values) {
                if (v != null && !v.isBlank()) {
                    parts.add(v.trim());
                }
            }
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String nextDisplayId() {
        int year = Year.now().getValue();
        String prefix = "DPIA-" + year + "-";
        long count = dpiaRepository.countByDisplayIdStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void audit(User user, AuditActionType action, PrivacyDpia row,
                       String field, String oldVal, String newVal) {
        auditTrailService.record(
                action, "PRIVACY_DPIA", user.getId(), primaryRole(user),
                "PRIVACY_DPIA", String.valueOf(row.getId()), row.getDisplayId(),
                field, oldVal, newVal, null, null);
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
