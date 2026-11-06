package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyRopa;
import com.a9.arbor.grc.modules.privacy.domain.RopaDepartment;
import com.a9.arbor.grc.modules.privacy.domain.RopaDepartmentStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaDesignation;
import com.a9.arbor.grc.modules.privacy.domain.RopaDpiaStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaIntakeMode;
import com.a9.arbor.grc.modules.privacy.domain.RopaLegalBasis;
import com.a9.arbor.grc.modules.privacy.domain.RopaProject;
import com.a9.arbor.grc.modules.privacy.domain.RopaProjectStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaReviewOutcome;
import com.a9.arbor.grc.modules.privacy.domain.RopaHeadOutcome;
import com.a9.arbor.grc.modules.privacy.domain.RopaStatus;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaDepartmentRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaProjectRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateRopaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaProcessImportResultResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaProjectDtos;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateRopaRequest;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class RopaProjectService {

    public static final String TYPE_ROPA_DEPARTMENT = "ROPA_DEPARTMENT";
    public static final String TYPE_ROPA_HEAD = "ROPA_HEAD";

    private final RopaProjectRepository projectRepository;
    private final RopaDepartmentRepository departmentRepository;
    private final PrivacyRopaRepository ropaRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final UserRepository userRepository;
    private final WorkTaskRepository workTaskRepository;
    private final PrivacyRopaService ropaService;
    private final PrivacyDpiaService dpiaService;
    private final NotificationService notificationService;
    private final RopaProcessImportWorkbook processImportWorkbook;

    public RopaProjectService(RopaProjectRepository projectRepository,
                              RopaDepartmentRepository departmentRepository,
                              PrivacyRopaRepository ropaRepository,
                              OrgUnitRepository orgUnitRepository,
                              UserRepository userRepository,
                              WorkTaskRepository workTaskRepository,
                              PrivacyRopaService ropaService,
                              PrivacyDpiaService dpiaService,
                              NotificationService notificationService,
                              RopaProcessImportWorkbook processImportWorkbook) {
        this.projectRepository = projectRepository;
        this.departmentRepository = departmentRepository;
        this.ropaRepository = ropaRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.userRepository = userRepository;
        this.workTaskRepository = workTaskRepository;
        this.ropaService = ropaService;
        this.dpiaService = dpiaService;
        this.notificationService = notificationService;
        this.processImportWorkbook = processImportWorkbook;
    }

    @Transactional(readOnly = true)
    public List<RopaProjectDtos.ProjectResponse> listProjects() {
        return projectRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toProject).toList();
    }

    @Transactional(readOnly = true)
    public RopaProjectDtos.ProjectResponse getProject(long id) {
        return toProject(requireProject(id));
    }

    @Transactional
    public RopaProjectDtos.ProjectResponse createProject(User admin, RopaProjectDtos.CreateProjectRequest req) {
        String name = req.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A ROPA project named \"" + name + "\" already exists");
        }
        RopaProject p = new RopaProject();
        p.setName(name);
        p.setYear(req.year() != null ? req.year() : java.time.LocalDate.now().getYear());
        p.setStatus(RopaProjectStatus.DRAFT);
        p.setCreatedByUserId(admin.getId());
        projectRepository.save(p);
        return toProject(p);
    }

    @Transactional
    public RopaProjectDtos.ProjectResponse renameProject(User admin, long id, RopaProjectDtos.CreateProjectRequest req) {
        RopaProject p = requireProject(id);
        String name = req.name().trim();
        if (projectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A ROPA project named \"" + name + "\" already exists");
        }
        p.setName(name);
        if (req.year() != null) {
            p.setYear(req.year());
        }
        projectRepository.save(p);
        return toProject(p);
    }

    @Transactional
    public RopaProjectDtos.DepartmentResponse addDepartment(User admin, long projectId,
                                                            RopaProjectDtos.AddDepartmentRequest req) {
        RopaProject p = requireProject(projectId);
        if (p.getStatus() == RopaProjectStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed project cannot take new departments");
        }
        OrgUnit unit = orgUnitRepository.findById(req.orgUnitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department (org unit) not found"));
        if (departmentRepository.existsByRopaProjectIdAndOrgUnitId(projectId, unit.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department \"" + unit.getName() + "\" is already in this project");
        }
        if (req.departmentHeadUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department Head is required");
        }
        if (req.championUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Privacy Champion is required");
        }
        validateUser(req.departmentHeadUserId(), null);
        validateUser(req.championUserId(), Role.CONTRIBUTOR);
        RopaDepartment d = new RopaDepartment();
        d.setRopaProjectId(projectId);
        d.setOrgUnitId(unit.getId());
        d.setDepartmentHeadUserId(req.departmentHeadUserId());
        d.setChampionUserId(req.championUserId());
        d.setDueDate(null);
        // Department, Head, and Champion are required at create: start assigned (In Progress).
        d.setStatus(RopaDepartmentStatus.IN_PROGRESS);
        d.setCreatedByUserId(admin.getId());
        departmentRepository.save(d);
        activateAssignment(admin, d);
        return toDepartment(d);
    }

    /** Assigns / reassigns champion + Department Head. */
    @Transactional
    public RopaProjectDtos.DepartmentResponse assignDepartment(User admin, long departmentId,
                                                               RopaProjectDtos.AssignDepartmentRequest req) {
        RopaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() == RopaDepartmentStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed department cannot be reassigned");
        }
        validateUser(req.championUserId(), Role.CONTRIBUTOR);
        validateUser(req.departmentHeadUserId(), null);
        d.setChampionUserId(req.championUserId());
        d.setDepartmentHeadUserId(req.departmentHeadUserId());
        boolean wasNotStarted = d.getStatus() == RopaDepartmentStatus.NOT_STARTED;
        if (wasNotStarted) {
            d.setStatus(RopaDepartmentStatus.IN_PROGRESS);
        }
        departmentRepository.save(d);
        activateAssignment(admin, d);
        return toDepartment(d);
    }

    /**
     * Makes an assignment live: champion work task, project moves out of Draft,
     * and the champion is notified. Used when a department is added with a
     * champion and when the champion is (re)assigned on the department page.
     */
    private void activateAssignment(User admin, RopaDepartment d) {
        WorkTask task = upsertTask(admin, d, TYPE_ROPA_DEPARTMENT, d.getChampionUserId(),
                "ROPA: " + orgUnitName(d.getOrgUnitId()),
                "Fill the ROPA processing activities for this department");
        d.setWorkTaskId(task.getId());
        departmentRepository.save(d);

        RopaProject p = requireProject(d.getRopaProjectId());
        if (p.getStatus() == RopaProjectStatus.DRAFT) {
            p.setStatus(RopaProjectStatus.IN_PROGRESS);
            projectRepository.save(p);
        }
        notificationService.trigger("ROPA_ASSIGNED", String.valueOf(d.getId()),
                List.of(String.valueOf(d.getChampionUserId())),
                notificationVars(d));
    }

    @Transactional(readOnly = true)
    public List<RopaResponse> listProcesses(long departmentId) {
        requireDepartment(departmentId);
        return ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(departmentId).stream()
                .map(RopaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ExportFile downloadProcessTemplate(User user, long departmentId) {
        RopaDepartment d = requireDepartment(departmentId);
        assertCanFill(user, d);
        return processImportWorkbook.template(orgUnitName(d.getOrgUnitId()));
    }

    @Transactional
    public RopaProcessImportResultResponse importProcesses(User user, long departmentId, byte[] bytes) {
        RopaDepartment d = requireDepartment(departmentId);
        assertCanFill(user, d);
        RopaProcessImportWorkbook.ParsedWorkbook book = processImportWorkbook.parse(bytes);
        int created = 0;
        int failed = 0;
        int ignored = 0;
        List<RopaProcessImportResultResponse.RopaProcessImportRowResult> rows = new ArrayList<>();
        for (RopaProcessImportWorkbook.ParsedProcess row : book.processes()) {
            String name = row.activityName() == null ? "" : row.activityName().trim();
            if (RopaProcessImportWorkbook.isSampleActivity(name)) {
                ignored++;
                rows.add(RopaProcessImportResultResponse.RopaProcessImportRowResult.ignored(
                        row.excelRow(),
                        name.isBlank() ? RopaProcessImportWorkbook.SAMPLE_ACTIVITY : name,
                        RopaProcessImportWorkbook.SAMPLE_IGNORED_MESSAGE));
                continue;
            }
            if (name.isBlank()) {
                failed++;
                rows.add(RopaProcessImportResultResponse.RopaProcessImportRowResult.failed(
                        row.excelRow(), name, "Processing Activity is required"));
                continue;
            }
            try {
                RopaResponse createdRow = createProcess(user, departmentId, toDraftRequest(row));
                created++;
                rows.add(RopaProcessImportResultResponse.RopaProcessImportRowResult.added(
                        row.excelRow(), createdRow.activityName(), createdRow.id()));
            } catch (ResponseStatusException e) {
                failed++;
                String msg = e.getReason() != null ? e.getReason() : e.getMessage();
                rows.add(RopaProcessImportResultResponse.RopaProcessImportRowResult.failed(
                        row.excelRow(), name, msg));
            } catch (RuntimeException e) {
                failed++;
                rows.add(RopaProcessImportResultResponse.RopaProcessImportRowResult.failed(
                        row.excelRow(), name, e.getMessage() != null ? e.getMessage() : "Import failed"));
            }
        }
        return new RopaProcessImportResultResponse(created, failed, ignored, book.skippedBlankCount(), rows);
    }

    private static CreateRopaRequest toDraftRequest(RopaProcessImportWorkbook.ParsedProcess row) {
        RopaDesignation designation = RopaProcessImportWorkbook.parseDesignation(row.designationRaw());
        String personal = RopaProcessImportWorkbook.toJsonList(row.personalDataRaw(), "List of Personal Data");
        String sensitive = RopaProcessImportWorkbook.toJsonList(
                row.sensitiveDataRaw(), "Categories of Sensitive Data");
        String subjects = RopaProcessImportWorkbook.toJsonList(row.dataSubjectsRaw(), "Data Subject categories");
        String legalJson = RopaProcessImportWorkbook.toLegalBasesJson(row.legalBasesRaw());
        RopaLegalBasis primary = RopaLegalBasis.CONSENT;
        if (legalJson != null) {
            List<String> bases = RopaSheetFields.parseList(legalJson, "Legal basis for processing");
            if (!bases.isEmpty()) {
                primary = RopaLegalBasis.valueOf(bases.get(0));
            }
        }
        Boolean shareExternal = RopaProcessImportWorkbook.parseYesNo(row.externalEntitiesRaw());
        Boolean adequate = RopaProcessImportWorkbook.parseYesNo(row.adequacyRaw());
        String internal = blank(row.internalDepartment());
        boolean shareInternal = internal != null;
        return new CreateRopaRequest(
                row.activityName().trim(),
                null, null, null, null,
                blank(row.processingDescription()),
                null,
                primary,
                personal,
                sensitive,
                subjects,
                blank(row.dataSubjectVolume()),
                null,
                blank(row.processingFrequency()),
                null,
                blank(row.sourceOfPersonalData()),
                blank(row.methodOfReceiving()),
                null,
                shareInternal,
                internal,
                null,
                null,
                shareExternal,
                blank(row.externalEntityDetail()),
                blank(row.location()),
                null,
                adequate,
                null,
                blank(row.safeguardsImplemented()),
                null,
                null,
                null,
                blank(row.retentionPeriod()),
                blank(row.securityMeasures()),
                blank(row.storageSystems()),
                null,
                null,
                blank(row.comments()),
                designation,
                RopaIntakeMode.FORM,
                null,
                null,
                legalJson,
                RopaProcessImportWorkbook.adequacyNote(row.adequacyRaw()),
                true);
    }

    private static String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    public RopaResponse createProcess(User user, long departmentId, CreateRopaRequest req) {
        RopaDepartment d = requireDepartment(departmentId);
        assertCanFill(user, d);
        boolean draft = Boolean.TRUE.equals(req.draft());
        // Draft: sheet list values checked when provided. Full save: every champion field required.
        RopaSheetFields.validateContributorFields(
                req.processingDescription(), req.dataCategories(), req.sensitiveDataCategories(),
                req.dataSubjects(), req.dataSubjectVolume(), req.processingFrequency(),
                req.sourceOfPersonalData(), req.methodOfReceiving(), req.legalBasesJson(),
                req.shareInternalDetails(), req.shareExternal(), req.shareExternalDetails(),
                req.externalEntityLocation(), req.adequacyNote(), req.safeguardsImplemented(),
                req.storageSystems(), req.retentionPeriod(), req.securityMeasures(), draft);
        CreateRopaRequest body = withDepartment(req, d);
        RopaResponse created = ropaService.create(adminActor(user), body);
        PrivacyRopa row = ropaRepository.findById(created.id()).orElseThrow();
        row.setReviewOutcome(RopaReviewOutcome.OPEN);
        row.setDraft(draft);
        ropaRepository.save(row);
        return RopaResponse.from(row);
    }

    @Transactional
    public RopaResponse updateProcess(User user, long departmentId, long processId, UpdateRopaRequest req) {
        RopaDepartment d = requireDepartment(departmentId);
        assertCanFill(user, d);
        PrivacyRopa row = requireProcess(departmentId, processId);
        if (row.getReviewOutcome() == RopaReviewOutcome.REVIEWED
                && headOutcome(row) != RopaHeadOutcome.RETURNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reviewed process is locked");
        }
        boolean draft = Boolean.TRUE.equals(req.draft());
        boolean mergedExternal = req.shareExternal() != null ? req.shareExternal() : row.isShareExternal();
        String mergedDetail = req.shareExternalDetails() != null
                ? req.shareExternalDetails() : row.getShareExternalDetails();
        String mergedInternal = req.shareInternalDetails() != null
                ? req.shareInternalDetails() : row.getShareInternalDetails();
        String mergedLocation = req.externalEntityLocation() != null
                ? req.externalEntityLocation() : row.getExternalEntityLocation();
        String mergedAdequacy = req.adequacyNote() != null ? req.adequacyNote() : row.getAdequacyNote();
        String mergedSafeguards = req.safeguardsImplemented() != null
                ? req.safeguardsImplemented() : row.getSafeguardsImplemented();
        String mergedStorage = req.storageSystems() != null ? req.storageSystems() : row.getStorageSystems();
        String mergedRetention = req.retentionPeriod() != null ? req.retentionPeriod() : row.getRetentionPeriod();
        String mergedSecurity = req.securityMeasures() != null ? req.securityMeasures() : row.getSecurityMeasures();
        String mergedDesc = req.processingDescription() != null
                ? req.processingDescription() : row.getProcessingDescription();
        String mergedSource = req.sourceOfPersonalData() != null
                ? req.sourceOfPersonalData() : row.getSourceOfPersonalData();
        String mergedMethod = req.methodOfReceiving() != null
                ? req.methodOfReceiving() : row.getMethodOfReceiving();
        String mergedPersonal = req.dataCategories() != null ? req.dataCategories() : row.getDataCategories();
        String mergedSensitive = req.sensitiveDataCategories() != null
                ? req.sensitiveDataCategories() : row.getSensitiveDataCategories();
        String mergedSubjects = req.dataSubjects() != null ? req.dataSubjects() : row.getDataSubjects();
        String mergedVolume = req.dataSubjectVolume() != null
                ? req.dataSubjectVolume() : row.getDataSubjectVolume();
        String mergedFreq = req.processingFrequency() != null
                ? req.processingFrequency() : row.getProcessingFrequency();
        String mergedBases = req.legalBasesJson() != null ? req.legalBasesJson() : row.getLegalBasesJson();
        RopaSheetFields.validateContributorFields(
                mergedDesc, mergedPersonal, mergedSensitive, mergedSubjects, mergedVolume, mergedFreq,
                mergedSource, mergedMethod, mergedBases, mergedInternal, mergedExternal, mergedDetail,
                mergedLocation, mergedAdequacy, mergedSafeguards, mergedStorage, mergedRetention,
                mergedSecurity, draft);
        RopaResponse updated = ropaService.update(adminActor(user), processId, withoutAdminFields(req));
        PrivacyRopa saved = ropaRepository.findById(processId).orElseThrow();
        saved.setDraft(draft);
        ropaRepository.save(saved);
        return RopaResponse.from(saved);
    }

    @Transactional
    public RopaProjectDtos.DepartmentResponse submitDepartment(User user, long departmentId) {
        RopaDepartment d = requireDepartment(departmentId);
        if (!Objects.equals(user.getId(), d.getChampionUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Privacy Champion can submit this department");
        }
        if (d.getStatus() != RopaDepartmentStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not In Progress");
        }
        List<PrivacyRopa> processes = ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(departmentId);
        if (processes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Add at least one processing activity before submitting");
        }
        List<String> drafts = processes.stream().filter(PrivacyRopa::isDraft)
                .map(PrivacyRopa::getActivityName).toList();
        if (!drafts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Complete the draft processing activities before submitting: " + String.join(", ", drafts));
        }
        for (PrivacyRopa row : processes) {
            if (row.getReviewOutcome() == RopaReviewOutcome.RETURNED) {
                row.setReviewOutcome(RopaReviewOutcome.OPEN);
                ropaRepository.save(row);
            }
            if (headOutcome(row) == RopaHeadOutcome.RETURNED) {
                // Fixed for the Head: needs a fresh head decision on resubmit.
                row.setHeadOutcome(RopaHeadOutcome.PENDING);
                ropaRepository.save(row);
            }
        }
        String returnFrom = d.getReturnFrom();
        boolean needsDpo = processes.stream()
                .anyMatch(r -> r.getReviewOutcome() == null || r.getReviewOutcome() == RopaReviewOutcome.OPEN);
        boolean backToHead = !needsDpo && "HEAD".equals(returnFrom);
        d.setStatus(backToHead
                ? RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF : RopaDepartmentStatus.UNDER_REVIEW);
        d.setReturnFrom(null);
        d.setReturnReason(null);
        departmentRepository.save(d);
        markTaskSubmitted(d.getWorkTaskId());
        if (backToHead && d.getDepartmentHeadUserId() != null) {
            WorkTask headTask = upsertTask(user, d, TYPE_ROPA_HEAD, d.getDepartmentHeadUserId(),
                    "ROPA sign-off: " + orgUnitName(d.getOrgUnitId()),
                    "Review the returned processes and Approve or Return each one");
            d.setHeadWorkTaskId(headTask.getId());
            departmentRepository.save(d);
            notificationService.trigger("ROPA_HEAD_SIGN_OFF", String.valueOf(d.getId()),
                    List.of(String.valueOf(d.getDepartmentHeadUserId())),
                    notificationVars(d));
        } else if (!backToHead) {
            notificationService.trigger("ROPA_SUBMITTED", String.valueOf(d.getId()),
                    List.of("OWNER", "ADMIN"),
                    notificationVars(d));
        }
        return toDepartment(d);
    }

    /** Admin review of one process: sets DPIA required + comments, reviewed or returned. */
    @Transactional
    public RopaResponse reviewProcess(User admin, long departmentId, long processId,
                                      RopaProjectDtos.ReviewProcessRequest req) {
        RopaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() != RopaDepartmentStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not Under Review");
        }
        PrivacyRopa row = requireProcess(departmentId, processId);
        boolean reviewed = "REVIEWED".equalsIgnoreCase(req.outcome());
        boolean returned = "RETURNED".equalsIgnoreCase(req.outcome());
        if (!reviewed && !returned) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outcome must be REVIEWED or RETURNED");
        }
        if (returned && (req.comments() == null || req.comments().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Comments are required to return a process");
        }
        row.setComments(blankToNull(req.comments()));
        row.setDpiaComments(blankToNull(req.dpiaComments()));
        if (returned) {
            // Criteria optional on return; keep any answers already entered.
            applyDpiaCriteriaIfPresent(row, req);
            // Recorded now, released to the Champion when the DPO finishes the
            // review round, so the DPO can review every process in one pass.
            row.setReviewOutcome(RopaReviewOutcome.RETURNED);
            row.setHeadOutcome(RopaHeadOutcome.PENDING);
            ropaRepository.save(row);
            return RopaResponse.from(row);
        }
        // Arbor sheet: Mark Reviewed requires all four DPIA criteria. Required
        // is derived (any Yes). Free Yes/No override is not allowed.
        if (req.dpiaCritLargeSensitive() == null
                || req.dpiaCritSystematicAssessment() == null
                || req.dpiaCritAutomatedProfiling() == null
                || req.dpiaCritLegalSeriousImpact() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Answer all four DPIA criteria before marking Reviewed");
        }
        row.setDpiaCritLargeSensitive(req.dpiaCritLargeSensitive());
        row.setDpiaCritSystematicAssessment(req.dpiaCritSystematicAssessment());
        row.setDpiaCritAutomatedProfiling(req.dpiaCritAutomatedProfiling());
        row.setDpiaCritLegalSeriousImpact(req.dpiaCritLegalSeriousImpact());
        boolean dpiaRequired = Boolean.TRUE.equals(req.dpiaCritLargeSensitive())
                || Boolean.TRUE.equals(req.dpiaCritSystematicAssessment())
                || Boolean.TRUE.equals(req.dpiaCritAutomatedProfiling())
                || Boolean.TRUE.equals(req.dpiaCritLegalSeriousImpact());
        // Flag only; new DPIAs live in the DPIA project journey. Mode A spawn retired.
        if (dpiaRequired) {
            row.setDpiaStatus(RopaDpiaStatus.REQUIRED);
        } else {
            dpiaService.cancelSpawnedForRopa(admin, row);
            row.setDpiaStatus(RopaDpiaStatus.NOT_REQUIRED);
        }
        row.setReviewOutcome(RopaReviewOutcome.REVIEWED);
        row.setReviewedByUserId(admin.getId());
        row.setReviewedAt(Instant.now());
        ropaRepository.save(row);
        return RopaResponse.from(row);
    }

    private static void applyDpiaCriteriaIfPresent(PrivacyRopa row, RopaProjectDtos.ReviewProcessRequest req) {
        if (req.dpiaCritLargeSensitive() != null) {
            row.setDpiaCritLargeSensitive(req.dpiaCritLargeSensitive());
        }
        if (req.dpiaCritSystematicAssessment() != null) {
            row.setDpiaCritSystematicAssessment(req.dpiaCritSystematicAssessment());
        }
        if (req.dpiaCritAutomatedProfiling() != null) {
            row.setDpiaCritAutomatedProfiling(req.dpiaCritAutomatedProfiling());
        }
        if (req.dpiaCritLegalSeriousImpact() != null) {
            row.setDpiaCritLegalSeriousImpact(req.dpiaCritLegalSeriousImpact());
        }
    }

    /**
     * The DPO finishes a review round. Every process must be decided
     * (Reviewed or Returned). Returned processes go back to the Champion
     * together; when every process is Reviewed, the ROPA goes back to the
     * Champion to send to the Department Head for sign-off (Ropa Journey step 5).
     */
    @Transactional
    public RopaProjectDtos.DepartmentResponse finishReview(User admin, long departmentId) {
        RopaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() != RopaDepartmentStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not Under Review");
        }
        List<PrivacyRopa> processes = ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(departmentId);
        if (processes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No processing activities to review");
        }
        List<String> undecided = processes.stream()
                .filter(r -> r.getReviewOutcome() == null || r.getReviewOutcome() == RopaReviewOutcome.OPEN)
                .map(PrivacyRopa::getActivityName)
                .toList();
        if (!undecided.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Review every process before completing the review: " + String.join(", ", undecided));
        }
        List<String> returnedNames = processes.stream()
                .filter(r -> r.getReviewOutcome() == RopaReviewOutcome.RETURNED)
                .map(PrivacyRopa::getActivityName)
                .toList();
        if (!returnedNames.isEmpty()) {
            String reason = (returnedNames.size() == 1 ? "1 process returned: "
                    : returnedNames.size() + " processes returned: ") + String.join(", ", returnedNames);
            d.setStatus(RopaDepartmentStatus.IN_PROGRESS);
            d.setReturnFrom("ADMIN");
            d.setReturnReason(reason);
            departmentRepository.save(d);
            reopenTask(d.getWorkTaskId());
            if (d.getChampionUserId() != null) {
                Map<String, String> vars = notificationVars(d);
                vars.put("returnReason", reason);
                notificationService.trigger("ROPA_RETURNED", String.valueOf(d.getId()),
                        List.of(String.valueOf(d.getChampionUserId())), vars);
            }
            return toDepartment(d);
        }
        d.setStatus(RopaDepartmentStatus.READY_FOR_HEAD_SIGN_OFF);
        departmentRepository.save(d);
        WorkTask task = upsertTask(admin, d, TYPE_ROPA_DEPARTMENT, d.getChampionUserId(),
                "ROPA: send to Department Head: " + orgUnitName(d.getOrgUnitId()),
                "The Data Privacy Officer has completed reviewing this ROPA. Send it to your Department Head for sign-off");
        d.setWorkTaskId(task.getId());
        departmentRepository.save(d);
        if (d.getChampionUserId() != null) {
            notificationService.trigger("ROPA_SEND_TO_HEAD", String.valueOf(d.getId()),
                    List.of(String.valueOf(d.getChampionUserId())),
                    notificationVars(d));
        }
        return toDepartment(d);
    }

    /**
     * The Privacy Champion sends the DPO-reviewed ROPA to the Department Head
     * for sign-off. Admin no longer sends it (Ropa Journey step 5).
     */
    @Transactional
    public RopaProjectDtos.DepartmentResponse sendToHead(User user, long departmentId) {
        RopaDepartment d = requireDepartment(departmentId);
        if (!Objects.equals(user.getId(), d.getChampionUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Privacy Champion can send this ROPA to the Department Head");
        }
        if (d.getStatus() != RopaDepartmentStatus.READY_FOR_HEAD_SIGN_OFF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The Data Privacy Officer must complete the review before this ROPA can be sent to the Department Head");
        }
        if (d.getDepartmentHeadUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a Department Head before sending for sign-off");
        }
        List<PrivacyRopa> processes = ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(departmentId);
        if (processes.isEmpty() || processes.stream().anyMatch(r -> r.getReviewOutcome() != RopaReviewOutcome.REVIEWED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Every processing activity must be Reviewed before sending to the Department Head");
        }
        d.setStatus(RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF);
        departmentRepository.save(d);
        WorkTask task = upsertTask(user, d, TYPE_ROPA_HEAD, d.getDepartmentHeadUserId(),
                "ROPA sign-off: " + orgUnitName(d.getOrgUnitId()),
                "Review each process and Approve or Return it, then Approve the ROPA or return it to the Privacy Champion");
        d.setHeadWorkTaskId(task.getId());
        departmentRepository.save(d);
        markTaskSubmitted(d.getWorkTaskId());
        notificationService.trigger("ROPA_HEAD_SIGN_OFF", String.valueOf(d.getId()),
                List.of(String.valueOf(d.getDepartmentHeadUserId())),
                notificationVars(d));
        return toDepartment(d);
    }

    /** Department Head decision on one process, while the department is with the Head. */
    @Transactional
    public RopaResponse headReviewProcess(User user, long departmentId, long processId,
                                          RopaProjectDtos.HeadReviewProcessRequest req) {
        RopaDepartment d = requireDepartment(departmentId);
        assertHead(user, d);
        if (d.getStatus() != RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not with the Department Head");
        }
        PrivacyRopa row = requireProcess(departmentId, processId);
        boolean approved = req != null && "APPROVED".equalsIgnoreCase(req.outcome());
        boolean returned = req != null && "RETURNED".equalsIgnoreCase(req.outcome());
        if (!approved && !returned) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outcome must be APPROVED or RETURNED");
        }
        if (returned && (req.comments() == null || req.comments().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Comments are required to return a process to the Privacy Champion");
        }
        row.setHeadOutcome(approved ? RopaHeadOutcome.APPROVED : RopaHeadOutcome.RETURNED);
        row.setHeadComments(blankToNull(req.comments()));
        row.setHeadDecidedByUserId(user.getId());
        row.setHeadDecidedAt(Instant.now());
        ropaRepository.save(row);
        return RopaResponse.from(row);
    }

    /**
     * Final head sign-off: only when every process is head-approved. The Head
     * approves processes one by one first (headReviewProcess).
     */
    @Transactional
    public RopaProjectDtos.DepartmentResponse headApprove(User user, long departmentId,
                                                          RopaProjectDtos.DecideRequest req) {
        RopaDepartment d = requireDepartment(departmentId);
        assertHead(user, d);
        if (d.getStatus() != RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not waiting for head sign-off");
        }
        List<PrivacyRopa> processes = ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(departmentId);
        List<String> notApproved = processes.stream()
                .filter(r -> headOutcome(r) != RopaHeadOutcome.APPROVED)
                .map(PrivacyRopa::getActivityName)
                .toList();
        if (processes.isEmpty() || !notApproved.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Approve every process before signing off the ROPA: " + String.join(", ", notApproved));
        }
        d.setStatus(RopaDepartmentStatus.APPROVED);
        d.setHeadNotes(blankToNull(req != null ? req.notes() : null));
        d.setHeadDecidedAt(Instant.now());
        departmentRepository.save(d);
        markTaskSubmitted(d.getHeadWorkTaskId());
        notificationService.trigger("ROPA_READY_TO_CLOSE", String.valueOf(d.getId()),
                List.of("OWNER", "ADMIN"),
                notificationVars(d));
        return toDepartment(d);
    }

    /**
     * Final head return: every process must be decided, and at least one must
     * be Returned. Only the Returned processes reopen for the Champion; the
     * Approved ones stay locked. Per-process comments carry the detail, so
     * overall notes are optional here.
     */
    @Transactional
    public RopaProjectDtos.DepartmentResponse headReturn(User user, long departmentId,
                                                         RopaProjectDtos.DecideRequest req) {
        RopaDepartment d = requireDepartment(departmentId);
        assertHead(user, d);
        if (d.getStatus() != RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department is not waiting for head sign-off");
        }
        List<PrivacyRopa> processes = ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(departmentId);
        List<String> undecided = processes.stream()
                .filter(r -> headOutcome(r) == RopaHeadOutcome.PENDING)
                .map(PrivacyRopa::getActivityName)
                .toList();
        if (!undecided.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Decide every process (Approve or Return) before sending back: "
                            + String.join(", ", undecided));
        }
        List<String> returnedNames = processes.stream()
                .filter(r -> headOutcome(r) == RopaHeadOutcome.RETURNED)
                .map(PrivacyRopa::getActivityName)
                .toList();
        if (returnedNames.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No process is marked Return. Use Approve ROPA when every process is approved");
        }
        String notes = blankToNull(req != null ? req.notes() : null);
        d.setStatus(RopaDepartmentStatus.IN_PROGRESS);
        d.setReturnFrom("HEAD");
        d.setReturnReason(notes != null ? notes
                : (returnedNames.size() == 1 ? "1 process returned: " : returnedNames.size() + " processes returned: ")
                        + String.join(", ", returnedNames));
        d.setHeadNotes(notes);
        d.setHeadDecidedAt(Instant.now());
        departmentRepository.save(d);
        markTaskSubmitted(d.getHeadWorkTaskId());
        reopenTask(d.getWorkTaskId());
        if (d.getChampionUserId() != null) {
            Map<String, String> vars = notificationVars(d);
            if (d.getReturnReason() != null) {
                vars.put("returnReason", d.getReturnReason());
            }
            notificationService.trigger("ROPA_RETURNED", String.valueOf(d.getId()),
                    List.of(String.valueOf(d.getChampionUserId())), vars);
        }
        return toDepartment(d);
    }

    @Transactional
    public RopaProjectDtos.DepartmentResponse closeDepartment(User admin, long departmentId) {
        RopaDepartment d = requireDepartment(departmentId);
        if (d.getStatus() != RopaDepartmentStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only an Approved department (head signed off) can be closed");
        }
        d.setStatus(RopaDepartmentStatus.CLOSED);
        d.setClosedAt(Instant.now());
        d.setClosedByUserId(admin.getId());
        departmentRepository.save(d);
        return toDepartment(d);
    }

    @Transactional
    public RopaProjectDtos.ProjectResponse completeProject(User admin, long projectId) {
        RopaProject p = requireProject(projectId);
        List<RopaDepartment> depts = departmentRepository.findByRopaProjectIdOrderByCreatedAtAsc(projectId);
        if (depts.isEmpty() || depts.stream().anyMatch(d -> d.getStatus() != RopaDepartmentStatus.CLOSED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Every department must be Closed before completing the project");
        }
        p.setStatus(RopaProjectStatus.COMPLETED);
        projectRepository.save(p);
        return toProject(p);
    }

    // --- helpers ---

    private CreateRopaRequest withDepartment(CreateRopaRequest req, RopaDepartment d) {
        return new CreateRopaRequest(
                req.activityName(), req.entityName(), req.sectorName(), req.divisionName(), req.sectionName(),
                req.processingDescription(), req.purpose(), req.legalBasis(), req.dataCategories(),
                req.sensitiveDataCategories(), req.dataSubjects(), req.dataSubjectVolume(),
                req.dataSubjectVolumeNote(), req.processingFrequency(), req.processingFrequencyNote(),
                req.sourceOfPersonalData(), req.methodOfReceiving(), req.recipients(), req.shareInternal(),
                req.shareInternalDetails(), req.shareOperator(), req.shareOperatorDetails(), req.shareExternal(),
                req.shareExternalDetails(), req.externalEntityLocation(), req.thirdCountryTransfers(),
                req.adequateProtection(), req.crossBorderTransferBasis(), req.safeguardsImplemented(),
                null, null, req.thirdCountryTransferDetails(), req.retentionPeriod(),
                req.securityMeasures(), req.storageSystems(), req.representativeName(),
                req.representativeEmployeeId(), null, req.designation(), req.intakeMode(),
                d.getOrgUnitId(), d.getId(), req.legalBasesJson(), req.adequacyNote(), req.draft());
    }

    /**
     * Sheet row 1: DPIA required (U) and Comments (V) are Admin fields. The journey
     * process form is Contributor-filled, so Admin values can only be set through
     * the DPO review action, never through process create or update.
     */
    private UpdateRopaRequest withoutAdminFields(UpdateRopaRequest req) {
        return new UpdateRopaRequest(
                req.activityName(), req.entityName(), req.sectorName(), req.divisionName(), req.sectionName(),
                req.processingDescription(), req.purpose(), req.legalBasis(), req.dataCategories(),
                req.sensitiveDataCategories(), req.dataSubjects(), req.dataSubjectVolume(),
                req.dataSubjectVolumeNote(), req.processingFrequency(), req.processingFrequencyNote(),
                req.sourceOfPersonalData(), req.methodOfReceiving(), req.recipients(), req.shareInternal(),
                req.shareInternalDetails(), req.shareOperator(), req.shareOperatorDetails(), req.shareExternal(),
                req.shareExternalDetails(), req.externalEntityLocation(), req.thirdCountryTransfers(),
                req.adequateProtection(), req.crossBorderTransferBasis(), req.safeguardsImplemented(),
                null, null, req.thirdCountryTransferDetails(), req.retentionPeriod(),
                req.securityMeasures(), req.storageSystems(), req.representativeName(),
                req.representativeEmployeeId(), null, req.designation(), req.orgUnitId(),
                req.ropaDepartmentId(), req.legalBasesJson(), req.adequacyNote(), req.draft());
    }

    /** Champion create/update goes through the admin ropa service for field validation; actor check already done. */
    private User adminActor(User user) {
        return user;
    }

    private void assertCanFill(User user, RopaDepartment d) {
        if (d.getStatus() == RopaDepartmentStatus.CLOSED || d.getStatus() == RopaDepartmentStatus.APPROVED
                || d.getStatus() == RopaDepartmentStatus.UNDER_REVIEW
                || d.getStatus() == RopaDepartmentStatus.READY_FOR_HEAD_SIGN_OFF
                || d.getStatus() == RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Processes cannot be edited while the department is " + d.getStatus().name());
        }
        // Word journey: the Privacy Champion fills the ROPA. Admin (DPO) only
        // reviews, so Admin cannot add or edit processing activities.
        if (!Objects.equals(user.getId(), d.getChampionUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned Privacy Champion can fill this department");
        }
    }

    private static RopaHeadOutcome headOutcome(PrivacyRopa row) {
        return row.getHeadOutcome() != null ? row.getHeadOutcome() : RopaHeadOutcome.PENDING;
    }

    private void assertHead(User user, RopaDepartment d) {
        // Sign-off belongs to the assigned Department Head only. Admin (DPO)
        // does not sign off by proxy: the DPO's step is closing afterwards.
        if (!Objects.equals(user.getId(), d.getDepartmentHeadUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Department Head can sign off this department");
        }
    }


    private WorkTask upsertTask(User admin, RopaDepartment d, String type, Long assigneeId,
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
        if (taskId == null) return;
        workTaskRepository.findById(taskId).ifPresent(t -> {
            t.setStatus(WorkTaskStatus.SUBMITTED);
            t.setSubmittedAt(Instant.now());
            workTaskRepository.save(t);
        });
    }

    private void reopenTask(Long taskId) {
        if (taskId == null) return;
        workTaskRepository.findById(taskId).ifPresent(t -> {
            t.setStatus(WorkTaskStatus.PENDING);
            t.setSubmittedAt(null);
            workTaskRepository.save(t);
        });
    }

    private void validateUser(Long userId, Role required) {
        if (userId == null) return;
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        if (required != null && !u.getRoles().contains(required)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Privacy Champion must have CONTRIBUTOR role");
        }
    }

    private RopaProject requireProject(long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ROPA project not found"));
    }

    private RopaDepartment requireDepartment(long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ROPA department not found"));
    }

    private PrivacyRopa requireProcess(long departmentId, long processId) {
        PrivacyRopa row = ropaRepository.findById(processId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processing activity not found"));
        if (!Objects.equals(row.getRopaDepartmentId(), departmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Processing activity not found in this department");
        }
        return row;
    }

    private RopaProjectDtos.ProjectResponse toProject(RopaProject p) {
        List<RopaProjectDtos.DepartmentResponse> depts = departmentRepository
                .findByRopaProjectIdOrderByCreatedAtAsc(p.getId()).stream().map(this::toDepartment).toList();
        return RopaProjectDtos.ProjectResponse.from(p, depts);
    }

    private RopaProjectDtos.DepartmentResponse toDepartment(RopaDepartment d) {
        int count = (int) ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(d.getId()).size();
        return RopaProjectDtos.DepartmentResponse.from(d, orgUnitName(d.getOrgUnitId()),
                userName(d.getDepartmentHeadUserId()), userName(d.getChampionUserId()), count);
    }

    private Map<String, String> notificationVars(RopaDepartment d) {
        Map<String, String> vars = new HashMap<>();
        String dept = orgUnitName(d.getOrgUnitId());
        vars.put("department", dept != null && !dept.isBlank() ? dept : "Department");
        RopaProject project = projectRepository.findById(d.getRopaProjectId()).orElse(null);
        String projectName = project != null ? project.getName() : null;
        vars.put("project", projectName != null && !projectName.isBlank() ? projectName : "-");
        return vars;
    }

    private String orgUnitName(Long id) {
        if (id == null) return null;
        return orgUnitRepository.findById(id).map(OrgUnit::getName).orElse(null);
    }

    private String userName(Long id) {
        if (id == null) return null;
        return userRepository.findById(id).map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail()).orElse(null);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
