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
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.privacy.domain.DpiaStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyRopa;
import com.a9.arbor.grc.modules.privacy.domain.RopaDpiaStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaIntakeMode;
import com.a9.arbor.grc.modules.privacy.domain.RopaLegalBasis;
import com.a9.arbor.grc.modules.privacy.domain.RopaStatus;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyDpiaRepository;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.AssignRopaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateRopaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.RejectRopaRequest;
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
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PrivacyRopaService {

    private final PrivacyRopaRepository ropaRepository;
    private final PrivacyDpiaRepository dpiaRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final UserRepository userRepository;
    private final WorkTaskRepository workTaskRepository;
    private final EvidenceLinkRepository evidenceLinkRepository;
    private final PrivacyRopaWorkbookTemplate workbookTemplate;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    public PrivacyRopaService(PrivacyRopaRepository ropaRepository,
                              PrivacyDpiaRepository dpiaRepository,
                              OrgUnitRepository orgUnitRepository,
                              UserRepository userRepository,
                              WorkTaskRepository workTaskRepository,
                              EvidenceLinkRepository evidenceLinkRepository,
                              PrivacyRopaWorkbookTemplate workbookTemplate,
                              NotificationService notificationService,
                              AuditTrailService auditTrailService) {
        this.ropaRepository = ropaRepository;
        this.dpiaRepository = dpiaRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.userRepository = userRepository;
        this.workTaskRepository = workTaskRepository;
        this.evidenceLinkRepository = evidenceLinkRepository;
        this.workbookTemplate = workbookTemplate;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<RopaResponse> list() {
        return ropaRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(RopaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RopaResponse get(long id) {
        return RopaResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public ExportFile downloadTemplate() {
        return workbookTemplate.buildTemplate();
    }

    @Transactional
    public RopaResponse create(User user, CreateRopaRequest req) {
        validateOrgUnit(req.orgUnitId());
        RopaIntakeMode mode = req.intakeMode() != null ? req.intakeMode() : RopaIntakeMode.FORM;
        RopaLegalBasis basis = req.legalBasis();
        if (basis == null) {
            if (mode == RopaIntakeMode.WORKBOOK) {
                basis = RopaLegalBasis.CONSENT;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "legalBasis is required for form intake");
            }
        }

        PrivacyRopa row = new PrivacyRopa();
        row.setDisplayId(nextDisplayId());
        row.setActivityName(req.activityName().trim());
        row.setEntityName(blankToNull(req.entityName()));
        row.setSectorName(blankToNull(req.sectorName()));
        row.setDivisionName(blankToNull(req.divisionName()));
        row.setSectionName(blankToNull(req.sectionName()));
        row.setProcessingDescription(blankToNull(req.processingDescription()));
        row.setPurpose(blankToNull(req.purpose()));
        row.setLegalBasis(basis);
        row.setDataCategories(blankToNull(req.dataCategories()));
        row.setSensitiveDataCategories(blankToNull(req.sensitiveDataCategories()));
        row.setDataSubjects(blankToNull(req.dataSubjects()));
        row.setDataSubjectVolume(blankToNull(req.dataSubjectVolume()));
        row.setDataSubjectVolumeNote(blankToNull(req.dataSubjectVolumeNote()));
        row.setProcessingFrequency(blankToNull(req.processingFrequency()));
        row.setProcessingFrequencyNote(blankToNull(req.processingFrequencyNote()));
        row.setSourceOfPersonalData(blankToNull(req.sourceOfPersonalData()));
        row.setMethodOfReceiving(blankToNull(req.methodOfReceiving()));
        row.setRecipients(blankToNull(req.recipients()));
        row.setShareInternal(Boolean.TRUE.equals(req.shareInternal()));
        row.setShareInternalDetails(blankToNull(req.shareInternalDetails()));
        row.setShareOperator(Boolean.TRUE.equals(req.shareOperator()));
        row.setShareOperatorDetails(blankToNull(req.shareOperatorDetails()));
        row.setShareExternal(Boolean.TRUE.equals(req.shareExternal()));
        row.setShareExternalDetails(blankToNull(req.shareExternalDetails()));
        row.setExternalEntityLocation(blankToNull(req.externalEntityLocation()));
        row.setThirdCountryTransfers(Boolean.TRUE.equals(req.thirdCountryTransfers()));
        row.setAdequateProtection(req.adequateProtection());
        row.setCrossBorderTransferBasis(blankToNull(req.crossBorderTransferBasis()));
        row.setSafeguardsImplemented(blankToNull(req.safeguardsImplemented()));
        applyDpiaStatus(row, req.dpiaStatus(), req.dpiaRequired(), req.thirdCountryTransfers());
        row.setThirdCountryTransferDetails(blankToNull(req.thirdCountryTransferDetails()));
        row.setRetentionPeriod(blankToNull(req.retentionPeriod()));
        row.setSecurityMeasures(blankToNull(req.securityMeasures()));
        row.setStorageSystems(blankToNull(req.storageSystems()));
        row.setRepresentativeName(blankToNull(req.representativeName()));
        row.setRepresentativeEmployeeId(blankToNull(req.representativeEmployeeId()));
        row.setComments(blankToNull(req.comments()));
        row.setDesignation(req.designation());
        row.setIntakeMode(mode);
        row.setRopaDepartmentId(req.ropaDepartmentId());
        row.setLegalBasesJson(blankToNull(req.legalBasesJson()));
        row.setAdequacyNote(blankToNull(req.adequacyNote()));
        row.setOrgUnitId(req.orgUnitId());
        row.setStatus(RopaStatus.DRAFT);
        row.setCreatedByUserId(user.getId());
        ropaRepository.save(row);

        audit(user, AuditActionType.CREATED, row, null, null, null);
        return RopaResponse.from(row);
    }

    @Transactional
    public RopaResponse update(User user, long id, UpdateRopaRequest req) {
        PrivacyRopa row = require(id);
        if (row.getStatus() == RopaStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approved ROPA cannot be edited");
        }
        if (row.getStatus() == RopaStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ROPA under review cannot be edited");
        }
        if (row.getStatus() == RopaStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Assigned workbook ROPA cannot be edited until Contributor submits or Admin rejects");
        }
        validateOrgUnit(req.orgUnitId());

        if (req.activityName() != null && !req.activityName().isBlank()) {
            row.setActivityName(req.activityName().trim());
        }
        if (req.entityName() != null) {
            row.setEntityName(blankToNull(req.entityName()));
        }
        if (req.sectorName() != null) {
            row.setSectorName(blankToNull(req.sectorName()));
        }
        if (req.divisionName() != null) {
            row.setDivisionName(blankToNull(req.divisionName()));
        }
        if (req.sectionName() != null) {
            row.setSectionName(blankToNull(req.sectionName()));
        }
        if (req.processingDescription() != null) {
            row.setProcessingDescription(blankToNull(req.processingDescription()));
        }
        if (req.purpose() != null) {
            row.setPurpose(blankToNull(req.purpose()));
        }
        if (req.legalBasis() != null) {
            row.setLegalBasis(req.legalBasis());
        }
        if (req.dataCategories() != null) {
            row.setDataCategories(blankToNull(req.dataCategories()));
        }
        if (req.sensitiveDataCategories() != null) {
            row.setSensitiveDataCategories(blankToNull(req.sensitiveDataCategories()));
        }
        if (req.dataSubjects() != null) {
            row.setDataSubjects(blankToNull(req.dataSubjects()));
        }
        if (req.dataSubjectVolume() != null) {
            row.setDataSubjectVolume(blankToNull(req.dataSubjectVolume()));
        }
        if (req.dataSubjectVolumeNote() != null) {
            row.setDataSubjectVolumeNote(blankToNull(req.dataSubjectVolumeNote()));
        }
        if (req.processingFrequency() != null) {
            row.setProcessingFrequency(blankToNull(req.processingFrequency()));
        }
        if (req.processingFrequencyNote() != null) {
            row.setProcessingFrequencyNote(blankToNull(req.processingFrequencyNote()));
        }
        if (req.sourceOfPersonalData() != null) {
            row.setSourceOfPersonalData(blankToNull(req.sourceOfPersonalData()));
        }
        if (req.methodOfReceiving() != null) {
            row.setMethodOfReceiving(blankToNull(req.methodOfReceiving()));
        }
        if (req.recipients() != null) {
            row.setRecipients(blankToNull(req.recipients()));
        }
        if (req.shareInternal() != null) {
            row.setShareInternal(req.shareInternal());
        }
        if (req.shareInternalDetails() != null) {
            row.setShareInternalDetails(blankToNull(req.shareInternalDetails()));
        }
        if (req.shareOperator() != null) {
            row.setShareOperator(req.shareOperator());
        }
        if (req.shareOperatorDetails() != null) {
            row.setShareOperatorDetails(blankToNull(req.shareOperatorDetails()));
        }
        if (req.shareExternal() != null) {
            row.setShareExternal(req.shareExternal());
        }
        if (req.shareExternalDetails() != null) {
            row.setShareExternalDetails(blankToNull(req.shareExternalDetails()));
        }
        if (req.externalEntityLocation() != null) {
            row.setExternalEntityLocation(blankToNull(req.externalEntityLocation()));
        }
        if (req.thirdCountryTransfers() != null) {
            row.setThirdCountryTransfers(req.thirdCountryTransfers());
        }
        if (req.adequateProtection() != null) {
            row.setAdequateProtection(req.adequateProtection());
        }
        if (req.crossBorderTransferBasis() != null) {
            row.setCrossBorderTransferBasis(blankToNull(req.crossBorderTransferBasis()));
        }
        if (req.safeguardsImplemented() != null) {
            row.setSafeguardsImplemented(blankToNull(req.safeguardsImplemented()));
        }
        if (req.dpiaStatus() != null || req.dpiaRequired() != null || req.thirdCountryTransfers() != null) {
            applyDpiaStatus(row, req.dpiaStatus(), req.dpiaRequired(), req.thirdCountryTransfers());
        }
        if (req.thirdCountryTransferDetails() != null) {
            row.setThirdCountryTransferDetails(blankToNull(req.thirdCountryTransferDetails()));
        }
        if (req.retentionPeriod() != null) {
            row.setRetentionPeriod(blankToNull(req.retentionPeriod()));
        }
        if (req.securityMeasures() != null) {
            row.setSecurityMeasures(blankToNull(req.securityMeasures()));
        }
        if (req.storageSystems() != null) {
            row.setStorageSystems(blankToNull(req.storageSystems()));
        }
        if (req.representativeName() != null) {
            row.setRepresentativeName(blankToNull(req.representativeName()));
        }
        if (req.representativeEmployeeId() != null) {
            row.setRepresentativeEmployeeId(blankToNull(req.representativeEmployeeId()));
        }
        if (req.comments() != null) {
            row.setComments(blankToNull(req.comments()));
        }
        if (req.designation() != null) {
            row.setDesignation(req.designation());
        }
        if (req.orgUnitId() != null) {
            row.setOrgUnitId(req.orgUnitId());
        }
        if (req.ropaDepartmentId() != null) {
            row.setRopaDepartmentId(req.ropaDepartmentId());
        }
        if (req.legalBasesJson() != null) {
            row.setLegalBasesJson(blankToNull(req.legalBasesJson()));
        }
        if (req.adequacyNote() != null) {
            row.setAdequacyNote(blankToNull(req.adequacyNote()));
        }
        if (row.getStatus() == RopaStatus.REJECTED) {
            row.setStatus(RopaStatus.DRAFT);
            row.setReviewNotes(null);
            row.setReviewedByUserId(null);
            row.setReviewedAt(null);
        }
        ropaRepository.save(row);

        audit(user, AuditActionType.UPDATED, row, null, null, null);
        return RopaResponse.from(row);
    }

    @Transactional
    public RopaResponse assign(User admin, long id, AssignRopaRequest req) {
        PrivacyRopa row = require(id);
        if (row.getIntakeMode() != RopaIntakeMode.WORKBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only workbook-intake ROPA can be assigned to a Contributor");
        }
        if (row.getStatus() != RopaStatus.DRAFT && row.getStatus() != RopaStatus.REJECTED
                && row.getStatus() != RopaStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "ROPA must be DRAFT, REJECTED, or ASSIGNED to assign or reassign");
        }
        if (row.getStatus() == RopaStatus.ASSIGNED && row.getWorkTaskId() != null) {
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
                        MyTasksService.MODULE_PRIVACY, MyTasksService.TYPE_PRIVACY_ROPA, row.getId())
                .orElseGet(WorkTask::new);
        task.setSourceModule(MyTasksService.MODULE_PRIVACY);
        task.setSourceRecordType(MyTasksService.TYPE_PRIVACY_ROPA);
        task.setSourceRecordId(row.getId());
        task.setTitle(row.getDisplayId() + ": " + row.getActivityName());
        task.setDetail("RoPA workbook · attach filled sheet + approval evidence");
        task.setAssignedToUserId(assignee.getId());
        task.setAssignedByUserId(admin.getId());
        task.setStatus(WorkTaskStatus.PENDING);
        task.setDueDate(req.dueDate());
        task.setSubmittedAt(null);
        workTaskRepository.save(task);

        row.setAssignedContributorId(assignee.getId());
        row.setWorkTaskId(task.getId());
        row.setAssignedAt(Instant.now());
        row.setStatus(RopaStatus.ASSIGNED);
        row.setReviewNotes(null);
        row.setReviewedByUserId(null);
        row.setReviewedAt(null);
        ropaRepository.save(row);

        notificationService.trigger(
                "ROPA_ASSIGNED",
                String.valueOf(row.getId()),
                List.of(assignee.getId().toString()),
                Map.of(
                        "displayId", row.getDisplayId(),
                        "activityName", row.getActivityName(),
                        "dueDate", req.dueDate() != null ? req.dueDate().toString() : "-"));

        audit(admin, AuditActionType.UPDATED, row, "assignedTo", null, assignee.getEmail());
        return RopaResponse.from(row);
    }

    /**
     * Contributor completes Mode B: requires WORKBOOK_SUBMITTED + APPROVAL_EVIDENCE links.
     */
    @Transactional
    public RopaResponse submitFromContributor(User user, long id) {
        PrivacyRopa row = require(id);
        if (row.getIntakeMode() != RopaIntakeMode.WORKBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a workbook ROPA");
        }
        if (row.getStatus() != RopaStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ROPA is not awaiting Contributor submission");
        }
        if (!Objects.equals(user.getId(), row.getAssignedContributorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this ROPA");
        }
        requireModeBEvidence(row);

        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_PRIVACY, MyTasksService.TYPE_PRIVACY_ROPA, row.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Work task missing"));
        if (!Objects.equals(user.getId(), task.getAssignedToUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this task");
        }
        task.setStatus(WorkTaskStatus.SUBMITTED);
        task.setSubmittedAt(Instant.now());
        workTaskRepository.save(task);

        String previous = row.getStatus().name();
        row.setStatus(RopaStatus.UNDER_REVIEW);
        ropaRepository.save(row);

        if (row.needsApprovedDpia() && !hasApprovedDpia(row.getId())) {
            notificationService.trigger(
                    "DPIA_REQUIRED",
                    String.valueOf(row.getId()),
                    List.of("ADMIN"),
                    Map.of(
                            "displayId", row.getDisplayId(),
                            "activityName", row.getActivityName()));
        }
        audit(user, AuditActionType.UPDATED, row, "status", previous, RopaStatus.UNDER_REVIEW.name());
        return RopaResponse.from(row);
    }

    @Transactional
    public RopaResponse submitForReview(User user, long id) {
        PrivacyRopa row = require(id);
        if (row.getIntakeMode() == RopaIntakeMode.WORKBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Workbook ROPA is submitted by the assigned Contributor via My Tasks");
        }
        if (row.getStatus() != RopaStatus.DRAFT && row.getStatus() != RopaStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only DRAFT or REJECTED ROPA can be submitted for review");
        }
        String previous = row.getStatus().name();
        row.setStatus(RopaStatus.UNDER_REVIEW);
        row.setReviewNotes(null);
        row.setReviewedByUserId(null);
        row.setReviewedAt(null);
        ropaRepository.save(row);
        if (row.needsApprovedDpia() && !hasApprovedDpia(row.getId())) {
            notificationService.trigger(
                    "DPIA_REQUIRED",
                    String.valueOf(row.getId()),
                    List.of("ADMIN"),
                    Map.of(
                            "displayId", row.getDisplayId(),
                            "activityName", row.getActivityName()));
        }
        audit(user, AuditActionType.UPDATED, row, "status", previous, RopaStatus.UNDER_REVIEW.name());
        return RopaResponse.from(row);
    }

    @Transactional
    public RopaResponse approve(User user, long id) {
        PrivacyRopa row = require(id);
        if (row.getStatus() != RopaStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only UNDER_REVIEW ROPA can be approved");
        }
        String previous = row.getStatus().name();
        row.setStatus(RopaStatus.APPROVED);
        row.setReviewedByUserId(user.getId());
        row.setReviewedAt(Instant.now());
        ropaRepository.save(row);
        markTaskDone(row);
        audit(user, AuditActionType.APPROVED, row, "status", previous, RopaStatus.APPROVED.name());
        return RopaResponse.from(row);
    }

    @Transactional
    public RopaResponse reject(User user, long id, RejectRopaRequest req) {
        PrivacyRopa row = require(id);
        if (row.getStatus() != RopaStatus.UNDER_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only UNDER_REVIEW ROPA can be rejected");
        }
        String previous = row.getStatus().name();
        row.setStatus(RopaStatus.REJECTED);
        row.setReviewNotes(blankToNull(req != null ? req.notes() : null));
        row.setReviewedByUserId(user.getId());
        row.setReviewedAt(Instant.now());
        ropaRepository.save(row);
        reopenContributorTask(row);
        audit(user, AuditActionType.REJECTED, row, "status", previous, RopaStatus.REJECTED.name());
        return RopaResponse.from(row);
    }

    private void reopenContributorTask(PrivacyRopa row) {
        if (row.getIntakeMode() != RopaIntakeMode.WORKBOOK || row.getAssignedContributorId() == null) {
            return;
        }
        WorkTask task = workTaskRepository
                .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                        MyTasksService.MODULE_PRIVACY, MyTasksService.TYPE_PRIVACY_ROPA, row.getId())
                .orElse(null);
        if (task == null) {
            return;
        }
        task.setStatus(WorkTaskStatus.PENDING);
        task.setSubmittedAt(null);
        workTaskRepository.save(task);
        row.setStatus(RopaStatus.ASSIGNED);
        ropaRepository.save(row);
    }

    private void markTaskDone(PrivacyRopa row) {
        if (row.getWorkTaskId() == null) {
            return;
        }
        workTaskRepository.findById(row.getWorkTaskId()).ifPresent(task -> {
            if (task.getStatus() != WorkTaskStatus.CANCELLED) {
                task.setStatus(WorkTaskStatus.SUBMITTED);
                if (task.getSubmittedAt() == null) {
                    task.setSubmittedAt(Instant.now());
                }
                workTaskRepository.save(task);
            }
        });
    }

    private void requireModeBEvidence(PrivacyRopa row) {
        List<EvidenceLink> links = evidenceLinkRepository
                .findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(
                        PrivacyEvidenceStages.MODULE,
                        PrivacyEvidenceStages.RECORD_ROPA,
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

    private static String normStage(String stage) {
        return stage == null ? "" : stage.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasApprovedDpia(long ropaId) {
        return dpiaRepository.findFirstByRopaIdAndStatusOrderByUpdatedAtDesc(ropaId, DpiaStatus.APPROVED)
                .isPresent();
    }

    private static void applyDpiaStatus(PrivacyRopa row,
                                        RopaDpiaStatus explicit,
                                        Boolean legacyRequired,
                                        Boolean thirdCountry) {
        boolean third = thirdCountry != null ? thirdCountry : row.isThirdCountryTransfers();
        RopaDpiaStatus status;
        if (explicit != null) {
            status = explicit;
        } else if (legacyRequired != null) {
            status = Boolean.TRUE.equals(legacyRequired) ? RopaDpiaStatus.REQUIRED : RopaDpiaStatus.NOT_REQUIRED;
        } else if (row.getDpiaStatus() != null) {
            status = row.getDpiaStatus();
        } else {
            status = RopaDpiaStatus.NOT_REQUIRED;
        }
        // Cross-border heuristic (F4): third-country with no explicit status ⇒ Required.
        if (third && status == RopaDpiaStatus.NOT_REQUIRED && explicit == null) {
            status = RopaDpiaStatus.REQUIRED;
        }
        row.setDpiaStatus(status);
    }

    private PrivacyRopa require(long id) {
        return ropaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ROPA not found"));
    }

    private void validateOrgUnit(Long orgUnitId) {
        if (orgUnitId != null && orgUnitRepository.findById(orgUnitId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Org unit not found");
        }
    }

    private String nextDisplayId() {
        int year = Year.now().getValue();
        String prefix = "ROPA-" + year + "-";
        long count = ropaRepository.countByDisplayIdStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void audit(User user, AuditActionType action, PrivacyRopa row,
                       String field, String oldVal, String newVal) {
        auditTrailService.record(
                action, "PRIVACY_ROPA", user.getId(), primaryRole(user),
                "PRIVACY_ROPA", String.valueOf(row.getId()), row.getDisplayId(),
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
