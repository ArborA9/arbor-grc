package com.a9.arbor.grc.modules.tprm.service;

import com.a9.arbor.grc.common.validation.EmailRules;
import com.a9.arbor.grc.config.ArborNotificationProperties;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.AuthType;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.iam.web.dto.LoginResponse;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceClassification;
import com.a9.arbor.grc.modules.evidence.service.EvidenceService;
import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceDetailResponse;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.notification.mail.NotificationMailContent;
import com.a9.arbor.grc.modules.notification.mail.TprmNotificationMailComposer;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.tprm.domain.AssessmentStatus;
import com.a9.arbor.grc.modules.tprm.domain.TprmAnswer;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessment;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessmentItem;
import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendorToken;
import com.a9.arbor.grc.modules.tprm.domain.VendorLifecycleState;
import com.a9.arbor.grc.modules.tprm.domain.VendorTokenStatus;
import com.a9.arbor.grc.modules.tprm.pack.TprmControlDisplay;
import com.a9.arbor.grc.modules.tprm.repo.TprmAssessmentItemRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmAssessmentRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmControlRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorTokenRepository;
import com.a9.arbor.grc.modules.tprm.web.dto.AssessmentResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.RequestVendorOtpResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.SaveAnswersRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.ValidateVendorAccessResponse;
import com.a9.arbor.grc.security.JwtService;
import com.a9.arbor.grc.security.SecureTokenUtils;
import com.a9.arbor.grc.security.VendorPortalSession;
import com.a9.arbor.grc.security.otp.OtpAttemptLimiter;
import com.a9.arbor.grc.tenancy.TenantContext;
import com.a9.arbor.grc.tenancy.TenantDataSourceManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Public vendor portal: tenant bound before JPA TX; hashed token + OTP lockout. */
@Service
public class TprmVendorAccessService {

    private static final Set<AssessmentStatus> EDITABLE = Set.of(
            AssessmentStatus.ISSUED, AssessmentStatus.IN_PROGRESS, AssessmentStatus.RETURNED);
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final TprmVendorTokenRepository tokenRepository;
    private final TprmAssessmentRepository assessmentRepository;
    private final TprmAssessmentItemRepository itemRepository;
    private final TprmControlRepository controlRepository;
    private final TprmAssessmentService assessmentService;
    private final TprmVendorService vendorService;
    private final EvidenceService evidenceService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final TprmNotificationMailComposer tprmMailComposer;
    private final JwtService jwtService;
    private final AuditTrailService auditTrailService;
    private final TenantDataSourceManager tenantDataSourceManager;
    private final TransactionTemplate readTx;
    private final TransactionTemplate writeTx;
    private final OtpAttemptLimiter otpAttemptLimiter;
    private final ArborNotificationProperties notificationProperties;

    public TprmVendorAccessService(TprmVendorTokenRepository tokenRepository,
                                   TprmAssessmentRepository assessmentRepository,
                                   TprmAssessmentItemRepository itemRepository,
                                   TprmControlRepository controlRepository,
                                   TprmAssessmentService assessmentService,
                                   TprmVendorService vendorService,
                                   EvidenceService evidenceService,
                                   UserRepository userRepository,
                                   NotificationService notificationService,
                                   TprmNotificationMailComposer tprmMailComposer,
                                   JwtService jwtService,
                                   AuditTrailService auditTrailService,
                                   TenantDataSourceManager tenantDataSourceManager,
                                   OtpAttemptLimiter otpAttemptLimiter,
                                   ArborNotificationProperties notificationProperties,
                                   @Qualifier("tenantTransactionManager") PlatformTransactionManager transactionManager) {
        this.tokenRepository = tokenRepository;
        this.assessmentRepository = assessmentRepository;
        this.itemRepository = itemRepository;
        this.controlRepository = controlRepository;
        this.assessmentService = assessmentService;
        this.vendorService = vendorService;
        this.evidenceService = evidenceService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.tprmMailComposer = tprmMailComposer;
        this.jwtService = jwtService;
        this.auditTrailService = auditTrailService;
        this.tenantDataSourceManager = tenantDataSourceManager;
        this.otpAttemptLimiter = otpAttemptLimiter;
        this.notificationProperties = notificationProperties;
        this.readTx = new TransactionTemplate(transactionManager);
        this.readTx.setReadOnly(true);
        this.writeTx = new TransactionTemplate(transactionManager);
    }

