package com.a9.arbor.grc.modules.tprm.service;

import com.a9.arbor.grc.common.validation.DateRules;
import com.a9.arbor.grc.common.validation.EmailRules;
import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.config.ArborSecurityProperties;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceLinkRepository;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;
import com.a9.arbor.grc.modules.evidence.service.EvidenceService;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.notification.mail.NotificationMailContent;
import com.a9.arbor.grc.modules.notification.mail.TprmNotificationMailComposer;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.risk.domain.RiskCategory;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.service.RiskService;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse;
import com.a9.arbor.grc.modules.tprm.domain.AssessmentStatus;
import com.a9.arbor.grc.modules.tprm.domain.ContactRole;
import com.a9.arbor.grc.modules.tprm.domain.FindingStatus;
import com.a9.arbor.grc.modules.tprm.domain.TprmAnswer;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessment;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessmentItem;
import com.a9.arbor.grc.modules.tprm.domain.TprmCompliance;
import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.domain.TprmFinding;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendorContact;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendorToken;
import com.a9.arbor.grc.modules.tprm.domain.TprmVerdict;
import com.a9.arbor.grc.modules.tprm.domain.VendorLifecycleState;
import com.a9.arbor.grc.modules.tprm.domain.VendorTokenStatus;
import com.a9.arbor.grc.modules.tprm.pack.TprmControlDisplay;
import com.a9.arbor.grc.modules.tprm.pack.TprmPosture;
import com.a9.arbor.grc.modules.tprm.pack.TprmQuestionnaireScope;
import com.a9.arbor.grc.modules.tprm.domain.TprmQuestionnaire;
import com.a9.arbor.grc.modules.tprm.domain.TprmQuestionnaireItem;
import com.a9.arbor.grc.modules.tprm.repo.TprmAssessmentItemRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmAssessmentRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmControlRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmFindingRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmQuestionnaireItemRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmQuestionnaireRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorTokenRepository;
import com.a9.arbor.grc.modules.tprm.web.dto.AddCollaboratorRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.AssessmentResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.CreateAssessmentRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.CreateTprmProjectRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.UpdateTprmProjectRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.FindingResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.ReviewAssessmentRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.UpdateFindingRequest;
import com.a9.arbor.grc.security.SecureTokenUtils;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class TprmAssessmentService {

    private static final int DEFAULT_EXPIRY_DAYS = 14;
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final TprmAssessmentRepository assessmentRepository;
    private final TprmAssessmentItemRepository itemRepository;
    private final TprmControlRepository controlRepository;
    private final TprmFindingRepository findingRepository;
    private final TprmQuestionnaireRepository questionnaireRepository;
    private final TprmQuestionnaireItemRepository questionnaireItemRepository;
    private final TprmVendorTokenRepository tokenRepository;
    private final TprmVendorService vendorService;
    private final TprmControlLibrarySeeder seeder;
    private final TprmQuestionnaireService questionnaireService;
    private final TprmQuestionnaireExcel questionnaireExcel;
    private final NotificationService notificationService;
    private final TprmNotificationMailComposer tprmMailComposer;
    private final AuditTrailService auditTrailService;
    private final ArborSecurityProperties securityProperties;
    private final ArborNotificationProperties notificationProperties;
    private final RiskService riskService;
    private final EvidenceService evidenceService;
    private final EvidenceLinkRepository evidenceLinkRepository;

    public static final String EVIDENCE_MODULE = "TPRM";
    public static final String EVIDENCE_RECORD_TYPE = "ASSESSMENT_ITEM";
    public static final int MAX_EVIDENCE_FILES_PER_ITEM = 5;

    public TprmAssessmentService(TprmAssessmentRepository assessmentRepository,
                                 TprmAssessmentItemRepository itemRepository,
                                 TprmControlRepository controlRepository,
                                 TprmFindingRepository findingRepository,
                                 TprmQuestionnaireRepository questionnaireRepository,
                                 TprmQuestionnaireItemRepository questionnaireItemRepository,
                                 TprmVendorTokenRepository tokenRepository,
                                 TprmVendorService vendorService,
                                 TprmControlLibrarySeeder seeder,
                                 TprmQuestionnaireService questionnaireService,
                                 TprmQuestionnaireExcel questionnaireExcel,
                                 NotificationService notificationService,
                                 TprmNotificationMailComposer tprmMailComposer,
                                 AuditTrailService auditTrailService,
                                 ArborSecurityProperties securityProperties,
                                 ArborNotificationProperties notificationProperties,
                                 RiskService riskService,
                                 EvidenceService evidenceService,
                                 EvidenceLinkRepository evidenceLinkRepository) {
        this.assessmentRepository = assessmentRepository;
        this.itemRepository = itemRepository;
        this.controlRepository = controlRepository;
        this.findingRepository = findingRepository;
        this.questionnaireRepository = questionnaireRepository;
        this.questionnaireItemRepository = questionnaireItemRepository;
        this.tokenRepository = tokenRepository;
        this.vendorService = vendorService;
        this.seeder = seeder;
        this.questionnaireService = questionnaireService;
        this.questionnaireExcel = questionnaireExcel;
        this.notificationService = notificationService;
        this.tprmMailComposer = tprmMailComposer;
        this.auditTrailService = auditTrailService;
        this.securityProperties = securityProperties;
        this.notificationProperties = notificationProperties;
        this.riskService = riskService;
        this.evidenceService = evidenceService;
        this.evidenceLinkRepository = evidenceLinkRepository;
    }

    @Transactional
    public AssessmentResponse generate(User admin, long vendorId) {
        vendorService.requireAdmin(admin);
        seeder.seedIfEmpty();
        TprmVendor vendor = vendorService.requireVendor(vendorId);
        if (!vendorService.profileReady(vendor) || vendor.getTier() == null || vendor.getVendorType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Complete the vendor profile and categorisation before generating a questionnaire");
        }
        TprmAssessment draft = currentDraft(vendor);
        if (draft != null && FREEZE.contains(draft.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A questionnaire is already issued; complete it before generating another");
        }
        if (draft == null || draft.getStatus() != AssessmentStatus.DRAFT) {
            draft = new TprmAssessment();
            draft.setVendorId(vendor.getId());
            draft.setDisplayId(nextDisplayId());
            draft.setCreatedByUserId(admin.getId());
            draft.setTitle(vendor.getName() + " questionnaire");
        }
        snapshotFromVendor(draft, vendor);
        draft.setStatus(AssessmentStatus.DRAFT);
        draft = assessmentRepository.save(draft);

        List<TprmAssessmentItem> existing = itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(draft.getId());
        itemRepository.deleteAll(existing);
        itemRepository.flush();

        List<TprmControl> scoped = TprmQuestionnaireScope.scoped(
                controlRepository.findAllByOrderBySortOrderAsc(), vendor.getVendorType(), vendor.getTier());
        int order = 1;
        for (TprmControl control : scoped) {
            TprmAssessmentItem item = new TprmAssessmentItem();
            item.setAssessmentId(draft.getId());
            item.setControlId(control.getId());
            item.setSortOrder(order++);
            item.setEvidenceMandatory(control.isMandatory());
            itemRepository.save(item);
        }
        vendor.setCurrentAssessmentId(draft.getId());
        vendor.setLifecycleState(VendorLifecycleState.QUESTIONNAIRE_READY);
        vendorService.persist(vendor);

        auditTrailService.record(AuditActionType.CREATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_ASSESSMENT", String.valueOf(draft.getId()),
                draft.getDisplayId(), null, null, "generated " + scoped.size() + " controls", null, null);
        return toResponse(draft, vendor.getName(), null, null);
    }

    @Transactional
    public AssessmentResponse createProject(User admin, CreateTprmProjectRequest req) {
        vendorService.requireAdmin(admin);
        seeder.seedIfEmpty();
        String name = req.name().trim();
        if (assessmentRepository.existsByTitleIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A project named \"" + name + "\" already exists");
        }
        TprmVendor vendor = vendorService.requireVendor(req.vendorId());
        TprmQuestionnaire questionnaire = questionnaireRepository.findById(req.questionnaireId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questionnaire not found"));
        List<TprmQuestionnaireItem> qItems =
                questionnaireItemRepository.findByQuestionnaireIdOrderBySortOrderAscIdAsc(questionnaire.getId());
        if (qItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questionnaire has no controls");
        }
        TprmAssessment a = new TprmAssessment();
        a.setVendorId(vendor.getId());
        a.setQuestionnaireId(questionnaire.getId());
        a.setDisplayId(nextDisplayId());
        a.setTitle(name);
        String description = req.description() == null ? null : req.description().trim();
        a.setDescription(description == null || description.isEmpty() ? null : description);
        a.setCreatedByUserId(admin.getId());
        a.setStatus(AssessmentStatus.DRAFT);
        snapshotFromVendor(a, vendor);
        a = assessmentRepository.save(a);
        copyItemsFromQuestionnaire(a, qItems);
        vendor.setCurrentAssessmentId(a.getId());
        vendor.setLifecycleState(VendorLifecycleState.QUESTIONNAIRE_READY);
        vendorService.persist(vendor);
        auditTrailService.record(AuditActionType.CREATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_ASSESSMENT", String.valueOf(a.getId()),
                a.getDisplayId(), null, null, name, null, null);
        return toResponse(a, vendor.getName(), null, null);
    }

    @Transactional
    public AssessmentResponse updateProject(User admin, long assessmentId, UpdateTprmProjectRequest req) {
        vendorService.requireAdmin(admin);
        TprmAssessment a = requireAssessment(assessmentId);
        if (a.getStatus() != AssessmentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vendor and questionnaire can only be changed before Assign");
        }
        TprmVendor newVendor = vendorService.requireVendor(req.vendorId());
        TprmQuestionnaire questionnaire = questionnaireRepository.findById(req.questionnaireId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questionnaire not found"));
        List<TprmQuestionnaireItem> qItems =
                questionnaireItemRepository.findByQuestionnaireIdOrderBySortOrderAscIdAsc(questionnaire.getId());
        if (qItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questionnaire has no controls");
        }
        TprmVendor oldVendor = vendorService.requireVendor(a.getVendorId());
        boolean vendorChanged = !oldVendor.getId().equals(newVendor.getId());
        boolean questionnaireChanged = !Objects.equals(a.getQuestionnaireId(), questionnaire.getId());
        if (vendorChanged) {
            releaseDraftLink(oldVendor, a.getId());
        }
        a.setVendorId(newVendor.getId());
        a.setQuestionnaireId(questionnaire.getId());
        snapshotFromVendor(a, newVendor);
        if (questionnaireChanged) {
            List<TprmAssessmentItem> existing = itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(a.getId());
            itemRepository.deleteAll(existing);
            itemRepository.flush();
            copyItemsFromQuestionnaire(a, qItems);
        }
        a = assessmentRepository.save(a);
        newVendor.setCurrentAssessmentId(a.getId());
        newVendor.setLifecycleState(VendorLifecycleState.QUESTIONNAIRE_READY);
        vendorService.persist(newVendor);
        auditTrailService.record(AuditActionType.UPDATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_ASSESSMENT", String.valueOf(a.getId()),
                a.getDisplayId(), null, null, "draft vendor or questionnaire", null, null);
        return toResponse(a, newVendor.getName(), null, null);
    }

    @Transactional
    public AssessmentResponse issueProject(User admin, long assessmentId, CreateAssessmentRequest req) {
        vendorService.requireAdmin(admin);
        TprmAssessment assessment = requireAssessment(assessmentId);
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project is already assigned");
        }
        TprmVendor vendor = applyIssueVendor(assessment, req.vendorId());
        if (itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(assessment.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questionnaire has no controls");
        }
        if (!vendorService.profileReady(vendor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Complete the vendor profile before assigning the questionnaire");
        }
        snapshotFromVendor(assessment, vendor);
        DateRules.requireActionDueDateIfPresent(req.dueDate(), "Due date");
        assessment.setDueDate(req.dueDate());
        int expiryDays = req.expiryDays() != null && req.expiryDays() > 0 ? req.expiryDays() : DEFAULT_EXPIRY_DAYS;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiryDays, ChronoUnit.DAYS);
        assessment.setExpiresAt(expiresAt);
        assessment.setSentAt(now);
        assessment.setStatus(AssessmentStatus.ISSUED);
        assessment = assessmentRepository.save(assessment);

        List<String> emails = inviteEmails(vendor, req);
        IssuedSecrets secrets = issueTokens(admin, assessment, emails, expiresAt);
        for (String email : emails) {
            sendAssignMail(email, vendor, assessment, secrets.linkFor(email), expiresAt);
        }

        vendor.setCurrentAssessmentId(assessment.getId());
        vendor.setLifecycleState(VendorLifecycleState.ISSUED);
        vendorService.persist(vendor);

        auditTrailService.record(AuditActionType.CREATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_ASSESSMENT", String.valueOf(assessment.getId()),
                assessment.getDisplayId(), null, null, emails.get(0), null, null);
        return toResponse(assessment, vendor.getName(), localPreviewToken(secrets), null);
    }

    @Transactional
    public AssessmentResponse addCollaborator(User admin, long assessmentId, AddCollaboratorRequest req) {
        vendorService.requireAdmin(admin);
        TprmAssessment assessment = requireAssessment(assessmentId);
        if (assessment.getStatus() != AssessmentStatus.ISSUED
                && assessment.getStatus() != AssessmentStatus.IN_PROGRESS
                && assessment.getStatus() != AssessmentStatus.RETURNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Add a collaborator after the questionnaire is assigned");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        EmailRules.requireValid(email, "Collaborator email");
        Instant expiresAt = assessment.getExpiresAt() != null
                ? assessment.getExpiresAt()
                : Instant.now().plus(DEFAULT_EXPIRY_DAYS, ChronoUnit.DAYS);
        IssuedSecrets secrets = issueTokens(admin, assessment, List.of(email), expiresAt);
        TprmVendor vendor = vendorService.requireVendor(assessment.getVendorId());
        sendAssignMail(email, vendor, assessment, secrets.linkFor(email), expiresAt);
        return toResponse(assessment, vendor.getName(), localPreviewToken(secrets), null);
    }

    @Transactional
    public AssessmentResponse issue(User admin, long vendorId, CreateAssessmentRequest req) {
        vendorService.requireAdmin(admin);
        TprmVendor vendor = vendorService.requireVendor(vendorId);
        TprmAssessment assessment = currentDraft(vendor);
        if (assessment == null || assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Generate a questionnaire before issuing it");
        }
        if (itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(assessment.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questionnaire has no controls");
        }
        snapshotFromVendor(assessment, vendor);
        DateRules.requireActionDueDateIfPresent(req.dueDate(), "Due date");
        assessment.setDueDate(req.dueDate());
        int expiryDays = req.expiryDays() != null && req.expiryDays() > 0 ? req.expiryDays() : DEFAULT_EXPIRY_DAYS;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiryDays, ChronoUnit.DAYS);
        assessment.setExpiresAt(expiresAt);
        assessment.setSentAt(now);
        assessment.setStatus(AssessmentStatus.ISSUED);
        if (req.title() != null && !req.title().isBlank()) {
            assessment.setTitle(req.title().trim());
        }
        assessment = assessmentRepository.save(assessment);

        List<String> emails = inviteEmails(vendor, req);
        IssuedSecrets secrets = issueTokens(admin, assessment, emails, expiresAt);
        for (String email : emails) {
            sendAssignMail(email, vendor, assessment, secrets.linkFor(email), expiresAt);
        }

        vendor.setCurrentAssessmentId(assessment.getId());
        vendor.setLifecycleState(VendorLifecycleState.ISSUED);
        vendorService.persist(vendor);

        auditTrailService.record(AuditActionType.CREATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_ASSESSMENT", String.valueOf(assessment.getId()),
                assessment.getDisplayId(), null, null, emails.get(0), null, null);
        return toResponse(assessment, vendor.getName(), localPreviewToken(secrets), null);
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list(Long vendorId) {
        seeder.seedIfEmpty();
        List<TprmAssessment> rows = vendorId != null
                ? assessmentRepository.findByVendorIdOrderByCreatedAtDesc(vendorId)
                : assessmentRepository.findAllByOrderByCreatedAtDesc();
        return rows.stream().map(a -> toResponse(a, vendorService.requireVendor(a.getVendorId()).getName(), null, null)).toList();
    }

    @Transactional(readOnly = true)
    public AssessmentResponse get(long id) {
        TprmAssessment a = requireAssessment(id);
        return toResponse(a, vendorService.requireVendor(a.getVendorId()).getName(), null, null);
    }

    @Transactional(readOnly = true)
    public ExportFile downloadExcel(long assessmentId) {
        TprmAssessment a = requireAssessment(assessmentId);
        byte[] bytes = questionnaireExcel.write(excelRows(a.getId()));
        return new ExportFile(bytes, a.getDisplayId() + "-questionnaire.xlsx", XLSX);
    }

    @Transactional
    public AssessmentResponse uploadExcel(User admin, long assessmentId, MultipartFile file) {
        vendorService.requireAdmin(admin);
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Questionnaire answers are submitted by the vendor SPOC in the portal, not by admin import");
    }

    @Transactional
    public AssessmentResponse review(User admin, long id, ReviewAssessmentRequest req) {
        vendorService.requireAdmin(admin);
        TprmAssessment a = requireAssessment(id);
        if (a.getStatus() != AssessmentStatus.SUBMITTED && a.getStatus() != AssessmentStatus.IN_REVIEW
                && a.getStatus() != AssessmentStatus.COMPLETE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assessment must be submitted before review");
        }
        if (req.complete() && req.returnToVendor()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose either Return to vendor or Close project, not both");
        }
        Map<Long, TprmAssessmentItem> items = itemsById(a.getId());
        for (ReviewAssessmentRequest.ReviewItem line : req.items()) {
            TprmAssessmentItem item = items.get(line.itemId());
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + line.itemId());
            }
            if (line.verdict() == TprmVerdict.DOWNGRADED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Downgraded is no longer a review verdict");
            }
            if (line.verdict() == TprmVerdict.ACCEPTED_WITH_OBSERVATION
                    && (line.comment() == null || line.comment().isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Review comment is required for Accepted with observation");
            }
            item.setReviewVerdict(line.verdict());
            item.setReviewComment(blankToNull(line.comment()));
            item.setReturned(line.verdict() == TprmVerdict.RETURNED);
            if (line.verdict() != TprmVerdict.RETURNED) {
                item.setReturnFingerprint(null);
            }
            itemRepository.save(item);
        }
        a.setStatus(AssessmentStatus.IN_REVIEW);
        a.setReviewedByUserId(admin.getId());
        assessmentRepository.save(a);
        TprmVendor vendor = vendorService.requireVendor(a.getVendorId());
        vendor.setLifecycleState(VendorLifecycleState.IN_REVIEW);
        vendorService.persist(vendor);

        if (req.returnToVendor()) {
            boolean hasReturned = itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(a.getId()).stream()
                    .anyMatch(item -> item.getReviewVerdict() == TprmVerdict.RETURNED);
            if (!hasReturned) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mark at least one control as Returned before returning to the vendor");
            }
            return returnToVendor(admin, a, vendor);
        }
        if (req.complete()) {
            return completeReview(admin, a, vendor);
        }
        return toResponse(a, vendor.getName(), null, null);
    }

    @Transactional
    public RiskRecordResponse raiseErm(User admin, long id) {
        vendorService.requireAdmin(admin);
        TprmAssessment a = requireAssessment(id);
        if (a.getStatus() != AssessmentStatus.COMPLETE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review must be complete first");
        }
        if (!Boolean.FALSE.equals(a.getCompliant()) && !a.isRaiseErmPrompted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ERM risk can only be raised when the vendor is not compliant");
        }
        if (a.getLinkedErmRiskId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ERM risk already raised for this assessment");
        }
        TprmVendor vendor = vendorService.requireVendor(a.getVendorId());
        String posture = a.getPosturePercent() == null ? "-"
                : a.getPosturePercent().multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "%";
        CreateRiskRequest riskRequest = new CreateRiskRequest(
                RiskType.ERM,
                null,
                "Third-party not compliant: " + vendor.getName(),
                "Auto-raised from TPRM assessment " + a.getDisplayId()
                        + ". Pack compliance failed (posture " + posture
                        + ", target " + a.getSnapshotTarget() + ").",
                RiskCategory.OPERATIONAL,
                null,
                null, null, null, null, null, null,
                null,
                "Vendor did not meet the TPRM posture target for " + a.getSnapshotTier(),
                null,
                RiskLevel.HIGH,
                RiskLevel.HIGH,
                null,
                RiskLevel.MEDIUM,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Restore " + vendor.getName() + " to the pack posture target for " + a.getSnapshotTier(),
                null,
                vendor.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Supplier Failure",
                null,
                null,
                null,
                null,
                null,
                null,
                null, null, null, null);
        RiskRecordResponse risk = riskService.create(admin, riskRequest);
        a.setLinkedErmRiskId(risk.id());
        assessmentRepository.save(a);
        vendor.setLinkedErmRiskId(risk.id());
        vendorService.persist(vendor);
        auditTrailService.record(AuditActionType.CREATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_ASSESSMENT", String.valueOf(a.getId()),
                "raised ERM risk " + risk.displayId(), null, null, null, null, null);
        return risk;
    }

    @Transactional(readOnly = true)
    public List<FindingResponse> listFindings(Long vendorId) {
        List<TprmFinding> rows = vendorId != null
                ? findingRepository.findByVendorIdOrderByCreatedAtDesc(vendorId)
                : findingRepository.findAllByOrderByCreatedAtDesc();
        return rows.stream().map(FindingResponse::from).toList();
    }

    @Transactional
    public FindingResponse updateFinding(User admin, long id, UpdateFindingRequest req) {
        vendorService.requireAdmin(admin);
        TprmFinding f = findingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Finding not found"));
        f.setStatus(req.status());
        f.setClosedAt(req.status() == FindingStatus.CLOSED ? Instant.now() : null);
        findingRepository.save(f);
        return FindingResponse.from(f);
    }

    String vendorNameFor(long vendorId) {
        return vendorService.requireVendor(vendorId).getName();
    }

    TprmAssessment requireAssessment(long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
    }

    AssessmentResponse toResponse(TprmAssessment a, String vendorName, String accessToken, String otpCode) {
        return toResponse(a, vendorName, accessToken, otpCode, null);
    }

    AssessmentResponse toResponse(TprmAssessment a, String vendorName, String accessToken, String otpCode,
                                  Boolean canSubmit) {
        List<AssessmentResponse.PortalInviteView> invites = portalInvitesFor(a.getId());
        String token = accessToken;
        String otp = otpCode;
        if ((token == null || token.isBlank()) && !invites.isEmpty()) {
            token = invites.getFirst().accessToken();
            otp = invites.getFirst().otpCode();
        }
        return AssessmentResponse.from(a, vendorName, questionnaireName(a), buildItemViews(a.getId()),
                token, otp, canSubmit, invites);
    }

    private String questionnaireName(TprmAssessment a) {
        if (a.getQuestionnaireId() == null) {
            return a.getTitle();
        }
        return questionnaireRepository.findById(a.getQuestionnaireId()).map(TprmQuestionnaire::getName).orElse(a.getTitle());
    }

    /** While email is off, Admin can reopen portal links from stored token previews. */
    private List<AssessmentResponse.PortalInviteView> portalInvitesFor(long assessmentId) {
        if (notificationProperties.emailLive()) {
            return List.of();
        }
        List<AssessmentResponse.PortalInviteView> out = new ArrayList<>();
        for (TprmVendorToken row : tokenRepository.findByAssessmentId(assessmentId)) {
            if (row.getTokenPreview() == null || row.getTokenPreview().isBlank()) {
                continue;
            }
            if (row.getStatus() == VendorTokenStatus.REVOKED) {
                continue;
            }
            out.add(new AssessmentResponse.PortalInviteView(row.getInviteeEmail(), row.getTokenPreview(), null));
        }
        return out;
    }

    /** UAT with live email must not expose the portal token in Admin JSON. */
    private String localPreviewToken(IssuedSecrets secrets) {
        return notificationProperties.emailLive() ? null : secrets.primaryToken();
    }

    void mergeExcel(TprmAssessment assessment, byte[] bytes) {
        Map<String, TprmAssessmentItem> byCode = new HashMap<>();
        Map<Long, TprmControl> controls = controlsById();
        for (TprmAssessmentItem item : itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(assessment.getId())) {
            TprmControl c = controls.get(item.getControlId());
            if (c != null) {
                byCode.put(TprmControlDisplay.code(item, c).toLowerCase(Locale.ROOT), item);
                byCode.put(c.getControlCode().toLowerCase(Locale.ROOT), item);
            }
        }
        for (TprmQuestionnaireExcel.ParsedRow row : questionnaireExcel.parse(bytes)) {
            TprmAssessmentItem item = byCode.get(row.controlCode().toLowerCase(Locale.ROOT));
            if (item == null) {
                continue;
            }
            if (assessment.getStatus() == AssessmentStatus.RETURNED && !item.isReturned()) {
                continue;
            }
            if (row.answer() != null) {
                item.setVendorAnswer(row.answer());
            }
            if (row.justification() != null) {
                item.setJustification(row.justification());
            }
            if (row.evidenceRef() != null) {
                item.setEvidenceRef(row.evidenceRef());
            }
            itemRepository.save(item);
        }
    }

    List<TprmQuestionnaireExcel.RowView> excelRows(long assessmentId) {
        Map<Long, TprmControl> controls = controlsById();
        List<TprmQuestionnaireExcel.RowView> rows = new ArrayList<>();
        for (TprmAssessmentItem item : itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(assessmentId)) {
            TprmControl c = controls.get(item.getControlId());
            if (c != null) {
                rows.add(new TprmQuestionnaireExcel.RowView(c, item, evidenceMandatory(item, c)));
            }
        }
        return rows;
    }

    private AssessmentResponse returnToVendor(User admin, TprmAssessment a, TprmVendor vendor) {
        for (TprmAssessmentItem item : itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(a.getId())) {
            if (item.getReviewVerdict() == TprmVerdict.RETURNED) {
                item.setReturned(true);
                item.setReturnFingerprint(AssessmentResponse.AssessmentItemView.answerFingerprint(
                        item, evidenceFilesForItem(item)));
            } else {
                item.setReturned(false);
                item.setReturnFingerprint(null);
            }
            itemRepository.save(item);
        }
        a.setStatus(AssessmentStatus.RETURNED);
        assessmentRepository.save(a);
        vendor.setLifecycleState(VendorLifecycleState.RETURNED);
        vendorService.persist(vendor);
        Instant expiresAt = a.getExpiresAt() != null ? a.getExpiresAt() : Instant.now().plus(14, ChronoUnit.DAYS);
        List<String> emails = inviteEmails(vendor, new CreateAssessmentRequest(null, null, null, null, null, null));
        IssuedSecrets secrets = refreshTokensForReturn(admin, a, emails, expiresAt);
        for (String email : emails) {
            sendReturnMail(email, vendor, a, secrets.linkFor(email), expiresAt);
        }
        auditTrailService.record(AuditActionType.UPDATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_ASSESSMENT", String.valueOf(a.getId()),
                a.getDisplayId(), null, null, "returned questions", null, null);
        return toResponse(a, vendor.getName(), localPreviewToken(secrets), null);
    }

    private AssessmentResponse completeReview(User admin, TprmAssessment a, TprmVendor vendor) {
        List<TprmAssessmentItem> items = itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(a.getId());
        Map<Long, TprmControl> controls = controlsById();
        List<TprmPosture.Item> postureItems = new ArrayList<>();
        for (TprmAssessmentItem item : items) {
            if (item.getReviewVerdict() == null || item.getReviewVerdict() == TprmVerdict.RETURNED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Every control needs an Accepted or Accepted with observation verdict");
            }
            TprmControl c = controls.get(item.getControlId());
            if (c == null) {
                continue;
            }
            postureItems.add(new TprmPosture.Item(c.getWeight(), c.isMandatory(), item.getVendorAnswer(), item.getReviewVerdict()));
        }
        double target = a.getSnapshotTarget() != null ? a.getSnapshotTarget().doubleValue() : 0.6;
        TprmPosture.Result result = TprmPosture.evaluate(postureItems, target);
        boolean compliant = result.compliant();
        a.setPosturePercent(BigDecimal.valueOf(result.percent()).setScale(3, RoundingMode.HALF_UP));
        a.setCompliant(compliant);
        a.setRaiseErmPrompted(!compliant);
        a.setStatus(AssessmentStatus.COMPLETE);
        a.setReviewedAt(Instant.now());
        a.setReviewedByUserId(admin.getId());
        assessmentRepository.save(a);

        vendor.setPosturePercent(a.getPosturePercent());
        vendor.setComplianceStatus(compliant ? TprmCompliance.COMPLIANT : TprmCompliance.NOT_COMPLIANT);
        vendor.setLifecycleState(VendorLifecycleState.COMPLETE);
        int months = vendor.getReassessmentMonths() != null ? vendor.getReassessmentMonths() : 12;
        vendor.setNextReviewDate(LocalDate.now().plusMonths(months));
        vendor.setReviewDueNotifiedAt(null);
        vendor.setReviewOverdueNotifiedAt(null);
        vendorService.persist(vendor);

        openFindings(a, vendor, items, controls);

        Map<String, String> data = new HashMap<>();
        data.put("vendorName", vendor.getName());
        data.put("displayId", a.getDisplayId());
        data.put("assessmentTitle", a.getTitle());
        data.put("posturePercent", a.getPosturePercent().multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP).toPlainString());
        data.put("complianceStatus", vendor.getComplianceStatus().name());
        notificationService.trigger("VENDOR_REVIEW_COMPLETE", String.valueOf(a.getId()),
                List.of("OWNER", "ADMIN"), data);
        for (TprmVendorToken row : tokenRepository.findByAssessmentId(a.getId())) {
            if (row.getInviteeEmail() != null && !row.getInviteeEmail().isBlank()) {
                sendClosedMail(row.getInviteeEmail(), vendor, a, compliant);
            }
        }
        if (!compliant) {
            notificationService.trigger("VENDOR_NOT_COMPLIANT", String.valueOf(a.getId()),
                    List.of("OWNER", "ADMIN"), data);
        }
        auditTrailService.record(AuditActionType.UPDATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_ASSESSMENT", String.valueOf(a.getId()),
                a.getDisplayId(), null, null, "review complete compliant=" + compliant, null, null);
        return toResponse(a, vendor.getName(), null, null);
    }

    private void openFindings(TprmAssessment a, TprmVendor vendor, List<TprmAssessmentItem> items,
                              Map<Long, TprmControl> controls) {
        for (TprmAssessmentItem item : items) {
            TprmControl c = controls.get(item.getControlId());
            if (c == null) {
                continue;
            }
            boolean failed = item.getVendorAnswer() == TprmAnswer.NO
                    || item.getReviewVerdict() == TprmVerdict.DOWNGRADED;
            if (!failed) {
                continue;
            }
            TprmFinding f = new TprmFinding();
            f.setVendorId(vendor.getId());
            f.setAssessmentId(a.getId());
            f.setItemId(item.getId());
            f.setControlCode(TprmControlDisplay.code(item, c));
            f.setDomain(TprmControlDisplay.domain(item, c));
            String req = TprmControlDisplay.requirement(item, c);
            f.setTitle(TprmControlDisplay.code(item, c) + " - " + req);
            f.setOwnerUserId(a.getCreatedByUserId());
            f.setDueDate(vendor.getNextReviewDate());
            f.setStatus(FindingStatus.OPEN);
            findingRepository.save(f);
        }
    }

    private List<AssessmentResponse.AssessmentItemView> buildItemViews(long assessmentId) {
        Map<Long, TprmControl> controls = controlsById();
        List<AssessmentResponse.AssessmentItemView> views = new ArrayList<>();
        for (TprmAssessmentItem item : itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(assessmentId)) {
            TprmControl c = controls.get(item.getControlId());
            if (c == null) {
                continue;
            }
            List<AssessmentResponse.AssessmentItemView.EvidenceFileView> files = evidenceFilesForItem(item);
            String primaryName = evidenceService.filenameForEvidence(item.getEvidenceId());
            views.add(AssessmentResponse.AssessmentItemView.from(item, c, primaryName, files));
        }
        return views;
    }

    List<AssessmentResponse.AssessmentItemView.EvidenceFileView> evidenceFilesForItem(TprmAssessmentItem item) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (EvidenceLink link : evidenceLinkRepository
                .findByModuleCodeAndRecordTypeAndRecordIdOrderByLinkedAtDesc(
                        EVIDENCE_MODULE, EVIDENCE_RECORD_TYPE, String.valueOf(item.getId()))) {
            if (link.getEvidenceId() != null) {
                ids.add(link.getEvidenceId());
            }
        }
        // Legacy single-column fallback when links were never written.
        if (ids.isEmpty() && item.getEvidenceId() != null) {
            ids.add(item.getEvidenceId());
        }
        List<AssessmentResponse.AssessmentItemView.EvidenceFileView> files = new ArrayList<>();
        for (Long evidenceId : ids) {
            String name = evidenceService.filenameForEvidence(evidenceId);
            if (name == null || name.isBlank()) {
                name = "evidence-" + evidenceId;
            }
            files.add(new AssessmentResponse.AssessmentItemView.EvidenceFileView(evidenceId, name));
        }
        return files;
    }

    int evidenceFileCount(TprmAssessmentItem item) {
        return evidenceFilesForItem(item).size();
    }

    boolean itemHasEvidenceFile(TprmAssessmentItem item) {
        return !evidenceFilesForItem(item).isEmpty();
    }

    boolean evidenceMandatory(TprmAssessmentItem item, TprmControl control) {
        if (item.getEvidenceMandatory() != null) {
            return item.getEvidenceMandatory();
        }
        return control != null && control.isMandatory();
    }

    private Map<Long, TprmControl> controlsById() {
        Map<Long, TprmControl> map = new HashMap<>();
        for (TprmControl c : controlRepository.findAll()) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private Map<Long, TprmAssessmentItem> itemsById(long assessmentId) {
        Map<Long, TprmAssessmentItem> map = new HashMap<>();
        for (TprmAssessmentItem item : itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(assessmentId)) {
            map.put(item.getId(), item);
        }
        return map;
    }

    /**
     * When Risk profile / vendor type / assurance changes while a DRAFT questionnaire exists,
     * rebuild scope and snapshot so Build stays aligned with Categorise (no silent desync).
     * No-op when there is no DRAFT, or snapshot already matches the vendor.
     */
    @Transactional
    public void resyncDraftQuestionnaire(long vendorId) {
        TprmVendor vendor = vendorService.requireVendor(vendorId);
        TprmAssessment draft = currentDraft(vendor);
        if (draft == null || draft.getStatus() != AssessmentStatus.DRAFT) {
            return;
        }
        if (!draftNeedsResync(draft, vendor)) {
            return;
        }
        if (vendor.getTier() == null || vendor.getVendorType() == null) {
            return;
        }
        seeder.seedIfEmpty();
        snapshotFromVendor(draft, vendor);
        assessmentRepository.save(draft);

        List<TprmAssessmentItem> existing = itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(draft.getId());
        itemRepository.deleteAll(existing);
        itemRepository.flush();

        List<TprmControl> scoped = TprmQuestionnaireScope.scoped(
                controlRepository.findAllByOrderBySortOrderAsc(), vendor.getVendorType(), vendor.getTier());
        int order = 1;
        for (TprmControl control : scoped) {
            TprmAssessmentItem item = new TprmAssessmentItem();
            item.setAssessmentId(draft.getId());
            item.setControlId(control.getId());
            item.setSortOrder(order++);
            item.setEvidenceMandatory(control.isMandatory());
            itemRepository.save(item);
        }
        vendor.setLifecycleState(VendorLifecycleState.QUESTIONNAIRE_READY);
        vendorService.persist(vendor);
    }

    private static boolean draftNeedsResync(TprmAssessment draft, TprmVendor vendor) {
        String type = vendor.getVendorType() != null ? vendor.getVendorType().name() : null;
        String criticality = vendor.getCriticality() != null ? vendor.getCriticality().name() : null;
        return !Objects.equals(draft.getSnapshotVendorType(), type)
                || !Objects.equals(draft.getSnapshotTier(), vendor.getTier())
                || !Objects.equals(draft.getSnapshotCriticality(), criticality)
                || !Objects.equals(draft.getSnapshotTarget(), vendor.getPostureTarget())
                || !Objects.equals(draft.getAdjustedScore(), vendor.getAdjustedScore())
                || !Objects.equals(draft.getInherentScore(), vendor.getInherentScore())
                || !Objects.equals(draft.getAssuranceCredit(), vendor.getAssuranceCredit());
    }

    private TprmAssessment currentDraft(TprmVendor vendor) {
        if (vendor.getCurrentAssessmentId() != null) {
            return assessmentRepository.findById(vendor.getCurrentAssessmentId()).orElse(null);
        }
        return assessmentRepository.findByVendorIdOrderByCreatedAtDesc(vendor.getId()).stream().findFirst().orElse(null);
    }

    private void copyItemsFromQuestionnaire(TprmAssessment a, List<TprmQuestionnaireItem> qItems) {
        int order = 1;
        for (TprmQuestionnaireItem qItem : qItems) {
            TprmControl control = questionnaireService.ensureControl(qItem);
            TprmAssessmentItem item = new TprmAssessmentItem();
            item.setAssessmentId(a.getId());
            item.setControlId(control.getId());
            item.setControlCode(TprmControlDisplay.first(qItem.getControlCode(), control.getControlCode()));
            item.setDomain(TprmControlDisplay.first(qItem.getDomain(), control.getDomain()));
            item.setRequirement(TprmControlDisplay.first(qItem.getRequirement(), control.getRequirement()));
            item.setRequiredEvidence(TprmControlDisplay.first(qItem.getRequiredEvidence(), control.getRequiredEvidence()));
            item.setSortOrder(order++);
            item.setEvidenceMandatory(qItem.isEvidenceMandatory());
            itemRepository.save(item);
        }
    }

    /** Draft only. Sets the vendor that will receive the questionnaire (Assign dialog can correct it). */
    private TprmVendor applyIssueVendor(TprmAssessment assessment, Long vendorId) {
        if (vendorId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a vendor to assign");
        }
        TprmVendor newVendor = vendorService.requireVendor(vendorId);
        TprmVendor oldVendor = vendorService.requireVendor(assessment.getVendorId());
        if (!oldVendor.getId().equals(newVendor.getId())) {
            releaseDraftLink(oldVendor, assessment.getId());
            assessment.setVendorId(newVendor.getId());
            snapshotFromVendor(assessment, newVendor);
            newVendor.setCurrentAssessmentId(assessment.getId());
            newVendor.setLifecycleState(VendorLifecycleState.QUESTIONNAIRE_READY);
            vendorService.persist(newVendor);
        }
        return vendorService.requireVendor(assessment.getVendorId());
    }

    private void releaseDraftLink(TprmVendor vendor, long assessmentId) {
        if (!Objects.equals(vendor.getCurrentAssessmentId(), assessmentId)) {
            return;
        }
        vendor.setCurrentAssessmentId(null);
        if (vendor.getLifecycleState() == VendorLifecycleState.QUESTIONNAIRE_READY) {
            vendor.setLifecycleState(vendorService.profileReady(vendor) && vendor.getTier() != null
                    ? VendorLifecycleState.CATEGORISED
                    : VendorLifecycleState.PROFILE_INCOMPLETE);
        }
        vendorService.persist(vendor);
    }

    private void snapshotFromVendor(TprmAssessment a, TprmVendor vendor) {
        a.setSnapshotVendorType(vendor.getVendorType() != null ? vendor.getVendorType().name() : null);
        a.setSnapshotTier(vendor.getTier());
        a.setSnapshotCriticality(vendor.getCriticality() != null ? vendor.getCriticality().name() : null);
        a.setSnapshotTarget(vendor.getPostureTarget());
        a.setInherentScore(vendor.getInherentScore());
        a.setAssuranceCredit(vendor.getAssuranceCredit());
        a.setAdjustedScore(vendor.getAdjustedScore());
    }

    private List<String> inviteEmails(TprmVendor vendor, CreateAssessmentRequest req) {
        LinkedHashSet<String> emails = new LinkedHashSet<>();
        String primary = req.spocEmail() != null && !req.spocEmail().isBlank()
                ? req.spocEmail() : null;
        if (primary == null) {
            TprmVendorContact spoc = vendorService.primarySpoc(vendor.getId());
            if (spoc != null) {
                primary = spoc.getEmail();
            }
        }
        if (primary == null || primary.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor SPOC email is required to issue");
        }
        EmailRules.requireValid(primary, "SPOC email");
        emails.add(primary.trim().toLowerCase(Locale.ROOT));
        if (req.extraEmails() != null) {
            for (String extra : req.extraEmails()) {
                EmailRules.requireValidIfPresent(extra, "Contributor email");
                if (extra != null && !extra.isBlank()) {
                    emails.add(extra.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        for (TprmVendorContact c : vendorService.contacts(vendor.getId())) {
            if (c.getRole() == ContactRole.CONTRIBUTOR && EmailRules.isValid(c.getEmail())) {
                emails.add(c.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(emails);
    }

    private IssuedSecrets issueTokens(User admin, TprmAssessment assessment, List<String> emails, Instant expiresAt) {
        IssuedSecrets secrets = new IssuedSecrets();
        boolean first = true;
        for (String email : emails) {
            String rawToken = SecureTokenUtils.randomToken();
            persistNewToken(admin, assessment, email, rawToken, expiresAt);
            secrets.put(email, rawToken);
            if (first) {
                secrets.primaryToken = rawToken;
                first = false;
            }
        }
        return secrets;
    }

    /**
     * Keep the same portal token hash so the original email link still works.
     * OTP is not minted here: the vendor must confirm email, then request a code.
     */
    private IssuedSecrets refreshTokensForReturn(User admin, TprmAssessment assessment,
                                                 List<String> emails, Instant expiresAt) {
        IssuedSecrets secrets = new IssuedSecrets();
        List<TprmVendorToken> existing = tokenRepository.findByAssessmentId(assessment.getId());
        boolean first = true;
        for (String email : emails) {
            List<TprmVendorToken> mine = new ArrayList<>();
            for (TprmVendorToken row : existing) {
                if (row.getStatus() == VendorTokenStatus.REVOKED) {
                    continue;
                }
                if (row.getInviteeEmail() != null && row.getInviteeEmail().equalsIgnoreCase(email)) {
                    mine.add(row);
                }
            }
            String rawToken;
            if (mine.isEmpty()) {
                rawToken = SecureTokenUtils.randomToken();
                persistNewToken(admin, assessment, email, rawToken, expiresAt);
            } else {
                rawToken = null;
                for (TprmVendorToken row : mine) {
                    if (row.getTokenPreview() != null && !row.getTokenPreview().isBlank()) {
                        rawToken = row.getTokenPreview();
                        break;
                    }
                }
                if (rawToken == null) {
                    rawToken = SecureTokenUtils.randomToken();
                    persistNewToken(admin, assessment, email, rawToken, expiresAt);
                }
                for (TprmVendorToken row : mine) {
                    row.setOtpHash(placeholderOtpHash());
                    row.setStatus(VendorTokenStatus.PENDING);
                    row.setConsumedAt(null);
                    row.setExpiresAt(expiresAt);
                    tokenRepository.save(row);
                }
            }
            secrets.put(email, rawToken);
            if (first) {
                secrets.primaryToken = rawToken;
                first = false;
            }
        }
        return secrets;
    }

    private void persistNewToken(User admin, TprmAssessment assessment, String email,
                                 String rawToken, Instant expiresAt) {
        TprmVendorToken tokenRow = new TprmVendorToken();
        tokenRow.setAssessmentId(assessment.getId());
        tokenRow.setInviteeEmail(email);
        tokenRow.setTokenHash(SecureTokenUtils.sha256(rawToken));
        tokenRow.setOtpHash(placeholderOtpHash());
        tokenRow.setTokenPreview(rawToken);
        tokenRow.setStatus(VendorTokenStatus.PENDING);
        tokenRow.setExpiresAt(expiresAt);
        tokenRow.setCreatedByUserId(admin.getId());
        tokenRepository.save(tokenRow);
    }

    /** Unusable until the vendor confirms email and request-otp mints a real code. */
    private static String placeholderOtpHash() {
        return SecureTokenUtils.sha256(SecureTokenUtils.randomOtp());
    }

    private void sendAssignMail(String email, TprmVendor vendor, TprmAssessment assessment,
                                String accessLink, Instant expiresAt) {
        NotificationMailContent mail = tprmMailComposer.assign(
                vendor.getName(), assessment.getTitle(), assessment.getDisplayId(),
                questionnaireName(assessment), assessment.getDueDate(), expiresAt, accessLink);
        notificationService.queueHtmlEmail("VENDOR_QUESTIONNAIRE_SENT", email, mail.subject(), mail.plain(), mail.html());
    }

    private void sendReturnMail(String email, TprmVendor vendor, TprmAssessment assessment,
                                String accessLink, Instant expiresAt) {
        NotificationMailContent mail = tprmMailComposer.returned(
                vendor.getName(), assessment.getTitle(), assessment.getDisplayId(),
                questionnaireName(assessment), expiresAt, accessLink);
        notificationService.queueHtmlEmail("VENDOR_QUESTIONNAIRE_RETURNED", email, mail.subject(), mail.plain(), mail.html());
    }

    private void sendClosedMail(String email, TprmVendor vendor, TprmAssessment assessment, boolean compliant) {
        String posture = assessment.getPosturePercent() == null
                ? null
                : assessment.getPosturePercent().multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP).toPlainString();
        NotificationMailContent mail = tprmMailComposer.closed(
                vendor.getName(), assessment.getTitle(), assessment.getDisplayId(), compliant, posture);
        notificationService.queueHtmlEmail("VENDOR_PROJECT_CLOSED", email, mail.subject(), mail.plain(), mail.html());
    }

    private String nextDisplayId() {
        int year = Year.now().getValue();
        String prefix = "VQ-" + year + "-";
        long count = assessmentRepository.countByDisplayIdStartingWith(prefix);
        return prefix + String.format("%03d", count + 1);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String urlEnc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final Set<AssessmentStatus> FREEZE = Set.of(
            AssessmentStatus.ISSUED, AssessmentStatus.IN_PROGRESS, AssessmentStatus.SUBMITTED,
            AssessmentStatus.IN_REVIEW, AssessmentStatus.RETURNED);

    private final class IssuedSecrets {
        private String primaryToken;
        private final Map<String, String> tokens = new HashMap<>();

        void put(String email, String token) {
            tokens.put(email, token);
        }

        String linkFor(String email) {
            String tenantId = TenantContext.getTenantId();
            return securityProperties.getSso().getFrontendBaseUrl()
                    + "/vendor-portal?tenant=" + urlEnc(tenantId) + "&token=" + urlEnc(tokens.get(email));
        }

        String primaryToken() {
            return primaryToken;
        }
    }
}
