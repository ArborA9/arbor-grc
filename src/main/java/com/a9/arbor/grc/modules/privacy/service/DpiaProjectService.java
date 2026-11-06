package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.privacy.domain.DpiaDepartment;
import com.a9.arbor.grc.modules.privacy.domain.DpiaDepartmentStatus;
import com.a9.arbor.grc.modules.privacy.domain.DpiaDpoAssessmentRow;
import com.a9.arbor.grc.modules.privacy.domain.DpiaHeadOutcome;
import com.a9.arbor.grc.modules.privacy.domain.DpiaInstanceQuestion;
import com.a9.arbor.grc.modules.privacy.domain.DpiaProject;
import com.a9.arbor.grc.modules.privacy.domain.DpiaProjectStatus;
import com.a9.arbor.grc.modules.privacy.domain.DpiaQuestionnaire;
import com.a9.arbor.grc.modules.privacy.domain.DpiaQuestionnaireQuestion;
import com.a9.arbor.grc.modules.privacy.domain.DpiaResponseType;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyRopa;
import com.a9.arbor.grc.modules.privacy.domain.RopaDepartment;
import com.a9.arbor.grc.modules.privacy.domain.RopaProject;
import com.a9.arbor.grc.modules.privacy.domain.RopaProjectStatus;
import com.a9.arbor.grc.modules.privacy.repo.DpiaDepartmentRepository;
import com.a9.arbor.grc.modules.privacy.repo.DpiaDpoAssessmentRowRepository;
import com.a9.arbor.grc.modules.privacy.repo.DpiaInstanceQuestionRepository;
import com.a9.arbor.grc.modules.privacy.repo.DpiaProjectRepository;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaDepartmentRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaProjectRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.DpiaProjectDtos;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class DpiaProjectService {

    public static final String TYPE_DPIA_DEPARTMENT = "DPIA_DEPARTMENT";
    public static final String TYPE_DPIA_HEAD = "DPIA_HEAD";

    private final DpiaProjectRepository projectRepository;
    private final DpiaDepartmentRepository departmentRepository;
    private final DpiaInstanceQuestionRepository instanceQuestionRepository;
    private final DpiaDpoAssessmentRowRepository assessmentRowRepository;
    private final DpiaQuestionnaireService questionnaireService;
    private final DpiaSeedData seedData;
    private final RopaProjectRepository ropaProjectRepository;
    private final RopaDepartmentRepository ropaDepartmentRepository;
    private final PrivacyRopaRepository privacyRopaRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final UserRepository userRepository;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository riskRecordRepository;

    public DpiaProjectService(DpiaProjectRepository projectRepository,
                              DpiaDepartmentRepository departmentRepository,
                              DpiaInstanceQuestionRepository instanceQuestionRepository,
                              DpiaDpoAssessmentRowRepository assessmentRowRepository,
                              DpiaQuestionnaireService questionnaireService,
                              DpiaSeedData seedData,
                              RopaProjectRepository ropaProjectRepository,
                              RopaDepartmentRepository ropaDepartmentRepository,
                              PrivacyRopaRepository privacyRopaRepository,
                              OrgUnitRepository orgUnitRepository,
                              UserRepository userRepository,
                              WorkTaskRepository workTaskRepository,
                              NotificationService notificationService,
                              ObjectMapper objectMapper,
                              com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository riskRecordRepository) {
        this.projectRepository = projectRepository;
        this.departmentRepository = departmentRepository;
        this.instanceQuestionRepository = instanceQuestionRepository;
        this.assessmentRowRepository = assessmentRowRepository;
        this.questionnaireService = questionnaireService;
        this.seedData = seedData;
        this.ropaProjectRepository = ropaProjectRepository;
        this.ropaDepartmentRepository = ropaDepartmentRepository;
        this.privacyRopaRepository = privacyRopaRepository;
        this.riskRecordRepository = riskRecordRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.userRepository = userRepository;
        this.workTaskRepository = workTaskRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DpiaProjectDtos.ProjectResponse> listProjects(User actor) {
        questionnaireService.ensureDefaultSeeded(actor);
        return projectRepository.findAll().stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .map(p -> toProject(p, false))
                .toList();
    }

    /** Non-blank Risk Titles from DPIA assessments, for Create Risk / Privacy Risk picker. */
    @Transactional(readOnly = true)
    public List<DpiaProjectDtos.RiskTitleCandidate> listRiskTitleCandidates(User actor) {
        List<DpiaDpoAssessmentRow> rows = assessmentRowRepository.findAll().stream()
                .filter(r -> blankToNull(r.getRiskTitle()) != null)
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .toList();
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, com.a9.arbor.grc.modules.risk.domain.RiskRecord> raisedByRow = new HashMap<>();
        for (com.a9.arbor.grc.modules.risk.domain.RiskRecord risk : riskRecordRepository.findBySourceDpiaAssessmentRowIdIn(
                rows.stream().map(DpiaDpoAssessmentRow::getId).toList())) {
            if (risk.getSourceDpiaAssessmentRowId() != null) {
                raisedByRow.put(risk.getSourceDpiaAssessmentRowId(), risk);
            }
        }
        Map<Long, DpiaDepartment> depts = new HashMap<>();
        Map<Long, DpiaProject> projects = new HashMap<>();
        List<DpiaProjectDtos.RiskTitleCandidate> out = new ArrayList<>();
        for (DpiaDpoAssessmentRow row : rows) {
            DpiaDepartment d = depts.computeIfAbsent(row.getDpiaDepartmentId(),
                    id -> departmentRepository.findById(id).orElse(null));
            if (d == null) {
                continue;
            }
            DpiaProject p = projects.computeIfAbsent(d.getDpiaProjectId(),
                    id -> projectRepository.findById(id).orElse(null));
            com.a9.arbor.grc.modules.risk.domain.RiskRecord raised = raisedByRow.get(row.getId());
            out.add(new DpiaProjectDtos.RiskTitleCandidate(
                    row.getId(),
                    row.getRiskTitle().trim(),
                    row.getPrinciple(),
                    d.getId(),
                    orgUnitName(d.getOrgUnitId()),
                    p != null ? p.getId() : null,
                    p != null ? p.getName() : null,
                    d.getOrgUnitId(),
                    raised != null ? raised.getId() : null,
                    raised != null ? raised.getDisplayId() : null));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public DpiaProjectDtos.ProjectResponse getProject(User actor, long id) {
        questionnaireService.ensureDefaultSeeded(actor);
        return toProject(requireProject(id), false);
    }

    @Transactional
    public DpiaProjectDtos.ProjectResponse createProject(User admin, DpiaProjectDtos.CreateProjectRequest req) {
        questionnaireService.ensureDefaultSeeded(admin);
        String name = req.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A DPIA project named \"" + name + "\" already exists");
        }
        DpiaProject p = new DpiaProject();
        p.setName(name);
        p.setYear(req.year() != null ? req.year() : LocalDate.now().getYear());
        p.setStatus(DpiaProjectStatus.DRAFT);
        p.setCreatedByUserId(admin.getId());
        if (req.linkedRopaProjectId() != null) {
            RopaProject ropa = ropaProjectRepository.findById(req.linkedRopaProjectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "ROPA project not found"));
            if (ropa.getStatus() != RopaProjectStatus.COMPLETED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Only a Completed ROPA project can be linked to a DPIA project");
            }
            p.setLinkedRopaProjectId(ropa.getId());
        }
        projectRepository.save(p);
        if (p.getLinkedRopaProjectId() != null) {
            autoPullDpiaApplicableDepartments(admin, p);
        }
        return toProject(p, false);
    }

    @Transactional
    public DpiaProjectDtos.ProjectResponse completeProject(User admin, long id) {
        DpiaProject p = requireProject(id);
        long open = departmentRepository.countByDpiaProjectIdAndStatusNot(id, DpiaDepartmentStatus.CLOSED);
        if (departmentRepository.countByDpiaProjectId(id) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add at least one department before completing");
        }
        if (open > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Every department must be Closed before the project can be completed");
        }
        p.setStatus(DpiaProjectStatus.COMPLETED);
        projectRepository.save(p);
        return toProject(p, false);
    }

    /**
     * When a DPIA project is linked to a Completed ROPA, add every ROPA department that has at least
     * one process with DPIA required. Prefills Head/Champion (and Due date) from that ROPA department;
     * if a Champion is present, snapshots the questionnaire and assigns immediately.
     */
    private void autoPullDpiaApplicableDepartments(User admin, DpiaProject p) {
        Long ropaProjectId = p.getLinkedRopaProjectId();
        if (ropaProjectId == null) {
            return;
        }
        DpiaQuestionnaire template = questionnaireService.defaultOr(null);
        boolean added = false;
        for (RopaDepartment rd : ropaDepartmentRepository.findByRopaProjectIdOrderByCreatedAtAsc(ropaProjectId)) {
            if (departmentRepository.existsByDpiaProjectIdAndOrgUnitId(p.getId(), rd.getOrgUnitId())) {
                continue;
            }
            boolean dpiaApplicable = privacyRopaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(rd.getId()).stream()
                    .anyMatch(PrivacyRopa::isDpiaRequired);
            if (!dpiaApplicable) {
                continue;
            }
            OrgUnit unit = orgUnitRepository.findById(rd.getOrgUnitId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
            DpiaDepartment d = new DpiaDepartment();
            d.setDpiaProjectId(p.getId());
            d.setOrgUnitId(unit.getId());
            d.setQuestionnaireId(template.getId());
            d.setDepartmentHeadUserId(rd.getDepartmentHeadUserId());
            d.setChampionUserId(rd.getChampionUserId());
            d.setDueDate(rd.getDueDate());
            d.setHeadOutcome(DpiaHeadOutcome.PENDING);
            d.setCreatedByUserId(admin.getId());
            if (rd.getChampionUserId() != null) {
                d.setStatus(DpiaDepartmentStatus.IN_PROGRESS);
                departmentRepository.save(d);
                snapshotAndAssign(admin, d, template);
            } else {
                d.setStatus(DpiaDepartmentStatus.NOT_STARTED);
                departmentRepository.save(d);
            }
            added = true;
        }
        if (added) {
            markProjectInProgress(p);
        }
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse addDepartment(User admin, long projectId,
                                                            DpiaProjectDtos.AddDepartmentRequest req) {
        DpiaProject p = requireProject(projectId);
        if (p.getStatus() == DpiaProjectStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed project cannot take new departments");
        }
        OrgUnit unit = orgUnitRepository.findById(req.orgUnitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department (org unit) not found"));
        if (departmentRepository.existsByDpiaProjectIdAndOrgUnitId(projectId, unit.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department \"" + unit.getName() + "\" is already in this project");
        }
        if (p.getLinkedRopaProjectId() == null) {
            String reason = blankToNull(req.reasonDescription());
            if (reason == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Short description and reason for the DPIA is required when not linked to ROPA");
            }
        }
        validateUser(req.departmentHeadUserId(), null);
        validateUser(req.championUserId(), Role.CONTRIBUTOR);
        DpiaQuestionnaire template = questionnaireService.defaultOr(req.questionnaireId());
        DpiaDepartment d = new DpiaDepartment();
        d.setDpiaProjectId(projectId);
        d.setOrgUnitId(unit.getId());
        d.setReasonDescription(blankToNull(req.reasonDescription()));
        d.setQuestionnaireId(template.getId());
        d.setDepartmentHeadUserId(req.departmentHeadUserId());
        d.setChampionUserId(req.championUserId());
        d.setDueDate(req.dueDate());
        d.setHeadOutcome(DpiaHeadOutcome.PENDING);
        d.setCreatedByUserId(admin.getId());
        if (req.championUserId() != null) {
            d.setStatus(DpiaDepartmentStatus.IN_PROGRESS);
            departmentRepository.save(d);
            snapshotAndAssign(admin, d, template);
        } else {
            d.setStatus(DpiaDepartmentStatus.NOT_STARTED);
            departmentRepository.save(d);
        }
        markProjectInProgress(p);
        return toDepartment(d, true);
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse assign(User admin, long departmentId,
                                                     DpiaProjectDtos.AssignDepartmentRequest req) {
        DpiaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() == DpiaDepartmentStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed department cannot be reassigned");
        }
        Long championId = req.championUserId() != null ? req.championUserId() : d.getChampionUserId();
        Long headId = req.departmentHeadUserId() != null ? req.departmentHeadUserId() : d.getDepartmentHeadUserId();
        if (championId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data Privacy Champion is required to assign");
        }
        validateUser(championId, Role.CONTRIBUTOR);
        validateUser(headId, null);
        d.setChampionUserId(championId);
        d.setDepartmentHeadUserId(headId);
        if (req.dueDate() != null) {
            d.setDueDate(req.dueDate());
        }
        DpiaQuestionnaire template = questionnaireService.defaultOr(
                req.questionnaireId() != null ? req.questionnaireId() : d.getQuestionnaireId());
        d.setQuestionnaireId(template.getId());
        boolean needsSnapshot = instanceQuestionRepository.countByDpiaDepartmentId(d.getId()) == 0;
        if (d.getStatus() == DpiaDepartmentStatus.NOT_STARTED) {
            d.setStatus(DpiaDepartmentStatus.IN_PROGRESS);
        }
        departmentRepository.save(d);
        if (needsSnapshot) {
            snapshotAndAssign(admin, d, template);
        } else {
            activateChampionTask(admin, d);
        }
        markProjectInProgress(requireProject(d.getDpiaProjectId()));
        return toDepartment(d, true);
    }

    @Transactional(readOnly = true)
    public DpiaProjectDtos.DepartmentResponse getDepartment(User actor, long departmentId) {
        questionnaireService.ensureDefaultSeeded(actor);
        return toDepartment(requireDepartment(departmentId), true);
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse saveAnswers(User user, long departmentId,
                                                          DpiaProjectDtos.SaveAnswersRequest req) {
        DpiaDepartment d = requireDepartment(departmentId);
        assertCanFill(user, d);
        if (d.getStatus() != DpiaDepartmentStatus.IN_PROGRESS
                && d.getStatus() != DpiaDepartmentStatus.NOT_STARTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Answers can only be edited while In Progress");
        }
        applyAnswers(d, req);
        return toDepartment(d, true);
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse submit(User user, long departmentId) {
        DpiaDepartment d = requireDepartment(departmentId);
        assertCanFill(user, d);
        if (d.getStatus() != DpiaDepartmentStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an In Progress DPIA can be submitted");
        }
        validateAnswersComplete(d);
        d.setStatus(DpiaDepartmentStatus.UNDER_REVIEW);
        d.setReturnFrom(null);
        d.setReturnReason(null);
        departmentRepository.save(d);
        markTaskSubmitted(d.getWorkTaskId());
        notificationService.trigger("DPIA_SUBMITTED", String.valueOf(d.getId()),
                List.of("OWNER", "ADMIN"),
                notificationVars(d));
        return toDepartment(d, true);
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse review(User admin, long departmentId,
                                                     DpiaProjectDtos.ReviewRequest req) {
        DpiaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() != DpiaDepartmentStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not under DPO review");
        }
        String outcome = req.outcome().trim().toUpperCase();
        if ("ACCEPT".equals(outcome) || "ACCEPTED".equals(outcome)) {
            d.setStatus(DpiaDepartmentStatus.DPO_ASSESSMENT);
            d.setDpoAcceptedAt(Instant.now());
            d.setDpoAcceptedByUserId(admin.getId());
            d.setReturnFrom(null);
            d.setReturnReason(null);
            ensureAssessmentRows(d);
            departmentRepository.save(d);
            return toDepartment(d, true);
        }
        if ("RETURN".equals(outcome) || "RETURNED".equals(outcome)) {
            String comments = blankToNull(req.comments());
            if (comments == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comments are required to return");
            }
            d.setStatus(DpiaDepartmentStatus.IN_PROGRESS);
            d.setReturnFrom("ADMIN");
            d.setReturnReason(comments);
            departmentRepository.save(d);
            reopenTask(d.getWorkTaskId());
            if (d.getChampionUserId() != null) {
                Map<String, String> vars = notificationVars(d);
                vars.put("returnReason", comments);
                notificationService.trigger("DPIA_RETURNED", String.valueOf(d.getId()),
                        List.of(String.valueOf(d.getChampionUserId())), vars);
            }
            return toDepartment(d, true);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outcome must be ACCEPT or RETURN");
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse saveAssessment(User admin, long departmentId,
                                                             DpiaProjectDtos.SaveAssessmentRequest req) {
        DpiaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() != DpiaDepartmentStatus.DPO_ASSESSMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "DPO assessment is only editable in DPO Assessment");
        }
        ensureAssessmentRows(d);
        if (req.rows() != null) {
            for (DpiaProjectDtos.AssessmentRowUpdate upd : req.rows()) {
                DpiaDpoAssessmentRow row = assessmentRowRepository.findByIdAndDpiaDepartmentId(upd.id(), departmentId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment row not found"));
                String compliance = blankToNull(upd.complianceStatus());
                row.setComplianceStatus(compliance);
                row.setRemarks(blankToNull(upd.remarks()));
                row.setRecommendedMeasures(blankToNull(upd.recommendedMeasures()));
                // Risk Title only applies when Non Compliant (required at finalize) or Partially Compliant (optional).
                if ("NON_COMPLIANT".equals(compliance) || "PARTIALLY_COMPLIANT".equals(compliance)) {
                    row.setRiskTitle(blankToNull(upd.riskTitle()));
                } else {
                    row.setRiskTitle(null);
                }
                assessmentRowRepository.save(row);
            }
        }
        return toDepartment(d, true);
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse finalizeAssessment(User admin, long departmentId) {
        DpiaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() != DpiaDepartmentStatus.DPO_ASSESSMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finalize is only available during DPO Assessment");
        }
        ensureAssessmentRows(d);
        for (DpiaDpoAssessmentRow row : assessmentRowRepository.findByDpiaDepartmentIdOrderBySortOrderAscIdAsc(d.getId())) {
            if (blankToNull(row.getComplianceStatus()) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Compliance status is required for every principle before finalizing");
            }
            if ("NON_COMPLIANT".equals(row.getComplianceStatus()) && blankToNull(row.getRiskTitle()) == null) {
                String label = blankToNull(row.getPrinciple()) != null ? row.getPrinciple() : "a principle";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Risk Title is required for Non Compliant principles (missing for " + label + ")");
            }
        }
        if (d.getDepartmentHeadUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assign a Department Head before sending for sign-off");
        }
        d.setAssessmentComplete(true);
        d.setStatus(DpiaDepartmentStatus.PENDING_HEAD_SIGN_OFF);
        d.setHeadOutcome(DpiaHeadOutcome.PENDING);
        d.setHeadNotes(null);
        d.setHeadDecidedAt(null);
        departmentRepository.save(d);
        WorkTask headTask = upsertTask(admin, d, TYPE_DPIA_HEAD, d.getDepartmentHeadUserId(),
                "DPIA Head sign-off: " + orgUnitName(d.getOrgUnitId()),
                "Review the completed DPIA assessment and approve or return");
        d.setHeadWorkTaskId(headTask.getId());
        departmentRepository.save(d);
        notificationService.trigger("DPIA_HEAD_SIGN_OFF", String.valueOf(d.getId()),
                List.of(String.valueOf(d.getDepartmentHeadUserId())),
                notificationVars(d));
        return toDepartment(d, true);
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse headApprove(User user, long departmentId,
                                                          DpiaProjectDtos.HeadDecideRequest req) {
        DpiaDepartment d = requireDepartment(departmentId);
        assertIsHead(user, d);
        if (d.getStatus() != DpiaDepartmentStatus.PENDING_HEAD_SIGN_OFF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not pending Head sign-off");
        }
        d.setHeadOutcome(DpiaHeadOutcome.APPROVED);
        d.setHeadNotes(blankToNull(req != null ? req.notes() : null));
        d.setHeadDecidedAt(Instant.now());
        d.setStatus(DpiaDepartmentStatus.APPROVED);
        departmentRepository.save(d);
        markTaskSubmitted(d.getHeadWorkTaskId());
        notificationService.trigger("DPIA_READY_TO_CLOSE", String.valueOf(d.getId()),
                List.of("OWNER", "ADMIN"),
                notificationVars(d));
        return toDepartment(d, true);
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse headReturn(User user, long departmentId,
                                                         DpiaProjectDtos.HeadDecideRequest req) {
        DpiaDepartment d = requireDepartment(departmentId);
        assertIsHead(user, d);
        if (d.getStatus() != DpiaDepartmentStatus.PENDING_HEAD_SIGN_OFF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not pending Head sign-off");
        }
        String notes = blankToNull(req != null ? req.notes() : null);
        if (notes == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notes are required to return");
        }
        d.setHeadOutcome(DpiaHeadOutcome.RETURNED);
        d.setHeadNotes(notes);
        d.setHeadDecidedAt(Instant.now());
        d.setStatus(DpiaDepartmentStatus.IN_PROGRESS);
        d.setReturnFrom("HEAD");
        d.setReturnReason(notes);
        d.setAssessmentComplete(false);
        departmentRepository.save(d);
        markTaskSubmitted(d.getHeadWorkTaskId());
        reopenTask(d.getWorkTaskId());
        if (d.getChampionUserId() != null) {
            Map<String, String> vars = notificationVars(d);
            vars.put("returnReason", notes);
            notificationService.trigger("DPIA_RETURNED", String.valueOf(d.getId()),
                    List.of(String.valueOf(d.getChampionUserId())), vars);
        }
        return toDepartment(d, true);
    }

    @Transactional
    public DpiaProjectDtos.DepartmentResponse close(User admin, long departmentId) {
        DpiaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() != DpiaDepartmentStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department must be Approved by the Department Head before close");
        }
        d.setStatus(DpiaDepartmentStatus.CLOSED);
        d.setClosedAt(Instant.now());
        d.setClosedByUserId(admin.getId());
        departmentRepository.save(d);
        return toDepartment(d, true);
    }

    private void snapshotAndAssign(User admin, DpiaDepartment d, DpiaQuestionnaire template) {
        instanceQuestionRepository.deleteByDpiaDepartmentId(d.getId());
        int order = 0;
        for (DpiaQuestionnaireQuestion q : questionnaireService.listQuestions(template.getId())) {
            DpiaInstanceQuestion iq = new DpiaInstanceQuestion();
            iq.setDpiaDepartmentId(d.getId());
            iq.setCode(q.getCode());
            iq.setSectionName(q.getSectionName());
            iq.setPrinciple(q.getPrinciple());
            iq.setQuestionText(q.getQuestionText());
            iq.setGuidance(q.getGuidance());
            iq.setResponseType(q.getResponseType());
            iq.setOptionsJson(q.getOptionsJson());
            iq.setSortOrder(order++);
            iq.setRequired(q.isRequired());
            instanceQuestionRepository.save(iq);
        }
        activateChampionTask(admin, d);
    }

    private void activateChampionTask(User admin, DpiaDepartment d) {
        WorkTask task = upsertTask(admin, d, TYPE_DPIA_DEPARTMENT, d.getChampionUserId(),
                "DPIA: " + orgUnitName(d.getOrgUnitId()),
                "Complete the DPIA questionnaire for this department");
        d.setWorkTaskId(task.getId());
        departmentRepository.save(d);
        notificationService.trigger("DPIA_ASSIGNED", String.valueOf(d.getId()),
                List.of(String.valueOf(d.getChampionUserId())),
                notificationVars(d));
    }

    private void ensureAssessmentRows(DpiaDepartment d) {
        if (assessmentRowRepository.countByDpiaDepartmentId(d.getId()) > 0) {
            return;
        }
        for (DpiaSeedData.SeedPrinciple p : seedData.load().principles()) {
            DpiaDpoAssessmentRow row = new DpiaDpoAssessmentRow();
            row.setDpiaDepartmentId(d.getId());
            row.setSortOrder(p.sortOrder());
            row.setPrinciple(p.principle());
            row.setDescriptionText(p.description());
            assessmentRowRepository.save(row);
        }
    }

    private void applyAnswers(DpiaDepartment d, DpiaProjectDtos.SaveAnswersRequest req) {
        if (req == null || req.answers() == null) {
            return;
        }
        for (DpiaProjectDtos.AnswerItem item : req.answers()) {
            DpiaInstanceQuestion q = instanceQuestionRepository.findByIdAndDpiaDepartmentId(item.questionId(), d.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
            q.setAnswerText(blankToNull(item.answerText()));
            q.setDetailsEvidence(blankToNull(item.detailsEvidence()));
            if (q.getResponseType() == DpiaResponseType.MULTI_SELECT) {
                q.setAnswerJson(writeOptions(item.answerOptions()));
            } else {
                q.setAnswerJson(null);
            }
            instanceQuestionRepository.save(q);
        }
    }

    private void validateAnswersComplete(DpiaDepartment d) {
        for (DpiaInstanceQuestion q : instanceQuestionRepository.findByDpiaDepartmentIdOrderBySortOrderAscIdAsc(d.getId())) {
            if (!q.isRequired()) {
                continue;
            }
            boolean ok;
            if (q.getResponseType() == DpiaResponseType.MULTI_SELECT) {
                ok = !readOptions(q.getAnswerJson()).isEmpty();
            } else {
                ok = blankToNull(q.getAnswerText()) != null;
            }
            if (!ok) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Answer required for question " + q.getCode());
            }
        }
    }

    private void assertCanFill(User user, DpiaDepartment d) {
        if (user.getRoles().contains(Role.OWNER) || user.getRoles().contains(Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Data Privacy Champion can fill the DPIA questionnaire");
        }
        if (!Objects.equals(user.getId(), d.getChampionUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned Data Privacy Champion can fill this DPIA");
        }
    }

    private void assertIsHead(User user, DpiaDepartment d) {
        if (!Objects.equals(user.getId(), d.getDepartmentHeadUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Department Head can sign off this DPIA");
        }
    }

    private void markProjectInProgress(DpiaProject p) {
        if (p.getStatus() == DpiaProjectStatus.DRAFT) {
            p.setStatus(DpiaProjectStatus.IN_PROGRESS);
            projectRepository.save(p);
        }
    }

    private WorkTask upsertTask(User admin, DpiaDepartment d, String type, Long assigneeId,
                                String title, String detail) {
        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(MyTasksService.MODULE_PRIVACY, type, d.getId())
                .orElseGet(WorkTask::new);
        task.setSourceModule(MyTasksService.MODULE_PRIVACY);
        task.setSourceRecordType(type);
        task.setSourceRecordId(d.getId());
        task.setTitle(title);
        task.setDetail(detail);
        task.setAssignedToUserId(assigneeId);
        task.setAssignedByUserId(admin.getId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(d.getDueDate());
        task.setSubmittedAt(null);
        workTaskRepository.save(task);
        return task;
    }

    private void markTaskSubmitted(Long taskId) {
        if (taskId == null) {
            return;
        }
        workTaskRepository.findById(taskId).ifPresent(t -> {
            t.setStatus(WorkTaskStatus.SUBMITTED);
            t.setSubmittedAt(Instant.now());
            workTaskRepository.save(t);
        });
    }

    private void reopenTask(Long taskId) {
        if (taskId == null) {
            return;
        }
        workTaskRepository.findById(taskId).ifPresent(t -> {
            t.setStatus(WorkTaskStatus.PENDING);
            t.setSubmittedAt(null);
            workTaskRepository.save(t);
        });
    }

    private void validateUser(Long userId, Role required) {
        if (userId == null) {
            return;
        }
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        if (required != null && !u.getRoles().contains(required)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data Privacy Champion must have CONTRIBUTOR role");
        }
    }

    private DpiaProject requireProject(long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DPIA project not found"));
    }

    private DpiaDepartment requireDepartment(long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DPIA department not found"));
    }

    private DpiaProjectDtos.ProjectResponse toProject(DpiaProject p, boolean includeDetail) {
        List<DpiaProjectDtos.DepartmentResponse> depts = departmentRepository
                .findByDpiaProjectIdOrderByIdAsc(p.getId()).stream()
                .map(d -> toDepartment(d, includeDetail))
                .toList();
        String ropaName = null;
        if (p.getLinkedRopaProjectId() != null) {
            ropaName = ropaProjectRepository.findById(p.getLinkedRopaProjectId()).map(RopaProject::getName).orElse(null);
        }
        return new DpiaProjectDtos.ProjectResponse(p.getId(), p.getName(), p.getYear(), p.getStatus(),
                p.getLinkedRopaProjectId(), ropaName, depts, p.getCreatedByUserId(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private DpiaProjectDtos.DepartmentResponse toDepartment(DpiaDepartment d, boolean includeDetail) {
        String qName = null;
        if (d.getQuestionnaireId() != null) {
            try {
                qName = questionnaireService.require(d.getQuestionnaireId()).getName();
            } catch (ResponseStatusException ignored) {
                qName = null;
            }
        }
        List<DpiaProjectDtos.InstanceQuestionResponse> questions = null;
        List<DpiaProjectDtos.AssessmentRowResponse> rows = null;
        if (includeDetail) {
            questions = instanceQuestionRepository.findByDpiaDepartmentIdOrderBySortOrderAscIdAsc(d.getId()).stream()
                    .map(this::toInstanceQuestion).toList();
            List<DpiaDpoAssessmentRow> assessmentRows =
                    assessmentRowRepository.findByDpiaDepartmentIdOrderBySortOrderAscIdAsc(d.getId());
            Map<Long, com.a9.arbor.grc.modules.risk.domain.RiskRecord> raisedByRow = new HashMap<>();
            if (!assessmentRows.isEmpty()) {
                for (com.a9.arbor.grc.modules.risk.domain.RiskRecord risk :
                        riskRecordRepository.findBySourceDpiaAssessmentRowIdIn(
                                assessmentRows.stream().map(DpiaDpoAssessmentRow::getId).toList())) {
                    if (risk.getSourceDpiaAssessmentRowId() != null) {
                        raisedByRow.put(risk.getSourceDpiaAssessmentRowId(), risk);
                    }
                }
            }
            rows = assessmentRows.stream()
                    .map(r -> {
                        com.a9.arbor.grc.modules.risk.domain.RiskRecord raised = raisedByRow.get(r.getId());
                        return new DpiaProjectDtos.AssessmentRowResponse(
                                r.getId(), r.getSortOrder(), r.getPrinciple(),
                                r.getDescriptionText(), r.getComplianceStatus(), r.getRemarks(),
                                r.getRecommendedMeasures(), r.getRiskTitle(),
                                raised != null ? raised.getId() : null,
                                raised != null ? raised.getDisplayId() : null);
                    })
                    .toList();
        }
        return new DpiaProjectDtos.DepartmentResponse(
                d.getId(), d.getDpiaProjectId(), d.getOrgUnitId(), orgUnitName(d.getOrgUnitId()),
                d.getReasonDescription(), d.getQuestionnaireId(), qName,
                d.getDepartmentHeadUserId(), userName(d.getDepartmentHeadUserId()),
                d.getChampionUserId(), userName(d.getChampionUserId()),
                d.getDueDate(), d.getStatus(), d.getReturnFrom(), d.getReturnReason(),
                d.getWorkTaskId(), d.getHeadWorkTaskId(), d.getHeadNotes(), d.getHeadDecidedAt(),
                d.getHeadOutcome(), d.isAssessmentComplete(), d.getClosedAt(),
                d.getCreatedAt(), d.getUpdatedAt(), questions, rows);
    }

    private DpiaProjectDtos.InstanceQuestionResponse toInstanceQuestion(DpiaInstanceQuestion q) {
        return new DpiaProjectDtos.InstanceQuestionResponse(
                q.getId(), q.getCode(), q.getSectionName(), q.getPrinciple(), q.getQuestionText(),
                q.getGuidance(), q.getResponseType(), readOptions(q.getOptionsJson()), q.getSortOrder(),
                q.isRequired(), q.getAnswerText(), readOptions(q.getAnswerJson()), q.getDetailsEvidence());
    }

    private Map<String, String> notificationVars(DpiaDepartment d) {
        Map<String, String> vars = new HashMap<>();
        String dept = orgUnitName(d.getOrgUnitId());
        vars.put("department", dept != null && !dept.isBlank() ? dept : "Department");
        DpiaProject project = projectRepository.findById(d.getDpiaProjectId()).orElse(null);
        String projectName = project != null ? project.getName() : null;
        vars.put("project", projectName != null && !projectName.isBlank() ? projectName : "-");
        return vars;
    }

    private String orgUnitName(Long id) {
        if (id == null) {
            return null;
        }
        return orgUnitRepository.findById(id).map(OrgUnit::getName).orElse(null);
    }

    private String userName(Long id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id)
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                .orElse(null);
    }

    private String writeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid multi-select answers");
        }
    }

    private List<String> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