    public ValidateVendorAccessResponse validate(String tenantId, String rawToken) {
        return withTenant(tenantId, readTx, () -> {
            TprmVendorToken row = findByRawToken(rawToken);
            TprmAssessment assessment = assessmentRepository.findById(row.getAssessmentId()).orElse(null);
            if (row.getStatus() == VendorTokenStatus.REVOKED) {
                return invalid(row, assessment, "Invite revoked");
            }
            if (isExpired(row)) {
                return invalid(row, assessment, "Invite expired");
            }
            if (row.getStatus() == VendorTokenStatus.CONSUMED && assessment != null
                    && assessment.getStatus() != AssessmentStatus.RETURNED) {
                return invalid(row, assessment, "Questionnaire already submitted");
            }
            return new ValidateVendorAccessResponse(
                    true, row.getStatus(),
                    vendorNameFor(assessment),
                    assessment != null ? assessment.getDisplayId() : null,
                    row.getExpiresAt(), "OK", null);
        });
    }

    public RequestVendorOtpResponse requestOtp(String tenantId, String rawToken, String email) {
        EmailRules.requireValid(email, "Email");
        String submitted = email.trim();
        return withTenant(tenantId, writeTx, () -> {
            TprmVendorToken row = findByRawToken(rawToken);
            if (row.getStatus() == VendorTokenStatus.REVOKED) {
                throw new ResponseStatusException(HttpStatus.GONE, "Invite no longer active");
            }
            TprmAssessment assessment = assessmentRepository.findById(row.getAssessmentId()).orElse(null);
            if (row.getStatus() == VendorTokenStatus.CONSUMED && !returnedForVendorUpdate(assessment)) {
                throw new ResponseStatusException(HttpStatus.GONE, "Invite no longer active");
            }
            if (isExpired(row)) {
                throw new ResponseStatusException(HttpStatus.GONE, "Invite expired");
            }
            if (row.getInviteeEmail() == null || !row.getInviteeEmail().equalsIgnoreCase(submitted)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This email does not match this invite");
            }
            String rawOtp = SecureTokenUtils.inviteOtp(notificationProperties.emailLive());
            row.setOtpHash(SecureTokenUtils.sha256(rawOtp));
            tokenRepository.save(row);

            String title = assessment == null ? null : assessment.getTitle();
            String displayId = assessment == null ? null : assessment.getDisplayId();
            NotificationMailContent mail = tprmMailComposer.otp(
                    vendorNameFor(assessment), title, displayId, rawOtp, row.getExpiresAt());
            notificationService.queueHtmlEmail(
                    "VENDOR_QUESTIONNAIRE_OTP", row.getInviteeEmail(), mail.subject(), mail.plain(), mail.html());
            otpAttemptLimiter.clear(otpScopeKey(tenantId, rawToken));
            return new RequestVendorOtpResponse(previewOtp());
        });
    }

