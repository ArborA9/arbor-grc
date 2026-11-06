package com.a9.arbor.grc.modules.bcms.service;

import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.bcms.domain.BiaCycle;
import com.a9.arbor.grc.modules.bcms.domain.BiaImpactCell;
import com.a9.arbor.grc.modules.bcms.domain.BiaMor;
import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.bcms.domain.BiaProject;
import com.a9.arbor.grc.modules.bcms.domain.BiaProjectStatus;
import com.a9.arbor.grc.modules.bcms.domain.BiaReviewOutcome;
import com.a9.arbor.grc.modules.bcms.domain.BiaStatus;
import com.a9.arbor.grc.modules.bcms.pack.BiaImpactEngine;
import com.a9.arbor.grc.modules.bcms.pack.BiaLookups;
import com.a9.arbor.grc.modules.bcms.pack.BiaStructuredJson;
import com.a9.arbor.grc.modules.bcms.repo.BiaCycleRepository;
import com.a9.arbor.grc.modules.bcms.repo.BiaImpactCellRepository;
import com.a9.arbor.grc.modules.bcms.repo.BiaMorRepository;
import com.a9.arbor.grc.modules.bcms.repo.BiaProcessRepository;
import com.a9.arbor.grc.modules.bcms.repo.BiaProjectRepository;
import com.a9.arbor.grc.modules.bcms.web.dto.AssignBiaRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaAssetDependency;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaCycleResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaImpactCellRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaKpiResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaLookupsResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaPersonRef;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaProcessImportResultResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaProcessResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaProjectResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaRankedRowResponse;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaThirdPartyContact;
import com.a9.arbor.grc.modules.bcms.web.dto.CreateBiaCycleRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.CreateBiaProjectRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.HeadDecideBiaRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.HeadVerifyRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.ReviewBiaProcessRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.SaveBiaImpactRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.SaveBiaMorRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.SendBiaToHeadRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.UpdateBiaCycleRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.UpsertBiaProcessRequest;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.iam.service.EmployeeDirectoryService;
import com.a9.arbor.grc.modules.iam.web.dto.EmployeeDirectoryEntry;
import com.a9.arbor.grc.modules.iam.web.dto.EmployeeDirectorySearchResponse;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.domain.WorkTaskStatus;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class BiaService {

    private final BiaProjectRepository projectRepository;
    private final BiaCycleRepository cycleRepository;
    private final BiaProcessRepository processRepository;
    private final BiaImpactCellRepository impactRepository;
    private final BiaMorRepository morRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final UserRepository userRepository;
    private final WorkTaskRepository workTaskRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;
    private final BiaWorkbookWriter biaWorkbookWriter;
    private final BiaProcessImportWorkbook processImportWorkbook;
    private final EmployeeDirectoryService employeeDirectoryService;

    public BiaService(BiaProjectRepository projectRepository,
                      BiaCycleRepository cycleRepository,
                      BiaProcessRepository processRepository,
                      BiaImpactCellRepository impactRepository,
                      BiaMorRepository morRepository,
                      OrgUnitRepository orgUnitRepository,
                      UserRepository userRepository,
                      WorkTaskRepository workTaskRepository,
                      NotificationService notificationService,
                      AuditTrailService auditTrailService,
                      BiaWorkbookWriter biaWorkbookWriter,
                      BiaProcessImportWorkbook processImportWorkbook,
                      EmployeeDirectoryService employeeDirectoryService) {
        this.projectRepository = projectRepository;
        this.cycleRepository = cycleRepository;
        this.processRepository = processRepository;
        this.impactRepository = impactRepository;
        this.morRepository = morRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.userRepository = userRepository;
        this.workTaskRepository = workTaskRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
        this.biaWorkbookWriter = biaWorkbookWriter;
        this.processImportWorkbook = processImportWorkbook;
        this.employeeDirectoryService = employeeDirectoryService;
    }

    @Transactional(readOnly = true)
    public List<BiaProjectResponse> listProjects() {
        return projectRepository.findAllByOrderByYearDesc().stream()
                .map(p -> BiaProjectResponse.from(p,
                        cycleRepository.findByBiaProjectIdOrderByUpdatedAtDesc(p.getId()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BiaProjectResponse getProject(long id) {
        BiaProject p = requireProject(id);
        return BiaProjectResponse.from(p,
                cycleRepository.findByBiaProjectIdOrderByUpdatedAtDesc(p.getId()).size());
    }

    @Transactional
    public BiaProjectResponse createProject(User admin, CreateBiaProjectRequest req) {
        assertAdmin(admin);
        String name = req.name().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project name is required");
        }
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A project named \"" + name + "\" already exists");
        }
        BiaProject p = new BiaProject();
        p.setYear(req.year());
        p.setName(name);
        p.setStatus(BiaProjectStatus.OPEN);
        p.setCreatedByUserId(admin.getId());
        projectRepository.save(p);
        return BiaProjectResponse.from(p, 0);
    }

    @Transactional(readOnly = true)
    public List<BiaCycleResponse> list(User viewer, Long biaProjectId, Long orgUnitId, BiaStatus status) {
        List<BiaCycle> rows;
        if (RoleAccess.isContributorOnly(viewer)) {
            Map<Long, BiaCycle> byId = new LinkedHashMap<>();
            for (BiaCycle c : cycleRepository.findByAssignedToUserIdOrderByUpdatedAtDesc(viewer.getId())) {
                byId.put(c.getId(), c);
            }
            for (BiaCycle c : cycleRepository.findByBcHeadUserIdOrderByUpdatedAtDesc(viewer.getId())) {
                byId.putIfAbsent(c.getId(), c);
            }
            rows = byId.values().stream()
                    .sorted(Comparator.comparing(BiaCycle::getUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        } else if (biaProjectId != null) {
            rows = cycleRepository.findByBiaProjectIdOrderByUpdatedAtDesc(biaProjectId);
        } else if (orgUnitId != null) {
            rows = cycleRepository.findByOrgUnitIdOrderByUpdatedAtDesc(orgUnitId);
        } else if (status != null) {
            rows = cycleRepository.findByStatusOrderByUpdatedAtDesc(status);
        } else {
            rows = cycleRepository.findAllByOrderByUpdatedAtDesc();
        }
        return rows.stream().map(c -> toResponse(c, false)).toList();
    }

    @Transactional(readOnly = true)
    public BiaCycleResponse get(User viewer, long id) {
        BiaCycle c = requireCycle(id);
        assertViewable(viewer, c);
        return toResponse(c, true);
    }

    @Transactional(readOnly = true)
    public BiaLookupsResponse lookups() {
        List<BiaLookupsResponse.OrgUnitOption> units = orgUnitRepository.findAllByOrderByLevelNumAscNameAsc()
                .stream()
                .filter(OrgUnit::isActive)
                .map(u -> new BiaLookupsResponse.OrgUnitOption(u.getId(), u.getName()))
                .toList();
        List<BiaLookupsResponse.UserOption> people = userRepository.findAll().stream()
                .filter(u -> u.getStatus() != UserStatus.DISABLED)
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(u -> new BiaLookupsResponse.UserOption(u.getId(), u.getFullName()))
                .toList();
        return BiaLookupsResponse.pack(units, people);
    }

    @Transactional(readOnly = true)
    public List<BiaProcessResponse> listApprovedProcesses() {
        List<BiaProcess> rows = new ArrayList<>();
        rows.addAll(processRepository.findByReviewOutcome(BiaReviewOutcome.VERIFIED));
        rows.addAll(processRepository.findByReviewOutcome(BiaReviewOutcome.APPROVED));
        return rows.stream()
                .map(p -> toProcessResponse(p, true))
                .toList();
    }

    @Transactional
    public BiaCycleResponse create(User admin, CreateBiaCycleRequest req) {
        assertAdmin(admin);
        BiaProject project = requireProject(req.biaProjectId());
        if (project.getStatus() == BiaProjectStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA project is closed");
        }
        if (cycleRepository.existsByBiaProjectIdAndOrgUnitId(project.getId(), req.orgUnitId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A department BIA already exists for this organisation unit in the project");
        }
        OrgUnit orgUnit = orgUnitRepository.findById(req.orgUnitId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organisation unit not found"));
        BiaCycle c = new BiaCycle();
        c.setBiaProjectId(project.getId());
        c.setOrgUnitId(req.orgUnitId());
        c.setDisplayId(resolveDisplayId(req.displayId(), orgUnit.getName(), orgUnit.getId(), project.getYear()));
        c.setIssueDate(req.issueDate() != null ? req.issueDate() : LocalDate.now());
        c.setNextReviewDate(req.nextReviewDate() != null ? req.nextReviewDate() : c.getIssueDate().plusYears(1));
        c.setVersionLabel(blankToDefault(req.versionLabel(), "1.0"));
        c.setObjective(blankToDefault(req.objective(), BiaLookups.DEFAULT_OBJECTIVE));
        c.setScopeText(blankToDefault(req.scopeText(), BiaLookups.DEFAULT_SCOPE));
        c.setMbco(blankToDefault(req.mbco(), BiaLookups.DEFAULT_MBCO));
        c.setStatus(BiaStatus.DRAFT);
        c.setCreatedByUserId(admin.getId());
        cycleRepository.save(c);
        audit(admin, AuditActionType.CREATED, c);
        return toResponse(c, true);
    }

    @Transactional
    public BiaCycleResponse updateCycle(User user, long id, UpdateBiaCycleRequest req) {
        BiaCycle c = requireCycle(id);
        assertAdmin(user);
        assertCycleNotClosed(c);
        if (req.displayId() != null && !req.displayId().isBlank() && !req.displayId().equals(c.getDisplayId())) {
            if (cycleRepository.existsByDisplayId(req.displayId().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA reference already exists");
            }
            c.setDisplayId(req.displayId().trim());
        }
        if (req.issueDate() != null) {
            c.setIssueDate(req.issueDate());
        }
        if (req.nextReviewDate() != null) {
            c.setNextReviewDate(req.nextReviewDate());
        }
        if (req.versionLabel() != null && !req.versionLabel().isBlank()) {
            c.setVersionLabel(req.versionLabel().trim());
        }
        if (req.objective() != null) {
            c.setObjective(blankToNull(req.objective()));
        }
        if (req.scopeText() != null) {
            c.setScopeText(blankToNull(req.scopeText()));
        }
        if (req.mbco() != null) {
            c.setMbco(blankToNull(req.mbco()));
        }
        cycleRepository.save(c);
        audit(user, AuditActionType.UPDATED, c);
        return toResponse(c, true);
    }

    @Transactional
    public BiaCycleResponse assign(User admin, long id, AssignBiaRequest req) {
        assertAdmin(admin);
        BiaCycle c = requireCycle(id);
        if (isClosed(c)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA is closed");
        }
        if (c.getStatus() == BiaStatus.PENDING_BC_HEAD || c.getStatus() == BiaStatus.READY_TO_CLOSE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA is with BC Head or ready to close");
        }
        User assignee = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
        if (!assignee.getRoles().contains(Role.CONTRIBUTOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must have CONTRIBUTOR role");
        }
        DateRules.requireActionDueDateIfPresent(req.dueDate(), "Due date");

        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_BCMS, MyTasksService.TYPE_BIA, c.getId())
                .orElseGet(WorkTask::new);
        task.setSourceModule(MyTasksService.MODULE_BCMS);
        task.setSourceRecordType(MyTasksService.TYPE_BIA);
        task.setSourceRecordId(c.getId());
        task.setTitle("BIA: " + c.getDisplayId());
        task.setDetail("Business Impact Analysis for org unit " + c.getOrgUnitId());
        task.setAssignedToUserId(assignee.getId());
        task.setAssignedByUserId(admin.getId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(req.dueDate());
        task.setSubmittedAt(null);
        workTaskRepository.save(task);

        c.setAssignedToUserId(assignee.getId());
        c.setWorkTaskId(task.getId());
        if (req.dueDate() != null) {
            c.setDueDate(req.dueDate());
        }
        c.setStatus(BiaStatus.ASSIGNED);
        c.setReturnReason(null);
        cycleRepository.save(c);

        notificationService.trigger("BIA_ASSIGNED", String.valueOf(c.getId()),
                List.of(assignee.getId().toString()), baseData(c));
        audit(admin, AuditActionType.UPDATED, c);
        return toResponse(c, true);
    }

    @Transactional
    public BiaProcessResponse createProcess(User user, long cycleId, UpsertBiaProcessRequest req) {
        BiaCycle c = requireCycle(cycleId);
        assertEditable(user, c);
        String code = req.processCode().trim();
        if (processRepository.existsByCycleIdAndProcessCode(cycleId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Process code already exists in this BIA");
        }
        BiaProcess p = new BiaProcess();
        p.setCycleId(cycleId);
        p.setSortOrder(processRepository.countByCycleId(cycleId));
        p.setCreatedByUserId(user.getId());
        p.setCreatedByAdmin(user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN));
        applyProcessFields(p, req);
        p.setReviewOutcome(BiaReviewOutcome.PENDING_REVIEW);
        processRepository.save(p);
        ensureImpactCells(p.getId());
        markInProgress(c);
        recalcProcess(p);
        reRank(cycleId);
        audit(user, AuditActionType.UPDATED, c);
        return toProcessResponse(processRepository.findById(p.getId()).orElseThrow(), true);
    }

    @Transactional
    public BiaProcessResponse updateProcess(User user, long processId, UpsertBiaProcessRequest req) {
        BiaProcess p = requireProcess(processId);
        BiaCycle c = requireCycle(p.getCycleId());
        assertEditableProcess(user, c, p);
        String code = req.processCode().trim();
        if (!code.equals(p.getProcessCode()) && processRepository.existsByCycleIdAndProcessCode(c.getId(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Process code already exists in this BIA");
        }
        applyProcessFields(p, req);
        processRepository.save(p);
        markInProgress(c);
        recalcProcess(p);
        reRank(c.getId());
        return toProcessResponse(processRepository.findById(p.getId()).orElseThrow(), true);
    }

    @Transactional
    public BiaCycleResponse deleteProcess(User user, long processId) {
        BiaProcess p = requireProcess(processId);
        BiaCycle c = requireCycle(p.getCycleId());
        assertEditableProcess(user, c, p);
        assertCanDeleteProcess(user, p);
        impactRepository.deleteByProcessId(processId);
        morRepository.deleteByProcessId(processId);
        processRepository.delete(p);
        reRank(c.getId());
        return toResponse(c, true);
    }

    @Transactional
    public BiaProcessResponse saveImpact(User user, long processId, SaveBiaImpactRequest req) {
        BiaProcess p = requireProcess(processId);
        BiaCycle c = requireCycle(p.getCycleId());
        assertEditableProcess(user, c, p);
        Map<String, BiaImpactCell> existing = new HashMap<>();
        for (BiaImpactCell cell : impactRepository.findByProcessId(processId)) {
            existing.put(cell.getCategory().name() + "|" + cell.getBand().name(), cell);
        }
        for (BiaImpactCellRequest in : req.cells()) {
            BiaImpactEngine.Category cat = parseCategory(in.category());
            BiaImpactEngine.Band band = parseBand(in.band());
            if (in.rating() != null && (in.rating() < 1 || in.rating() > 5)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impact rating must be 1-5");
            }
            String key = cat.name() + "|" + band.name();
            BiaImpactCell cell = existing.get(key);
            if (cell == null) {
                cell = new BiaImpactCell();
                cell.setProcessId(processId);
                cell.setCategory(cat);
                cell.setBand(band);
            }
            cell.setRating(in.rating());
            cell.setRemarks(blankToNull(in.remarks()));
            impactRepository.save(cell);
        }
        markInProgress(c);
        recalcProcess(p);
        reRank(c.getId());
        return toProcessResponse(processRepository.findById(p.getId()).orElseThrow(), true);
    }

    @Transactional
    public BiaProcessResponse saveMor(User user, long processId, SaveBiaMorRequest req) {
        BiaProcess p = requireProcess(processId);
        BiaCycle c = requireCycle(p.getCycleId());
        assertEditableProcess(user, c, p);
        recalcProcess(p);
        p = processRepository.findById(p.getId()).orElseThrow();
        if (!p.isMorRequired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "MOR is not required for Normal (Tier 4-5) processes");
        }
        BiaMor mor = morRepository.findByProcessId(processId).orElseGet(BiaMor::new);
        mor.setProcessId(processId);
        if (req.capacityPct() != null && (req.capacityPct() < 0 || req.capacityPct() > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacity % must be 0-100");
        }
        if (req.minStaff() != null && req.minStaff() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Min staff must be 0 or greater");
        }
        if (req.laptops() != null && req.laptops() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Laptops must be 0 or greater");
        }
        if (req.recoveryStrategies() != null) {
            for (String s : req.recoveryStrategies()) {
                if (s != null && !s.isBlank() && !BiaLookups.RECOVERY_STRATEGIES.contains(s)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown recovery strategy");
                }
            }
        }
        mor.setCapacityPct(req.capacityPct());
        List<BiaPersonRef> staffPeople = req.minStaffPeople() == null ? List.of() : req.minStaffPeople();
        assertMorStaffSubsetOfDependency(p, staffPeople);
        mor.setMinStaffPeople(BiaStructuredJson.writePeople(staffPeople));
        mor.setMinStaff(staffPeople.isEmpty() ? req.minStaff() : staffPeople.size());
        mor.setPeople(blankToNull(req.people()));
        mor.setTechnology(blankToNull(req.technology()));
        mor.setEquipment(blankToNull(req.equipment()));
        mor.setRecordsText(blankToNull(req.recordsText()));
        mor.setThirdPartySupport(blankToNull(req.thirdPartySupport()));
        List<String> strategies = req.recoveryStrategies() == null ? List.of() : req.recoveryStrategies().stream()
                .filter(s -> s != null && !s.isBlank())
                .toList();
        mor.setRecoveryStrategies(BiaStructuredJson.writeStrings(strategies));
        mor.setRecoveryStrategy(strategies.isEmpty() ? blankToNull(req.recoveryStrategy()) : strategies.get(0));
        boolean otherSelected = strategies.stream().anyMatch(BiaLookups.RECOVERY_OTHER::equals)
                || (strategies.isEmpty() && BiaLookups.RECOVERY_OTHER.equals(blankToNull(req.recoveryStrategy())));
        String otherComments = otherSelected ? blankToNull(req.workaround()) : null;
        if (Boolean.TRUE.equals(req.enforceRequired()) && otherSelected && otherComments == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Comments are required when Other is selected");
        }
        mor.setWorkaround(otherComments);
        mor.setInternetYn(yn(req.internetConnectivity()));
        mor.setIntranetYn(yn(req.intranetConnectivity()));
        mor.setVpnYn(yn(req.vpnAccess()));
        String site = siteType(req.siteType());
        mor.setSiteType(site);
        String siteName = BiaLookups.SITE_OTHER.equals(site) || site == null
                ? blankToNull(req.premises())
                : null;
        if (Boolean.TRUE.equals(req.enforceRequired()) && BiaLookups.SITE_OTHER.equals(site) && siteName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Other site is required when Other is selected");
        }
        mor.setPremises(siteName);
        List<String> apps = req.softwareApplications() == null ? List.of() : req.softwareApplications().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
        mor.setSoftwareApps(BiaStructuredJson.writeStrings(apps));
        mor.setLaptops(req.laptops());
        mor.setM365Yn(yn(req.m365()));
        List<BiaThirdPartyContact> contacts = req.thirdPartyContacts() == null ? List.of() : req.thirdPartyContacts();
        mor.setThirdPartyContacts(BiaStructuredJson.writeContacts(contacts));
        morRepository.save(mor);
        markInProgress(c);
        recalcProcess(p);
        reRank(c.getId());
        return toProcessResponse(processRepository.findById(p.getId()).orElseThrow(), true);
    }

    @Transactional
    public BiaCycleResponse submit(User user, long id) {
        BiaCycle c = requireCycle(id);
        assertEditable(user, c);
        List<BiaProcess> processes = inScope(c.getId());
        if (processes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add at least one process before submitting");
        }
        List<String> blocked = new ArrayList<>();
        for (BiaProcess p : processes) {
            recalcProcess(p);
            BiaProcess fresh = processRepository.findById(p.getId()).orElseThrow();
            if (!fresh.isCompletenessOk()) {
                blocked.add(fresh.getProcessCode() + " (" + fresh.getCompletenessLabel() + ")");
            }
        }
        if (!blocked.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Every process must be Complete before submit: " + String.join("; ", blocked));
        }
        Instant now = Instant.now();
        for (BiaProcess p : processes) {
            if (p.getReviewOutcome() == BiaReviewOutcome.RETURNED_FOR_REVISION
                    || p.getReviewOutcome() == BiaReviewOutcome.PENDING_REVIEW) {
                p.setReviewOutcome(BiaReviewOutcome.PENDING_REVIEW);
                p.setSubmittedByUserId(user.getId());
                p.setSubmittedAt(now);
                processRepository.save(p);
            }
        }
        c.setStatus(BiaStatus.SUBMITTED);
        c.setReturnReason(null);
        cycleRepository.save(c);
        if (c.getWorkTaskId() != null) {
            workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                t.setStatus(WorkTaskStatus.SUBMITTED);
                t.setSubmittedAt(now);
                workTaskRepository.save(t);
            });
        }
        notificationService.trigger("BIA_SUBMITTED", String.valueOf(c.getId()),
                List.of("OWNER", "ADMIN"), baseData(c));
        audit(user, AuditActionType.UPDATED, c);
        return toResponse(c, true);
    }

    @Transactional
    public BiaProcessResponse reviewProcess(User admin, long processId, ReviewBiaProcessRequest req) {
        assertAdmin(admin);
        BiaProcess p = requireProcess(processId);
        BiaCycle c = requireCycle(p.getCycleId());
        if (c.getStatus() != BiaStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    c.getStatus() == BiaStatus.RETURNED
                            ? "Review is locked while this BIA is with the Champion"
                            : "BIA must be submitted before review");
        }
        BiaReviewOutcome outcome = BiaReviewOutcome.fromDisplay(req.outcome());
        if (outcome == BiaReviewOutcome.PENDING_REVIEW
                || outcome == BiaReviewOutcome.APPROVED
                || outcome == BiaReviewOutcome.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose Verified or Returned for Revision");
        }
        p.setReviewOutcome(outcome);
        p.setReviewerUserId(admin.getId());
        p.setReviewerName(admin.getFullName() != null ? admin.getFullName() : admin.getEmail());
        p.setReviewDate(LocalDate.now());
        p.setReviewComment(blankToNull(req.comment()));
        if (outcome == BiaReviewOutcome.RETURNED_FOR_REVISION && (req.comment() == null || req.comment().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment is required when returning a process");
        }
        processRepository.save(p);

        if (outcome == BiaReviewOutcome.RETURNED_FOR_REVISION) {
            c.setStatus(BiaStatus.RETURNED);
            c.setReturnReason(req.comment());
            cycleRepository.save(c);
            if (c.getWorkTaskId() != null) {
                workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                    t.setStatus(WorkTaskStatus.PENDING);
                    t.setSubmittedAt(null);
                    workTaskRepository.save(t);
                });
            }
            List<String> recipients = new ArrayList<>(List.of("ADMIN", "OWNER"));
            if (c.getAssignedToUserId() != null) {
                recipients.add(c.getAssignedToUserId().toString());
            }
            Map<String, String> data = baseData(c);
            data.put("reason", req.comment());
            notificationService.trigger("BIA_RETURNED", String.valueOf(c.getId()), recipients, data);
        } else {
            refreshCycleApproval(c, admin);
        }
        audit(admin, AuditActionType.UPDATED, c);
        return toProcessResponse(processRepository.findById(p.getId()).orElseThrow(), true);
    }

    @Transactional(readOnly = true)
    public ExportFile exportExcel(User viewer, long cycleId) {
        BiaCycle c = requireCycle(cycleId);
        assertViewable(viewer, c);
        return biaWorkbookWriter.export(c, processRepository.findByCycleIdOrderBySortOrderAscIdAsc(cycleId),
                this::impactOf, this::morOf);
    }

    @Transactional(readOnly = true)
    public ExportFile downloadTemplate(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }
        return processImportWorkbook.template("bia", ownerEmails());
    }

    @Transactional(readOnly = true)
    public ExportFile downloadProcessTemplate(User user, long cycleId) {
        BiaCycle c = requireCycle(cycleId);
        assertViewable(user, c);
        return processImportWorkbook.template(c.getDisplayId(), ownerEmails());
    }

    @Transactional
    public BiaProcessImportResultResponse importProcesses(User user, long cycleId, byte[] bytes) {
        BiaCycle c = requireCycle(cycleId);
        assertEditable(user, c);
        BiaProcessImportWorkbook.ParsedWorkbook book = processImportWorkbook.parse(bytes);
        if (book.processes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No process rows to import. Add rows on the Processes sheet.");
        }

        Map<String, User> usersByEmail = new HashMap<>();
        for (User u : userRepository.findAll()) {
            if (u.getEmail() != null) {
                usersByEmail.put(u.getEmail().trim().toLowerCase(Locale.ROOT), u);
            }
        }
        Set<String> existingCodes = new HashSet<>();
        for (BiaProcess p : processRepository.findByCycleIdOrderBySortOrderAscIdAsc(cycleId)) {
            if (p.getProcessCode() != null) {
                existingCodes.add(p.getProcessCode().trim().toLowerCase(Locale.ROOT));
            }
        }

        Map<String, List<BiaProcessImportWorkbook.ParsedStaff>> staffByCode = groupStaff(book.staff());
        Map<String, List<BiaProcessImportWorkbook.ParsedAsset>> assetsByCode = groupAssets(book.assets());
        Map<String, List<BiaProcessImportWorkbook.ParsedImpact>> impactByCode = groupImpact(book.impact());
        Map<String, List<BiaProcessImportWorkbook.ParsedMor>> morByCode = groupMor(book.mor());
        Map<String, List<BiaProcessImportWorkbook.ParsedContact>> contactsByCode = groupContacts(book.contacts());
        Map<String, BiaPersonRef> personCache = new HashMap<>();

        List<BiaProcessImportResultResponse.BiaProcessImportRowResult> rows = new ArrayList<>();
        Set<String> seenInFile = new HashSet<>();
        int created = 0;
        int failed = 0;
        int ignored = 0;

        for (BiaProcessImportWorkbook.ParsedProcess row : book.processes()) {
            String code = row.processCode() == null ? "" : row.processCode().trim();
            String codeKey = code.toLowerCase(Locale.ROOT);
            if (BiaProcessImportWorkbook.isSampleCode(code)) {
                ignored++;
                rows.add(BiaProcessImportResultResponse.BiaProcessImportRowResult.ignored(
                        row.excelRow(),
                        code.isBlank() ? BiaProcessImportWorkbook.SAMPLE_CODE : code,
                        BiaProcessImportWorkbook.SAMPLE_IGNORED_MESSAGE));
                continue;
            }
            String fail = validateProcessRow(row, code, codeKey, existingCodes, seenInFile, usersByEmail,
                    staffByCode, assetsByCode, impactByCode, morByCode, contactsByCode, personCache);
            if (fail != null) {
                failed++;
                rows.add(BiaProcessImportResultResponse.BiaProcessImportRowResult.failed(
                        row.excelRow(), code.isBlank() ? null : code, fail));
                continue;
            }
            seenInFile.add(codeKey);
            try {
                BiaProcessResponse createdRow = persistImportedProcess(
                        user, cycleId, row, usersByEmail.get(row.ownerEmail().trim().toLowerCase(Locale.ROOT)),
                        staffByCode.getOrDefault(codeKey, List.of()),
                        assetsByCode.getOrDefault(codeKey, List.of()),
                        impactByCode.getOrDefault(codeKey, List.of()),
                        morByCode.getOrDefault(codeKey, List.of()),
                        contactsByCode.getOrDefault(codeKey, List.of()),
                        personCache);
                created++;
                existingCodes.add(codeKey);
                String note = createdRow.completenessOk() ? null : createdRow.completenessLabel();
                rows.add(BiaProcessImportResultResponse.BiaProcessImportRowResult.added(
                        row.excelRow(), code, createdRow.id(), createdRow.completenessLabel(), note));
            } catch (ResponseStatusException e) {
                failed++;
                String msg = e.getReason() == null ? "Could not create process" : e.getReason();
                rows.add(BiaProcessImportResultResponse.BiaProcessImportRowResult.failed(row.excelRow(), code, msg));
            }
        }
        audit(user, AuditActionType.UPDATED, requireCycle(cycleId));
        return new BiaProcessImportResultResponse(created, failed, ignored, book.skippedBlankCount(), rows);
    }

    @Transactional
    public BiaCycleResponse importExcel(User user, long cycleId, byte[] bytes) {
        BiaCycle c = requireCycle(cycleId);
        assertEditable(user, c);
        biaWorkbookWriter.importInto(c.getId(), bytes, (cycle, req, impact, mor) -> {
            BiaProcess existing = processRepository.findByCycleIdAndProcessCode(cycle, req.processCode().trim())
                    .orElse(null);
            if (existing == null) {
                createProcess(user, cycle, req);
                existing = processRepository.findByCycleIdAndProcessCode(cycle, req.processCode().trim()).orElseThrow();
            } else {
                updateProcess(user, existing.getId(), req);
                existing = processRepository.findById(existing.getId()).orElseThrow();
            }
            if (impact != null && !impact.cells().isEmpty()) {
                saveImpact(user, existing.getId(), impact);
            }
            existing = processRepository.findById(existing.getId()).orElseThrow();
            if (mor != null && existing.isMorRequired()) {
                saveMor(user, existing.getId(), mor);
            }
        });
        return toResponse(requireCycle(cycleId), true);
    }

    public BiaCycle requireCycle(long id) {
        return cycleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BIA not found"));
    }

    public BiaProcess requireProcess(long id) {
        return processRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BIA process not found"));
    }

    /** My Tasks still calls require(cycleId). */
    public BiaCycle require(long id) {
        return requireCycle(id);
    }

    /**
     * After Admin process review: stay SUBMITTED until Admin sends to BC Head.
     * Do not auto-close the department BIA.
     */
    private void refreshCycleApproval(BiaCycle c, User admin) {
        if (c.getStatus() == BiaStatus.RETURNED) {
            // stay RETURNED until Champion resubmits; process approvals alone do not reopen
            return;
        }
        if (c.getStatus() != BiaStatus.SUBMITTED) {
            return;
        }
        // Keep SUBMITTED so Admin can send to Head when every process is Approved.
        cycleRepository.save(c);
    }

    @Transactional
    public BiaCycleResponse sendToHead(User admin, long id, SendBiaToHeadRequest req) {
        assertAdmin(admin);
        BiaCycle c = requireCycle(id);
        if (c.getStatus() != BiaStatus.SUBMITTED && c.getStatus() != BiaStatus.RETURNED_TO_ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Send to BC Head only after Admin process review or after Head return");
        }
        List<BiaProcess> scope = inScope(c.getId());
        if (scope.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add and approve processes first");
        }
        List<String> notVerified = scope.stream()
                .filter(p -> !p.getReviewOutcome().specialistDone())
                .map(BiaProcess::getProcessCode)
                .toList();
        if (!notVerified.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Verify every in-scope process before sending to BC Head: " + String.join(", ", notVerified));
        }
        User head = userRepository.findById(req.bcHeadUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "BC Head user not found"));
        if (head.getStatus() == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BC Head user is disabled");
        }

        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_BCMS, MyTasksService.TYPE_BIA_HEAD, c.getId())
                .orElseGet(WorkTask::new);
        task.setSourceModule(MyTasksService.MODULE_BCMS);
        task.setSourceRecordType(MyTasksService.TYPE_BIA_HEAD);
        task.setSourceRecordId(c.getId());
        task.setTitle("BIA Head Review: " + c.getDisplayId());
        task.setDetail("Business Continuity Head review for " + c.getDisplayId());
        task.setAssignedToUserId(head.getId());
        task.setAssignedByUserId(admin.getId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(null);
        task.setSubmittedAt(null);
        workTaskRepository.save(task);

        c.setBcHeadUserId(head.getId());
        c.setBcHeadWorkTaskId(task.getId());
        c.setAdminForwardNotes(blankToNull(req.notes()));
        c.setBcHeadNotes(null);
        c.setBcHeadDecidedAt(null);
        c.setStatus(BiaStatus.PENDING_BC_HEAD);
        cycleRepository.save(c);

        notificationService.trigger("BIA_PENDING_BC_HEAD", String.valueOf(c.getId()),
                List.of(head.getId().toString()), baseData(c));
        audit(admin, AuditActionType.UPDATED, c);
        return toResponse(c, true);
    }

    @Transactional
    public BiaCycleResponse headVerify(User user, long id, HeadVerifyRequest req) {
        BiaCycle c = requireCycle(id);
        assertBcHead(user, c);
        if (c.getStatus() != BiaStatus.PENDING_BC_HEAD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA is not pending BC Head review");
        }
        List<Long> ids = req == null || req.processIds() == null ? List.of() : req.processIds();
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one process");
        }
        List<BiaProcess> scope = inScope(c.getId());
        Set<Long> allowed = new HashSet<>();
        for (BiaProcess p : scope) {
            allowed.add(p.getId());
        }
        for (Long processId : ids) {
            if (processId == null || !allowed.contains(processId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an in-scope process");
            }
        }
        for (BiaProcess p : scope) {
            if (ids.contains(p.getId())) {
                p.setHeadVerified(true);
                processRepository.save(p);
            }
        }
        audit(user, AuditActionType.UPDATED, c);
        return toResponse(c, true);
    }

    @Transactional
    public BiaCycleResponse headApprove(User user, long id, HeadDecideBiaRequest req) {
        BiaCycle c = requireCycle(id);
        assertBcHead(user, c);
        if (c.getStatus() != BiaStatus.PENDING_BC_HEAD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA is not pending BC Head review");
        }
        List<String> unverified = inScope(c.getId()).stream()
                .filter(p -> !p.isHeadVerified())
                .map(BiaProcess::getProcessCode)
                .toList();
        if (inScope(c.getId()).isEmpty() || !unverified.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    unverified.isEmpty()
                            ? "Verify every process before Approve"
                            : "Verify every process before Approve: " + String.join(", ", unverified));
        }
        Instant now = Instant.now();
        c.setStatus(BiaStatus.READY_TO_CLOSE);
        c.setBcHeadNotes(blankToNull(req != null ? req.notes() : null));
        c.setBcHeadDecidedAt(now);
        c.setApprovedAt(now);
        c.setApprovedByUserId(user.getId());
        cycleRepository.save(c);
        completeHeadTask(c, now);
        notificationService.trigger("BIA_READY_TO_CLOSE", String.valueOf(c.getId()),
                List.of("OWNER", "ADMIN"), baseData(c));
        audit(user, AuditActionType.UPDATED, c);
        return toResponse(c, true);
    }

    @Transactional
    public BiaCycleResponse headReturn(User user, long id, HeadDecideBiaRequest req) {
        BiaCycle c = requireCycle(id);
        assertBcHead(user, c);
        if (c.getStatus() != BiaStatus.PENDING_BC_HEAD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA is not pending BC Head review");
        }
        String notes = req != null ? req.notes() : null;
        if (notes == null || notes.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notes are required when returning to Admin");
        }
        Instant now = Instant.now();
        c.setStatus(BiaStatus.RETURNED_TO_ADMIN);
        c.setBcHeadNotes(notes.trim());
        c.setBcHeadDecidedAt(now);
        c.setReturnReason(notes.trim());
        cycleRepository.save(c);
        for (BiaProcess p : processRepository.findByCycleIdOrderBySortOrderAscIdAsc(c.getId())) {
            if (p.isHeadVerified()) {
                p.setHeadVerified(false);
                processRepository.save(p);
            }
        }
        completeHeadTask(c, now);
        Map<String, String> data = baseData(c);
        data.put("reason", notes.trim());
        notificationService.trigger("BIA_RETURNED_TO_ADMIN", String.valueOf(c.getId()),
                List.of("OWNER", "ADMIN"), data);
        audit(user, AuditActionType.UPDATED, c);
        return toResponse(c, true);
    }

    @Transactional
    public BiaCycleResponse close(User admin, long id) {
        assertAdmin(admin);
        BiaCycle c = requireCycle(id);
        if (c.getStatus() != BiaStatus.READY_TO_CLOSE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Close only after BC Head approval (Ready to close)");
        }
        Instant now = Instant.now();
        c.setStatus(BiaStatus.CLOSED);
        c.setClosedAt(now);
        c.setClosedByUserId(admin.getId());
        cycleRepository.save(c);
        notificationService.trigger("BIA_CLOSED", String.valueOf(c.getId()),
                List.of("OWNER", "ADMIN"), baseData(c));
        if (c.getAssignedToUserId() != null) {
            notificationService.trigger("BIA_CLOSED", String.valueOf(c.getId()),
                    List.of(c.getAssignedToUserId().toString()), baseData(c));
        }
        audit(admin, AuditActionType.UPDATED, c);
        return toResponse(c, true);
    }

    private void completeHeadTask(BiaCycle c, Instant now) {
        if (c.getBcHeadWorkTaskId() == null) {
            return;
        }
        workTaskRepository.findById(c.getBcHeadWorkTaskId()).ifPresent(t -> {
            t.setStatus(WorkTaskStatus.SUBMITTED);
            t.setSubmittedAt(now);
            workTaskRepository.save(t);
        });
    }

    private List<BiaProcess> inScope(long cycleId) {
        return processRepository.findByCycleIdOrderBySortOrderAscIdAsc(cycleId).stream()
                .filter(p -> p.getReviewOutcome() != BiaReviewOutcome.REJECTED)
                .toList();
    }

    private void applyProcessFields(BiaProcess p, UpsertBiaProcessRequest req) {
        p.setProcessCode(req.processCode().trim());
        p.setProcessName(req.processName().trim());
        p.setDescription(blankToNull(req.description()));
        p.setFlowTrigger(blankToNull(req.flowTrigger()));
        p.setFlowSteps(blankToNull(req.flowSteps()));
        p.setFlowOutput(blankToNull(req.flowOutput()));
        p.setThirdPartyActivities(blankToNull(req.thirdPartyActivities()));
        p.setSupplierName(blankToNull(req.supplierName()));
        if (req.ownerOrgUnitId() != null && !orgUnitRepository.existsById(req.ownerOrgUnitId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Process owner organisation unit not found");
        }
        p.setOwnerOrgUnitId(req.ownerOrgUnitId());
        if (req.ownerUserId() != null && !userRepository.existsById(req.ownerUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Process owner not found");
        }
        p.setOwnerUserId(req.ownerUserId());
        if (req.frequency() != null && !req.frequency().isBlank()
                && !BiaLookups.FREQUENCIES.contains(req.frequency())
                && !"Annually or Upon Request".equals(req.frequency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown process frequency");
        }
        p.setFrequency(blankToNull(req.frequency()));
        p.setPeakPeriod(null);
        if (req.depStaffPeople() != null) {
            p.setDepStaff(BiaStructuredJson.writePeople(req.depStaffPeople()));
        } else {
            p.setDepStaff(blankToNull(req.depStaff()));
        }
        p.setDepPremises(blankToNull(req.depPremises()));
        if (req.depAssets() != null) {
            for (var asset : req.depAssets()) {
                if (asset != null && asset.type() != null && !asset.type().isBlank()
                        && !BiaLookups.isKnownAssetType(asset.type())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown asset type");
                }
            }
            p.setDepIt(BiaStructuredJson.writeAssets(req.depAssets()));
        } else {
            p.setDepIt(blankToNull(req.depIt()));
        }
        p.setDepEquipment(null);
        p.setDepRecords(blankToNull(req.depRecords()));
        p.setDepThirdParty(blankToNull(req.depThirdParty()));
        p.setRelatedRiskCode(blankToNull(req.relatedRiskCode()));
        p.setChampionNotes(blankToNull(req.championNotes()));
    }

    private void ensureImpactCells(long processId) {
        List<BiaImpactCell> existing = impactRepository.findByProcessId(processId);
        if (existing.size() == 20) {
            return;
        }
        for (BiaImpactEngine.Category cat : BiaImpactEngine.Category.values()) {
            for (BiaImpactEngine.Band band : BiaImpactEngine.Band.values()) {
                boolean present = existing.stream()
                        .anyMatch(c -> c.getCategory() == cat && c.getBand() == band);
                if (!present) {
                    BiaImpactCell cell = new BiaImpactCell();
                    cell.setProcessId(processId);
                    cell.setCategory(cat);
                    cell.setBand(band);
                    impactRepository.save(cell);
                }
            }
        }
    }

    private void recalcProcess(BiaProcess p) {
        ensureImpactCells(p.getId());
        Integer[][] grid = BiaImpactEngine.emptyGrid();
        for (BiaImpactCell cell : impactRepository.findByProcessId(p.getId())) {
            grid[BiaImpactEngine.categoryIndex(cell.getCategory())][BiaImpactEngine.bandIndex(cell.getBand())] =
                    cell.getRating();
        }
        BiaImpactEngine.ImpactResult impact = BiaImpactEngine.calculate(grid);
        p.setPeakScore(impact.peak());
        p.setCriticalityTier(impact.tier());
        p.setCategoryLabel(impact.categoryLabel());
        p.setRtoHours(impact.rtoHours());
        p.setMaoHours(impact.maoHours());
        p.setMorRequired(impact.morRequired());
        p.setRtoCaption(impact.rtoCaption());
        if (!impact.morRequired()) {
            morRepository.deleteByProcessId(p.getId());
        }
        BiaMor mor = morRepository.findByProcessId(p.getId()).orElse(null);
        BiaImpactEngine.Completeness comp = BiaImpactEngine.completeness(processFields(p), impact, morFields(p, mor));
        p.setCompletenessOk(comp.complete());
        p.setCompletenessLabel(comp.label());
        processRepository.save(p);
    }

    private void reRank(long cycleId) {
        List<BiaProcess> processes = inScope(cycleId);
        List<BiaImpactEngine.RankInput> inputs = processes.stream()
                .map(p -> new BiaImpactEngine.RankInput(p.getId(), p.getSortOrder(), p.getCriticalityTier()))
                .toList();
        Map<Long, BiaImpactEngine.RankResult> byId = new HashMap<>();
        for (BiaImpactEngine.RankResult r : BiaImpactEngine.rank(inputs)) {
            byId.put(r.processId(), r);
        }
        for (BiaProcess p : processRepository.findByCycleIdOrderBySortOrderAscIdAsc(cycleId)) {
            BiaImpactEngine.RankResult r = byId.get(p.getId());
            if (r == null) {
                p.setRecoveryPriority(null);
                p.setCriticalRank(null);
            } else {
                p.setRecoveryPriority(r.recoveryPriority());
                p.setCriticalRank(r.criticalRank());
            }
            processRepository.save(p);
        }
    }

    private BiaCycleResponse toResponse(BiaCycle c, boolean includeProcesses) {
        List<BiaProcess> processes = processRepository.findByCycleIdOrderBySortOrderAscIdAsc(c.getId());
        BiaKpiResponse kpis = kpis(processes);
        List<BiaRankedRowResponse> ranked = ranked(processes);
        List<BiaProcessResponse> dtos = includeProcesses
                ? processes.stream().map(p -> toProcessResponse(p, true)).toList()
                : List.of();
        return BiaCycleResponse.from(c, kpis, ranked, dtos);
    }

    private BiaProcessResponse toProcessResponse(BiaProcess p, boolean withGrid) {
        List<BiaImpactCell> cells = withGrid ? impactRepository.findByProcessId(p.getId()) : List.of();
        BiaMor mor = morRepository.findByProcessId(p.getId()).orElse(null);
        Integer[][] grid = BiaImpactEngine.emptyGrid();
        for (BiaImpactCell cell : cells) {
            grid[BiaImpactEngine.categoryIndex(cell.getCategory())][BiaImpactEngine.bandIndex(cell.getBand())] =
                    cell.getRating();
        }
        BiaImpactEngine.ImpactResult impact = BiaImpactEngine.calculate(grid);
        BiaImpactEngine.Completeness comp = BiaImpactEngine.completeness(processFields(p), impact, morFields(p, mor));
        return BiaProcessResponse.from(p, cells, mor, comp.missing());
    }

    private BiaKpiResponse kpis(List<BiaProcess> processes) {
        List<BiaProcess> scope = processes.stream()
                .filter(p -> p.getReviewOutcome() != BiaReviewOutcome.REJECTED)
                .toList();
        int[] tiers = new int[6];
        int critical = 0;
        int normal = 0;
        int complete = 0;
        for (BiaProcess p : scope) {
            if (p.isCompletenessOk()) {
                complete++;
            }
            Integer t = p.getCriticalityTier();
            if (t == null) {
                continue;
            }
            if (t >= 1 && t <= 5) {
                tiers[t]++;
            }
            if (t <= 3) {
                critical++;
            } else {
                normal++;
            }
        }
        return new BiaKpiResponse(scope.size(), critical, normal, tiers[1], tiers[2], tiers[3], tiers[4], tiers[5], complete);
    }

    private List<BiaRankedRowResponse> ranked(List<BiaProcess> processes) {
        return processes.stream()
                .filter(p -> p.getReviewOutcome() != BiaReviewOutcome.REJECTED)
                .filter(p -> p.getCriticalRank() != null)
                .sorted(Comparator.comparingInt(BiaProcess::getCriticalRank))
                .map(p -> {
                    BiaMor mor = morRepository.findByProcessId(p.getId()).orElse(null);
                    return new BiaRankedRowResponse(
                            p.getCriticalRank(), p.getId(), p.getProcessName(), p.getOwnerOrgUnitId(),
                            p.getCategoryLabel(), p.getRtoHours(), p.getMaoHours(),
                            mor != null ? mor.getCapacityPct() : null,
                            mor != null ? mor.getMinStaff() : null,
                            mor != null ? mor.getRecoveryStrategy() : null);
                })
                .toList();
    }

    private List<BiaImpactCell> impactOf(long processId) {
        return impactRepository.findByProcessId(processId);
    }

    private BiaMor morOf(long processId) {
        return morRepository.findByProcessId(processId).orElse(null);
    }

    private static BiaImpactEngine.ProcessFields processFields(BiaProcess p) {
        return new BiaImpactEngine.ProcessFields(
                p.getProcessCode(), p.getProcessName(), p.getDescription(),
                p.getFlowTrigger(), p.getFlowSteps(), p.getFlowOutput(),
                p.getOwnerUserId(), p.getFrequency(),
                BiaStructuredJson.hasPeople(p.getDepStaff()),
                p.getDepPremises(),
                BiaStructuredJson.hasAssets(p.getDepIt()),
                p.getDepRecords());
    }

    private static BiaImpactEngine.MorFields morFields(BiaProcess p, BiaMor mor) {
        if (!p.isMorRequired()) {
            return new BiaImpactEngine.MorFields(false, true, true, true, true, true, true);
        }
        if (mor == null) {
            return new BiaImpactEngine.MorFields(true, false, false, false, !thirdPartyInvolved(p), true, true);
        }
        List<String> strategies = BiaStructuredJson.strings(mor.getRecoveryStrategies());
        boolean other = strategies.stream().anyMatch(BiaLookups.RECOVERY_OTHER::equals)
                || BiaLookups.RECOVERY_OTHER.equals(mor.getRecoveryStrategy());
        boolean commentsOk = !other || (mor.getWorkaround() != null && !mor.getWorkaround().isBlank());
        boolean siteOtherOk = !BiaLookups.SITE_OTHER.equals(mor.getSiteType())
                || (mor.getPremises() != null && !mor.getPremises().isBlank());
        boolean recoveryOk = !strategies.isEmpty()
                || (mor.getRecoveryStrategy() != null && !mor.getRecoveryStrategy().isBlank());
        boolean contactsOk = !thirdPartyInvolved(p) || BiaStructuredJson.hasContacts(mor.getThirdPartyContacts());
        return new BiaImpactEngine.MorFields(
                true,
                BiaStructuredJson.hasPeople(mor.getMinStaffPeople()),
                recoveryOk,
                resourcesComplete(mor),
                contactsOk,
                commentsOk,
                siteOtherOk);
    }

    private static boolean thirdPartyInvolved(BiaProcess p) {
        String v = p.getThirdPartyActivities();
        if (v == null || v.isBlank()) {
            return false;
        }
        String t = v.trim().toLowerCase();
        return !(t.equals("none") || t.equals("n/a") || t.equals("na") || t.equals("-"));
    }

    private static boolean resourcesComplete(BiaMor mor) {
        return isYn(mor.getInternetYn())
                && isYn(mor.getIntranetYn())
                && isYn(mor.getVpnYn())
                && isSite(mor.getSiteType())
                && isYn(mor.getM365Yn())
                && mor.getLaptops() != null
                && !BiaStructuredJson.strings(mor.getSoftwareApps()).isEmpty();
    }

    private static boolean isYn(String v) {
        return BiaLookups.YES.equals(v) || BiaLookups.NO.equals(v);
    }

    private static boolean isSite(String v) {
        return BiaLookups.SITE_PRIMARY.equals(v)
                || BiaLookups.SITE_SECONDARY.equals(v)
                || BiaLookups.SITE_OTHER.equals(v);
    }

    private static String yn(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (BiaLookups.YES.equalsIgnoreCase(t)) {
            return BiaLookups.YES;
        }
        if (BiaLookups.NO.equalsIgnoreCase(t)) {
            return BiaLookups.NO;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use Yes or No");
    }

    private static String siteType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (BiaLookups.SITE_PRIMARY.equalsIgnoreCase(t) || "PRIMARY_SITE".equalsIgnoreCase(t)) {
            return BiaLookups.SITE_PRIMARY;
        }
        if (BiaLookups.SITE_SECONDARY.equalsIgnoreCase(t) || "SECONDARY_SITE".equalsIgnoreCase(t)) {
            return BiaLookups.SITE_SECONDARY;
        }
        if (BiaLookups.SITE_OTHER.equalsIgnoreCase(t) || "OTHER".equalsIgnoreCase(t)) {
            return BiaLookups.SITE_OTHER;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use Primary Site, Secondary Site, or Other");
    }

    private static String personKey(BiaPersonRef p) {
        if (p == null) {
            return "";
        }
        if ("ENTRA".equalsIgnoreCase(p.source()) && p.directoryId() != null && !p.directoryId().isBlank()) {
            return "entra:" + p.directoryId().trim();
        }
        if ("ARBOR".equalsIgnoreCase(p.source()) && p.userId() != null) {
            return "arbor:" + p.userId();
        }
        String email = p.email() != null && !p.email().isBlank() ? p.email() : p.displayName();
        return "email:" + (email == null ? "" : email.trim().toLowerCase());
    }

    private static void assertMorStaffSubsetOfDependency(BiaProcess process, List<BiaPersonRef> morStaff) {
        if (morStaff == null || morStaff.isEmpty()) {
            return;
        }
        List<BiaPersonRef> dep = BiaStructuredJson.people(process.getDepStaff());
        java.util.Set<String> allowed = dep.stream()
                .filter(p -> p != null && p.displayName() != null && !p.displayName().isBlank())
                .map(BiaService::personKey)
                .collect(java.util.stream.Collectors.toSet());
        for (BiaPersonRef p : morStaff) {
            if (p == null || p.displayName() == null || p.displayName().isBlank()) {
                continue;
            }
            if (!allowed.contains(personKey(p))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Min staff must be chosen from Dependency Minimum Staff on Identity");
            }
        }
    }

    private void markInProgress(BiaCycle c) {
        if (c.getStatus() == BiaStatus.ASSIGNED || c.getStatus() == BiaStatus.RETURNED) {
            if (c.getStatus() == BiaStatus.ASSIGNED) {
                c.setStatus(BiaStatus.IN_PROGRESS);
            }
            cycleRepository.save(c);
            if (c.getWorkTaskId() != null) {
                workTaskRepository.findById(c.getWorkTaskId()).ifPresent(t -> {
                    if (t.getStatus() == WorkTaskStatus.PENDING) {
                        t.setStatus(WorkTaskStatus.IN_PROGRESS);
                        workTaskRepository.save(t);
                    }
                });
            }
        }
    }

    /**
     * Auto format: {@code BIA-{year}-{SHORT}-{orgUnitId}} (SHORT = dept initials, max 6).
     * If that id is taken, {@code …-{orgUnitId}-2}, {@code -3}, … Org unit id keeps refs unique
     * across departments that share the same initials.
     */
    private String resolveDisplayId(String requested, String departmentName, long orgUnitId, int assessmentYear) {
        if (requested != null && !requested.isBlank()) {
            String id = requested.trim();
            if (!cycleRepository.existsByDisplayId(id)) {
                if (id.length() > 64) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BIA reference is too long");
                }
                return id;
            }
            // Preview / retry collided - allocate below instead of 409.
        }
        int year = assessmentYear > 0 ? assessmentYear : LocalDate.now().getYear();
        String shortCode = shortDepartmentCode(departmentName);
        String base = "BIA-" + year + "-" + shortCode + "-" + orgUnitId;
        if (base.length() > 64) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not allocate a BIA reference");
        }
        if (!cycleRepository.existsByDisplayId(base)) {
            return base;
        }
        for (int i = 2; i < 1000; i++) {
            String id = base + "-" + i;
            if (id.length() > 64) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not allocate a BIA reference");
            }
            if (!cycleRepository.existsByDisplayId(id)) {
                return id;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Could not allocate a BIA reference");
    }

    private static final Set<String> DEPT_STOP_WORDS = Set.of(
            "AND", "OR", "THE", "OF", "FOR", "TO", "IN", "A", "AN", "AT", "BY", "ON");

    /**
     * Short department token for BIA refs: initials of significant words (max 6), or first
     * 6 alphanumerics of a single-word name.
     */
    static String shortDepartmentCode(String name) {
        if (name == null || name.isBlank()) {
            return "DEPT";
        }
        String[] parts = name.trim().toUpperCase().split("[^A-Z0-9]+");
        StringBuilder initials = new StringBuilder();
        int wordCount = 0;
        for (String part : parts) {
            if (part.isEmpty() || DEPT_STOP_WORDS.contains(part)) {
                continue;
            }
            wordCount++;
            initials.append(part.charAt(0));
            if (initials.length() >= 6) {
                return initials.substring(0, 6);
            }
        }
        if (wordCount >= 2 && initials.length() > 0) {
            return initials.toString();
        }
        // Single significant word (or only stop words): take up to 6 chars of first token.
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            return part.length() <= 6 ? part : part.substring(0, 6);
        }
        return "DEPT";
    }

    private void assertAdmin(User user) {
        if (user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private void assertViewable(User user, BiaCycle c) {
        if (RoleAccess.isContributorOnly(user)
                && !user.getId().equals(c.getAssignedToUserId())
                && !user.getId().equals(c.getBcHeadUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this BIA");
        }
    }

    private void assertEditable(User user, BiaCycle c) {
        assertCycleNotClosed(c);
        boolean admin = user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
        if (admin) {
            if (c.getStatus() != BiaStatus.DRAFT
                    && c.getStatus() != BiaStatus.ASSIGNED
                    && c.getStatus() != BiaStatus.IN_PROGRESS
                    && c.getStatus() != BiaStatus.RETURNED
                    && c.getStatus() != BiaStatus.RETURNED_TO_ADMIN) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA is not open for Admin editing");
            }
            return;
        }
        if (RoleAccess.isContributorOnly(user) || user.getRoles().contains(Role.CONTRIBUTOR)) {
            if (c.getStatus() == BiaStatus.RETURNED_TO_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Admin is updating this BIA after BC Head return");
            }
            if (!user.getId().equals(c.getAssignedToUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this BIA");
            }
            if (c.getStatus() != BiaStatus.ASSIGNED && c.getStatus() != BiaStatus.IN_PROGRESS
                    && c.getStatus() != BiaStatus.RETURNED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA is not open for editing");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this BIA");
    }

    private void assertEditableProcess(User user, BiaCycle c, BiaProcess p) {
        assertEditable(user, c);
        boolean admin = user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
        if (admin && c.getStatus() == BiaStatus.RETURNED_TO_ADMIN) {
            return;
        }
        if (p.getReviewOutcome().specialistDone()
                || p.getReviewOutcome() == BiaReviewOutcome.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This process is closed for editing");
        }
    }

    /** Admin may delete any editable process; Champion may only delete processes they added (not Admin-created). */
    private void assertCanDeleteProcess(User user, BiaProcess p) {
        boolean admin = user.getRoles().stream().anyMatch(r -> r == Role.OWNER || r == Role.ADMIN);
        if (admin) {
            return;
        }
        if (p.isCreatedByAdmin() || p.getCreatedByUserId() == null
                || !user.getId().equals(p.getCreatedByUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an Admin can delete a process that was added by Admin");
        }
    }

    private void assertCycleNotClosed(BiaCycle c) {
        if (isClosed(c)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "BIA is closed");
        }
    }

    private static boolean isClosed(BiaCycle c) {
        return c.getStatus() == BiaStatus.CLOSED || c.getStatus() == BiaStatus.APPROVED;
    }

    private BiaProject requireProject(long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BIA project not found"));
    }

    private void assertBcHead(User user, BiaCycle c) {
        if (c.getBcHeadUserId() == null || !c.getBcHeadUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned BC Head can decide");
        }
    }

    private Map<String, String> baseData(BiaCycle c) {
        Map<String, String> data = new HashMap<>();
        data.put("processName", c.getDisplayId());
        data.put("dueDate", c.getDueDate() != null ? c.getDueDate().toString() : "-");
        data.put("reviewDate", c.getNextReviewDate() != null ? c.getNextReviewDate().toString() : "-");
        data.put("reason", c.getReturnReason() != null ? c.getReturnReason() : "-");
        projectRepository.findById(c.getBiaProjectId()).ifPresent(p -> {
            if (p.getName() != null && !p.getName().isBlank()) {
                data.put("biaName", p.getName().trim());
            }
        });
        orgUnitRepository.findById(c.getOrgUnitId()).ifPresent(u -> {
            if (u.getName() != null && !u.getName().isBlank()) {
                data.put("orgUnitName", u.getName().trim());
            }
        });
        String submittedBy = personName(c.getAssignedToUserId());
        if (!submittedBy.isBlank()) {
            data.put("submittedBy", submittedBy);
        }
        if (c.getClosedAt() != null) {
            data.put("closedDate", c.getClosedAt().atZone(ZoneId.of("Asia/Dubai")).toLocalDate().toString());
        }
        return data;
    }

    private String personName(Long userId) {
        if (userId == null) {
            return "";
        }
        return userRepository.findById(userId).map(u -> {
            if (u.getFullName() != null && !u.getFullName().isBlank()) {
                return u.getFullName().trim();
            }
            return u.getEmail() == null ? "" : u.getEmail().trim();
        }).orElse("");
    }

    private void audit(User user, AuditActionType action, BiaCycle c) {
        String role = user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
        auditTrailService.record(action, "BCMS", user.getId(), role,
                "BIA", String.valueOf(c.getId()), c.getDisplayId(), null, null, null, null, null);
    }

    private static BiaImpactEngine.Category parseCategory(String raw) {
        try {
            return BiaImpactEngine.Category.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown impact category: " + raw);
        }
    }

    private static BiaImpactEngine.Band parseBand(String raw) {
        try {
            return BiaImpactEngine.Band.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown impact band: " + raw);
        }
    }

    private List<String> ownerEmails() {
        return userRepository.findAll().stream()
                .filter(u -> u.getStatus() != UserStatus.DISABLED && u.getEmail() != null && !u.getEmail().isBlank())
                .sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(u -> u.getEmail().trim())
                .toList();
    }

    private String validateProcessRow(
            BiaProcessImportWorkbook.ParsedProcess row,
            String code,
            String codeKey,
            Set<String> existingCodes,
            Set<String> seenInFile,
            Map<String, User> usersByEmail,
            Map<String, List<BiaProcessImportWorkbook.ParsedStaff>> staffByCode,
            Map<String, List<BiaProcessImportWorkbook.ParsedAsset>> assetsByCode,
            Map<String, List<BiaProcessImportWorkbook.ParsedImpact>> impactByCode,
            Map<String, List<BiaProcessImportWorkbook.ParsedMor>> morByCode,
            Map<String, List<BiaProcessImportWorkbook.ParsedContact>> contactsByCode,
            Map<String, BiaPersonRef> personCache) {
        if (code.isBlank()) {
            return "Process Code is required";
        }
        if (row.processName() == null || row.processName().isBlank()) {
            return "Business Process Name is required";
        }
        if (seenInFile.contains(codeKey)) {
            return "Process code \"" + code + "\" is duplicated in the workbook";
        }
        if (existingCodes.contains(codeKey)) {
            return "A process coded \"" + code + "\" already exists in this BIA";
        }
        if (row.ownerEmail() == null || row.ownerEmail().isBlank()) {
            return "Process Owner Email is required";
        }
        User owner = usersByEmail.get(row.ownerEmail().trim().toLowerCase(Locale.ROOT));
        if (owner == null || owner.getStatus() == UserStatus.DISABLED) {
            return "Process owner \"" + row.ownerEmail().trim() + "\" is not an Arbor user";
        }
        if (row.frequency() != null && !row.frequency().isBlank()
                && BiaLookups.resolveFrequency(row.frequency()) == null) {
            return "Unknown process frequency";
        }
        String tp = row.thirdPartyInvolved();
        if (tp != null && !tp.isBlank() && ynSafe(tp) == null) {
            return "Third Party Involved must be Yes or No";
        }
        for (BiaProcessImportWorkbook.ParsedStaff staff : staffByCode.getOrDefault(codeKey, List.of())) {
            String roleErr = staffRoleError(staff.role());
            if (roleErr != null) {
                return roleErr + " (Staff row " + staff.excelRow() + ")";
            }
            if (staff.email() == null || !staff.email().contains("@")) {
                return "Staff email is required (Staff row " + staff.excelRow() + ")";
            }
            if (resolvePerson(staff.email(), personCache) == null) {
                return "Staff email \"" + staff.email().trim() + "\" was not found in the directory";
            }
        }
        for (BiaProcessImportWorkbook.ParsedAsset asset : assetsByCode.getOrDefault(codeKey, List.of())) {
            BiaLookups.AssetType type = BiaLookups.resolveAssetType(asset.type());
            if (type == null) {
                return "Unknown asset type (Assets row " + asset.excelRow() + ")";
            }
            if (asset.detail() == null || asset.detail().isBlank()) {
                return "Asset detail is required (Assets row " + asset.excelRow() + ")";
            }
            if (BiaLookups.DETAIL_INTEGER.equals(type.detailKind()) && !asset.detail().trim().matches("\\d+")) {
                return "Asset detail must be a whole number for " + type.label()
                        + " (Assets row " + asset.excelRow() + ")";
            }
        }
        List<BiaProcessImportWorkbook.ParsedImpact> impactRows = impactByCode.getOrDefault(codeKey, List.of());
        if (!impactRows.isEmpty()) {
            String impactErr = validateImpactRows(impactRows);
            if (impactErr != null) {
                return impactErr;
            }
        }
        for (BiaProcessImportWorkbook.ParsedMor mor : morByCode.getOrDefault(codeKey, List.of())) {
            String morErr = validateMorRow(mor);
            if (morErr != null) {
                return morErr;
            }
        }
        boolean tpYes = BiaLookups.YES.equalsIgnoreCase(tp == null ? "" : tp.trim());
        if (tpYes) {
            for (BiaProcessImportWorkbook.ParsedContact contact : contactsByCode.getOrDefault(codeKey, List.of())) {
                if (contact.name() == null || contact.name().isBlank()
                        || contact.email() == null || !contact.email().contains("@")) {
                    return "Contact name and email are required (Contacts row " + contact.excelRow() + ")";
                }
            }
        }
        return null;
    }

    private BiaProcessResponse persistImportedProcess(
            User user,
            long cycleId,
            BiaProcessImportWorkbook.ParsedProcess row,
            User owner,
            List<BiaProcessImportWorkbook.ParsedStaff> staffRows,
            List<BiaProcessImportWorkbook.ParsedAsset> assetRows,
            List<BiaProcessImportWorkbook.ParsedImpact> impactRows,
            List<BiaProcessImportWorkbook.ParsedMor> morRows,
            List<BiaProcessImportWorkbook.ParsedContact> contactRows,
            Map<String, BiaPersonRef> personCache) {
        boolean tpYes = BiaLookups.YES.equalsIgnoreCase(
                row.thirdPartyInvolved() == null ? "" : row.thirdPartyInvolved().trim());
        List<BiaPersonRef> identityStaff = new ArrayList<>();
        List<BiaPersonRef> morStaff = new ArrayList<>();
        for (BiaProcessImportWorkbook.ParsedStaff staff : staffRows) {
            BiaPersonRef person = resolvePerson(staff.email(), personCache);
            if (person == null) {
                continue;
            }
            if (isMorRole(staff.role())) {
                morStaff.add(person);
            } else {
                identityStaff.add(person);
            }
        }
        List<BiaAssetDependency> assets = assetRows.stream()
                .map(a -> new BiaAssetDependency(BiaLookups.resolveAssetType(a.type()).code(), a.detail().trim()))
                .toList();
        String activities = tpYes ? blankToNull(row.thirdPartyActivities()) : "None";
        String supplier = tpYes ? blankToNull(row.supplierName()) : "N/A";
        String depTp = tpYes ? blankToNull(row.depThirdParty()) : "None";
        UpsertBiaProcessRequest req = new UpsertBiaProcessRequest(
                row.processCode().trim(),
                row.processName().trim(),
                blankToNull(row.description()),
                blankToNull(row.flowTrigger()),
                blankToNull(row.flowSteps()),
                blankToNull(row.flowOutput()),
                activities,
                supplier,
                null,
                owner.getId(),
                BiaLookups.resolveFrequency(row.frequency()),
                null,
                null,
                blankToNull(row.depPremises()),
                null,
                null,
                blankToNull(row.depRecords()),
                depTp,
                null,
                blankToNull(row.notes()),
                identityStaff,
                assets);
        BiaProcessResponse created = createProcess(user, cycleId, req);
        if (!impactRows.isEmpty()) {
            created = saveImpact(user, created.id(), toImpactRequest(impactRows));
        }
        created = toProcessResponse(requireProcess(created.id()), true);
        if (created.morRequired() && !morRows.isEmpty()) {
            BiaProcessImportWorkbook.ParsedMor mor = morRows.get(0);
            List<BiaThirdPartyContact> contacts = tpYes
                    ? contactRows.stream()
                    .map(c -> new BiaThirdPartyContact(c.name().trim(), c.email().trim(), blankToNull(c.phone())))
                    .toList()
                    : List.of();
            created = saveMor(user, created.id(), new SaveBiaMorRequest(
                    null, null, null, null, null, null, null, null, null, null,
                    morStaff,
                    splitLookup(mor.recoveryStrategies(), BiaLookups.RECOVERY_STRATEGIES),
                    yn(mor.internet()),
                    yn(mor.intranet()),
                    yn(mor.vpn()),
                    siteType(mor.site()),
                    splitNames(mor.softwareApps()),
                    parseLaptops(mor.laptops()),
                    yn(mor.m365()),
                    contacts,
                    null));
        }
        return toProcessResponse(requireProcess(created.id()), true);
    }

    private static String validateImpactRows(List<BiaProcessImportWorkbook.ParsedImpact> rows) {
        Map<BiaImpactEngine.Category, Integer[]> byCat = new LinkedHashMap<>();
        for (BiaProcessImportWorkbook.ParsedImpact row : rows) {
            BiaImpactEngine.Category cat = BiaLookups.resolveCategory(row.category());
            if (cat == null) {
                return "Unknown impact category (Impact row " + row.excelRow() + ")";
            }
            for (Integer rating : row.ratings()) {
                if (rating != null && rating == Integer.MIN_VALUE) {
                    return "Impact rating must be 1-5 (Impact row " + row.excelRow() + ")";
                }
            }
            String mono = BiaProcessImportWorkbook.monotonicError(row.ratings());
            if (mono != null) {
                return mono + " (Impact row " + row.excelRow() + ")";
            }
            if (byCat.put(cat, row.ratings()) != null) {
                return "Impact category " + cat.name() + " is duplicated for this process";
            }
        }
        if (byCat.size() != 4) {
            return "Impact needs four category rows (Strategic, Financial, Operational, Compliance)";
        }
        return null;
    }

    private static String validateMorRow(BiaProcessImportWorkbook.ParsedMor mor) {
        if (splitLookup(mor.recoveryStrategies(), BiaLookups.RECOVERY_STRATEGIES).isEmpty()
                && mor.recoveryStrategies() != null && !mor.recoveryStrategies().isBlank()) {
            return "Unknown recovery strategy (MOR row " + mor.excelRow() + ")";
        }
        if (mor.internet() != null && !mor.internet().isBlank() && ynSafe(mor.internet()) == null) {
            return "Internet must be Yes or No (MOR row " + mor.excelRow() + ")";
        }
        if (mor.intranet() != null && !mor.intranet().isBlank() && ynSafe(mor.intranet()) == null) {
            return "Intranet must be Yes or No (MOR row " + mor.excelRow() + ")";
        }
        if (mor.vpn() != null && !mor.vpn().isBlank() && ynSafe(mor.vpn()) == null) {
            return "VPN must be Yes or No (MOR row " + mor.excelRow() + ")";
        }
        if (mor.m365() != null && !mor.m365().isBlank() && ynSafe(mor.m365()) == null) {
            return "Microsoft 365 must be Yes or No (MOR row " + mor.excelRow() + ")";
        }
        if (mor.site() != null && !mor.site().isBlank()) {
            try {
                siteType(mor.site());
            } catch (ResponseStatusException e) {
                return "Site must be Primary Site, Secondary Site, or Other (MOR row " + mor.excelRow() + ")";
            }
        }
        if (mor.laptops() != null && !mor.laptops().isBlank()) {
            try {
                int n = Integer.parseInt(mor.laptops().trim());
                if (n < 0) {
                    return "Laptops must be 0 or greater (MOR row " + mor.excelRow() + ")";
                }
            } catch (NumberFormatException e) {
                return "Laptops must be a whole number (MOR row " + mor.excelRow() + ")";
            }
        }
        return null;
    }

    private static SaveBiaImpactRequest toImpactRequest(List<BiaProcessImportWorkbook.ParsedImpact> rows) {
        List<BiaImpactCellRequest> cells = new ArrayList<>();
        BiaImpactEngine.Band[] bands = BiaImpactEngine.Band.values();
        for (BiaProcessImportWorkbook.ParsedImpact row : rows) {
            BiaImpactEngine.Category cat = BiaLookups.resolveCategory(row.category());
            for (int i = 0; i < bands.length; i++) {
                Integer rating = row.ratings()[i];
                cells.add(new BiaImpactCellRequest(cat.name(), bands[i].name(), rating, null));
            }
        }
        return new SaveBiaImpactRequest(cells);
    }

    private BiaPersonRef resolvePerson(String email, Map<String, BiaPersonRef> cache) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String key = email.trim().toLowerCase(Locale.ROOT);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        BiaPersonRef found = null;
        try {
            EmployeeDirectorySearchResponse dir = employeeDirectoryService.search(email.trim());
            if (dir.available()) {
                for (EmployeeDirectoryEntry e : dir.results()) {
                    if (e.email() != null && key.equals(e.email().trim().toLowerCase(Locale.ROOT))) {
                        found = new BiaPersonRef("ENTRA", e.directoryId(), null, e.email(), e.displayName(), e.jobTitle());
                        break;
                    }
                }
            }
        } catch (ResponseStatusException ignored) {
            // short query or Graph down: fall through to Arbor users
        }
        if (found == null) {
            User arbor = userRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
            if (arbor != null && arbor.getStatus() != UserStatus.DISABLED) {
                found = new BiaPersonRef("ARBOR", null, arbor.getId(), arbor.getEmail(), arbor.getFullName(), null);
            }
        }
        cache.put(key, found);
        return found;
    }

    private static String staffRoleError(String role) {
        if (role == null || role.isBlank() || "identity".equalsIgnoreCase(role.trim())
                || "mor".equalsIgnoreCase(role.trim())) {
            return null;
        }
        return "Staff Role must be Identity or MOR";
    }

    private static boolean isMorRole(String role) {
        return role != null && "mor".equalsIgnoreCase(role.trim());
    }

    private static String ynSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (BiaLookups.YES.equalsIgnoreCase(t)) {
            return BiaLookups.YES;
        }
        if (BiaLookups.NO.equalsIgnoreCase(t)) {
            return BiaLookups.NO;
        }
        return null;
    }

    private static List<String> splitNames(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[;|]")) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private static List<String> splitLookup(String raw, List<String> allowed) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(";")) {
            if (part == null || part.isBlank()) {
                continue;
            }
            String t = part.trim();
            String match = null;
            for (String a : allowed) {
                if (a.equalsIgnoreCase(t)) {
                    match = a;
                    break;
                }
            }
            if (match == null) {
                return List.of();
            }
            out.add(match);
        }
        return out;
    }

    private static Integer parseLaptops(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Integer.parseInt(raw.trim());
    }

    private static Map<String, List<BiaProcessImportWorkbook.ParsedStaff>> groupStaff(
            List<BiaProcessImportWorkbook.ParsedStaff> rows) {
        Map<String, List<BiaProcessImportWorkbook.ParsedStaff>> map = new LinkedHashMap<>();
        for (BiaProcessImportWorkbook.ParsedStaff row : rows) {
            if (row.processCode() == null || row.processCode().isBlank()) {
                continue;
            }
            map.computeIfAbsent(row.processCode().trim().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private static Map<String, List<BiaProcessImportWorkbook.ParsedAsset>> groupAssets(
            List<BiaProcessImportWorkbook.ParsedAsset> rows) {
        Map<String, List<BiaProcessImportWorkbook.ParsedAsset>> map = new LinkedHashMap<>();
        for (BiaProcessImportWorkbook.ParsedAsset row : rows) {
            if (row.processCode() == null || row.processCode().isBlank()) {
                continue;
            }
            map.computeIfAbsent(row.processCode().trim().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private static Map<String, List<BiaProcessImportWorkbook.ParsedImpact>> groupImpact(
            List<BiaProcessImportWorkbook.ParsedImpact> rows) {
        Map<String, List<BiaProcessImportWorkbook.ParsedImpact>> map = new LinkedHashMap<>();
        for (BiaProcessImportWorkbook.ParsedImpact row : rows) {
            if (row.processCode() == null || row.processCode().isBlank()) {
                continue;
            }
            map.computeIfAbsent(row.processCode().trim().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private static Map<String, List<BiaProcessImportWorkbook.ParsedMor>> groupMor(
            List<BiaProcessImportWorkbook.ParsedMor> rows) {
        Map<String, List<BiaProcessImportWorkbook.ParsedMor>> map = new LinkedHashMap<>();
        for (BiaProcessImportWorkbook.ParsedMor row : rows) {
            if (row.processCode() == null || row.processCode().isBlank()) {
                continue;
            }
            map.computeIfAbsent(row.processCode().trim().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private static Map<String, List<BiaProcessImportWorkbook.ParsedContact>> groupContacts(
            List<BiaProcessImportWorkbook.ParsedContact> rows) {
        Map<String, List<BiaProcessImportWorkbook.ParsedContact>> map = new LinkedHashMap<>();
        for (BiaProcessImportWorkbook.ParsedContact row : rows) {
            if (row.processCode() == null || row.processCode().isBlank()) {
                continue;
            }
            map.computeIfAbsent(row.processCode().trim().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String blankToDefault(String s, String fallback) {
        String v = blankToNull(s);
        return v != null ? v : fallback;
    }
}