    public LoginResponse redeem(String tenantId, String rawToken, String rawOtp) {
        String otpScope = otpScopeKey(tenantId, rawToken);
        otpAttemptLimiter.assertAllowed(otpScope);
        return withTenant(tenantId, writeTx, () -> {
            TprmVendorToken row = findByRawToken(rawToken);
            if (row.getStatus() == VendorTokenStatus.REVOKED) {
                throw new ResponseStatusException(HttpStatus.GONE, "Invite revoked");
            }
            TprmAssessment assessment = assessmentRepository.findById(row.getAssessmentId()).orElse(null);
            if (row.getStatus() == VendorTokenStatus.CONSUMED && !returnedForVendorUpdate(assessment)) {
                throw new ResponseStatusException(HttpStatus.GONE, "Questionnaire already submitted");
            }
            if (isExpired(row)) {
                throw new ResponseStatusException(HttpStatus.GONE, "Invite expired");
            }
            if (rawOtp == null || !SecureTokenUtils.sha256(rawOtp.trim()).equals(row.getOtpHash())) {
                otpAttemptLimiter.recordFailure(otpScope);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
            }
            otpAttemptLimiter.clear(otpScope);

            User user = resolveEphemeralUser(row);
            row.setUserId(user.getId());
            row.setStatus(VendorTokenStatus.ACTIVE);
            row.setRedeemedAt(Instant.now());
            row.setConsumedAt(null);
            tokenRepository.save(row);

            if (assessment == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found");
            }
            if (assessment.getStatus() == AssessmentStatus.ISSUED) {
                assessment.setStatus(AssessmentStatus.IN_PROGRESS);
                if (assessment.getStartedAt() == null) {
                    assessment.setStartedAt(Instant.now());
                }
                assessmentRepository.save(assessment);
                var vendor = vendorService.requireVendor(assessment.getVendorId());
                vendor.setLifecycleState(VendorLifecycleState.ISSUED);
                vendorService.persist(vendor);
            }

            long minutesLeft = ChronoUnit.MINUTES.between(Instant.now(), row.getExpiresAt());
            if (minutesLeft < 1) {
                minutesLeft = 1;
            }
            List<String> roles = List.of(Role.VENDOR.name());
            String token = jwtService.issueForVendorSession(user, tenantId, roles, minutesLeft, row.getId());

            auditTrailService.record(AuditActionType.LOGIN, "TPRM", user.getId(),
                    Role.VENDOR.name(), "TPRM_VENDOR_TOKEN", String.valueOf(row.getId()),
                    assessment.getDisplayId(), null, null, "OTP redeem", null, null);

            return LoginResponse.full(token, user.getEmail(), tenantId, roles, minutesLeft);
        });
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getAssessmentForCurrentVendor(User vendorUser) {
        TprmVendorToken row = requireActiveToken(vendorUser);
        TprmAssessment assessment = assessmentRepository.findById(row.getAssessmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
        boolean canSubmit = isPrimarySpocInvite(row, assessment.getVendorId());
        return assessmentService.toResponse(assessment, assessmentService.vendorNameFor(assessment.getVendorId()),
                null, null, canSubmit);
    }

    @Transactional
    public AssessmentResponse saveAnswers(User vendorUser, SaveAnswersRequest req) {
        TprmVendorToken row = requireActiveToken(vendorUser);
        TprmAssessment assessment = requireEditable(row);
        for (var line : req.answers()) {
            TprmAssessmentItem item = itemRepository.findByIdAndAssessmentId(line.itemId(), assessment.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
            if (assessment.getStatus() == AssessmentStatus.RETURNED && !item.isReturned()) {
                continue;
            }
            item.setVendorAnswer(line.vendorAnswer());
            item.setJustification(line.justification());
            if (line.evidenceRef() != null) {
                item.setEvidenceRef(line.evidenceRef());
            }
            itemRepository.save(item);
        }
        if (assessment.getStatus() == AssessmentStatus.ISSUED) {
            assessment.setStatus(AssessmentStatus.IN_PROGRESS);
            if (assessment.getStartedAt() == null) {
                assessment.setStartedAt(Instant.now());
            }
            assessmentRepository.save(assessment);
        }
        boolean canSubmit = isPrimarySpocInvite(row, assessment.getVendorId());
        return assessmentService.toResponse(assessment, assessmentService.vendorNameFor(assessment.getVendorId()),
                null, null, canSubmit);
    }

    @Transactional
    public AssessmentResponse uploadAnswerEvidence(User vendorUser, long itemId, MultipartFile file) {
        TprmVendorToken row = requireActiveToken(vendorUser);
        TprmAssessment assessment = requireEditable(row);
        TprmAssessmentItem item = itemRepository.findByIdAndAssessmentId(itemId, assessment.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (assessment.getStatus() == AssessmentStatus.RETURNED && !item.isReturned()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only returned controls can be updated");
        }
        if (assessmentService.evidenceFileCount(item) >= TprmAssessmentService.MAX_EVIDENCE_FILES_PER_ITEM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At most " + TprmAssessmentService.MAX_EVIDENCE_FILES_PER_ITEM
                            + " evidence files are allowed per control");
        }
        EvidenceDetailResponse evidence = evidenceService.upload(
                vendorUser,
                file.getOriginalFilename(),
                EvidenceClassification.OPEN.name(),
                null,
                file,
                TprmAssessmentService.EVIDENCE_MODULE,
                TprmAssessmentService.EVIDENCE_RECORD_TYPE,
                String.valueOf(item.getId()),
                null);
        item.setEvidenceId(evidence.id());
        itemRepository.save(item);
        boolean canSubmit = isPrimarySpocInvite(row, assessment.getVendorId());
        return assessmentService.toResponse(assessment, assessmentService.vendorNameFor(assessment.getVendorId()),
                null, null, canSubmit);
    }

    @Transactional
    public AssessmentResponse removeAnswerEvidence(User vendorUser, long itemId, long evidenceId) {
        TprmVendorToken row = requireActiveToken(vendorUser);
        TprmAssessment assessment = requireEditable(row);
        TprmAssessmentItem item = itemRepository.findByIdAndAssessmentId(itemId, assessment.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (assessment.getStatus() == AssessmentStatus.RETURNED && !item.isReturned()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only returned controls can be updated");
        }
        assertEvidenceLinkedToItem(itemId, evidenceId);
        evidenceService.unlinkFromRecord(
                vendorUser,
                evidenceId,
                TprmAssessmentService.EVIDENCE_MODULE,
                TprmAssessmentService.EVIDENCE_RECORD_TYPE,
                String.valueOf(itemId));
        if (item.getEvidenceId() != null && item.getEvidenceId() == evidenceId) {
            var remaining = assessmentService.evidenceFilesForItem(item).stream()
                    .filter(f -> f.id() != null && f.id() != evidenceId)
                    .toList();
            item.setEvidenceId(remaining.isEmpty() ? null : remaining.getFirst().id());
            itemRepository.save(item);
        }
        boolean canSubmit = isPrimarySpocInvite(row, assessment.getVendorId());
        return assessmentService.toResponse(assessment, assessmentService.vendorNameFor(assessment.getVendorId()),
                null, null, canSubmit);
    }

    @Transactional(readOnly = true)
    public EvidenceService.Download downloadAnswerEvidence(User vendorUser, long itemId, long evidenceId) {
        TprmVendorToken row = requireActiveToken(vendorUser);
        TprmAssessment assessment = assessmentRepository.findById(row.getAssessmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
        itemRepository.findByIdAndAssessmentId(itemId, assessment.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        assertEvidenceLinkedToItem(itemId, evidenceId);
        return evidenceService.download(vendorUser, evidenceId, null);
    }

    private void assertEvidenceLinkedToItem(long itemId, long evidenceId) {
        boolean linked = assessmentService.evidenceFilesForItem(
                        itemRepository.findById(itemId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found")))
                .stream()
                .anyMatch(f -> f.id() != null && f.id() == evidenceId);
        if (!linked) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidence not linked to this control");
        }
    }

    @Transactional(readOnly = true)
    public ExportFile downloadExcel(User vendorUser) {
        TprmVendorToken row = requireActiveToken(vendorUser);
        TprmAssessment assessment = assessmentRepository.findById(row.getAssessmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
        return assessmentService.downloadExcel(assessment.getId());
    }

    @Transactional
    public AssessmentResponse uploadExcel(User vendorUser, MultipartFile file) {
        TprmVendorToken row = requireActiveToken(vendorUser);
        TprmAssessment assessment = requireEditable(row);
        try {
            assessmentService.mergeExcel(assessment, file.getBytes());
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
        if (assessment.getStatus() == AssessmentStatus.ISSUED) {
            assessment.setStatus(AssessmentStatus.IN_PROGRESS);
            if (assessment.getStartedAt() == null) {
                assessment.setStartedAt(Instant.now());
            }
            assessmentRepository.save(assessment);
        }
        return assessmentService.toResponse(assessment, assessmentService.vendorNameFor(assessment.getVendorId()),
                null, null);
    }

    @Transactional
    public AssessmentResponse submit(User vendorUser) {
        TprmVendorToken row = requireActiveToken(vendorUser);
        TprmAssessment assessment = requireEditable(row);
        assertPrimarySpocCanSubmit(row, assessment);
        assertReadyToSubmit(assessment);

        assessment.setStatus(AssessmentStatus.SUBMITTED);
        assessment.setSubmittedAt(Instant.now());
        assessmentRepository.save(assessment);

        for (TprmVendorToken token : tokenRepository.findByAssessmentId(assessment.getId())) {
            if (token.getStatus() != VendorTokenStatus.REVOKED) {
                token.setStatus(VendorTokenStatus.CONSUMED);
                token.setConsumedAt(Instant.now());
                tokenRepository.save(token);
            }
        }

        var vendor = vendorService.requireVendor(assessment.getVendorId());
        vendor.setLifecycleState(VendorLifecycleState.SUBMITTED);
        vendorService.persist(vendor);

        Map<String, String> data = new HashMap<>();
        data.put("assessmentTitle", assessment.getTitle());
        data.put("displayId", assessment.getDisplayId());
        data.put("vendorName", vendor.getName());
        notificationService.trigger("VENDOR_QUESTIONNAIRE_SUBMITTED", String.valueOf(assessment.getId()),
                List.of("OWNER", "ADMIN"), data);

        auditTrailService.record(AuditActionType.UPDATED, "TPRM", vendorUser.getId(),
                Role.VENDOR.name(), "TPRM_ASSESSMENT", String.valueOf(assessment.getId()),
                assessment.getDisplayId(), null, null, "vendor submitted", null, null);

        return assessmentService.toResponse(assessment, vendor.getName(), null, null);
    }

    private void assertReadyToSubmit(TprmAssessment assessment) {
        Map<Long, TprmControl> controls = new HashMap<>();
        for (TprmControl c : controlRepository.findAll()) {
            controls.put(c.getId(), c);
        }
        boolean returnedCycle = assessment.getStatus() == AssessmentStatus.RETURNED;
        for (TprmAssessmentItem item : itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(assessment.getId())) {
            if (returnedCycle && !item.isReturned()) {
                continue;
            }
            if (item.getVendorAnswer() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Every in-scope control must be answered before submit");
            }
            if (item.getJustification() == null || item.getJustification().isBlank()) {
                String code = TprmControlDisplay.code(item, controls.get(item.getControlId()));
                if (code == null || code.isBlank()) {
                    code = String.valueOf(item.getId());
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Justification is required for " + code);
            }
            if (returnedCycle && item.isReturned() && item.getReturnFingerprint() != null) {
                String current = AssessmentResponse.AssessmentItemView.answerFingerprint(
                        item, assessmentService.evidenceFilesForItem(item));
                if (item.getReturnFingerprint().equals(current)) {
                    String code = TprmControlDisplay.code(item, controls.get(item.getControlId()));
                    if (code == null || code.isBlank()) {
                        code = String.valueOf(item.getId());
                    }
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Update returned control " + code
                                    + " before submit. Change the answer, justification, or evidence.");
                }
            }
            TprmControl c = controls.get(item.getControlId());
            boolean evidenceMandatory = assessmentService.evidenceMandatory(item, c);
            if (evidenceMandatory && item.getVendorAnswer() != TprmAnswer.NA
                    && !assessmentService.itemHasEvidenceFile(item)) {
                String code = TprmControlDisplay.code(item, c);
                if (code.isBlank()) {
                    code = String.valueOf(item.getId());
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mandatory evidence file is missing for " + code);
            }
        }
    }

    private void assertPrimarySpocCanSubmit(TprmVendorToken row, TprmAssessment assessment) {
        if (!isPrimarySpocInvite(row, assessment.getVendorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the Vendor primary SPOC can submit. Additional vendor contributors can save drafts only.");
        }
    }

    private boolean isPrimarySpocInvite(TprmVendorToken row, long vendorId) {
        var spoc = vendorService.primarySpoc(vendorId);
        if (spoc == null || spoc.getEmail() == null || spoc.getEmail().isBlank()) {
            return false;
        }
        if (row.getInviteeEmail() == null) {
            return false;
        }
        return spoc.getEmail().trim().equalsIgnoreCase(row.getInviteeEmail().trim());
    }

    private TprmAssessment requireEditable(TprmVendorToken row) {
        TprmAssessment assessment = assessmentRepository.findById(row.getAssessmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
        if (!EDITABLE.contains(assessment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assessment already submitted");
        }
        return assessment;
    }

    private TprmVendorToken requireActiveToken(User vendorUser) {
        Long tokenId = VendorPortalSession.tokenId();
        if (tokenId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active questionnaire session");
        }
        TprmVendorToken row = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active questionnaire session"));
        if (row.getUserId() != null && !row.getUserId().equals(vendorUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active questionnaire session");
        }
        if (row.getInviteeEmail() == null
                || vendorUser.getEmail() == null
                || !row.getInviteeEmail().equalsIgnoreCase(vendorUser.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active questionnaire session");
        }
        if (row.getStatus() != VendorTokenStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No active questionnaire session");
        }
        return row;
    }

    private User resolveEphemeralUser(TprmVendorToken row) {
        // Tenant email is unique. The same SPOC on two vendors is one User row.
        // Isolation is the JWT invite id (vtid), not a second user.
        return userRepository.findByEmail(row.getInviteeEmail())
                .map(existing -> {
                    if (!existing.getRoles().contains(Role.VENDOR)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Email belongs to an existing user with a different role: use standard login");
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(row.getInviteeEmail());
                    u.setFullName(row.getInviteeEmail());
                    u.setAuthType(AuthType.LOCAL);
                    u.setStatus(UserStatus.ACTIVE);
                    u.setRoles(Set.of(Role.VENDOR));
                    return userRepository.save(u);
                });
    }

    private static String otpScopeKey(String tenantId, String rawToken) {
        String token = rawToken == null ? "" : rawToken.trim();
        return tenantId + ":vendor:" + SecureTokenUtils.sha256(token);
    }

    private boolean returnedForVendorUpdate(TprmAssessment assessment) {
        return assessment != null && assessment.getStatus() == AssessmentStatus.RETURNED;
    }

    private TprmVendorToken findByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token required");
        }
        return tokenRepository.findByTokenHash(SecureTokenUtils.sha256(rawToken.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token"));
    }

    private boolean isExpired(TprmVendorToken row) {
        return row.getExpiresAt().isBefore(Instant.now());
    }

    private String vendorNameFor(TprmAssessment assessment) {
        if (assessment == null) {
            return "-";
        }
        return assessmentService.vendorNameFor(assessment.getVendorId());
    }

    private ValidateVendorAccessResponse invalid(TprmVendorToken row, TprmAssessment assessment, String message) {
        return new ValidateVendorAccessResponse(
                false, row.getStatus(), vendorNameFor(assessment),
                assessment != null ? assessment.getDisplayId() : null,
                row.getExpiresAt(), message, null);
    }

    private String previewOtp() {
        return notificationProperties.emailLive() ? null : SecureTokenUtils.LOCAL_PREVIEW_OTP;
    }

    private <T> T withTenant(String tenantId, TransactionTemplate tx, java.util.function.Supplier<T> work) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant required");
        }
        tenantDataSourceManager.ensureRegistered(tenantId);
        TenantContext.setTenantId(tenantId);
        try {
            return tx.execute(status -> work.get());
        } finally {
            TenantContext.clear();
        }
    }
}
