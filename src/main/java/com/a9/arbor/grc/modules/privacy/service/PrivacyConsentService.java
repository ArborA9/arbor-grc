package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.common.validation.EmailRules;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.privacy.domain.ConsentStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyConsent;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyConsentRepository;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.ConsentResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateConsentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Year;
import java.util.List;

@Service
public class PrivacyConsentService {

    private final PrivacyConsentRepository consentRepository;
    private final PrivacyRopaRepository ropaRepository;
    private final AuditTrailService auditTrailService;

    public PrivacyConsentService(PrivacyConsentRepository consentRepository,
                                 PrivacyRopaRepository ropaRepository,
                                 AuditTrailService auditTrailService) {
        this.consentRepository = consentRepository;
        this.ropaRepository = ropaRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> list() {
        return consentRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(ConsentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConsentResponse get(long id) {
        return ConsentResponse.from(require(id));
    }

    @Transactional
    public ConsentResponse create(User user, CreateConsentRequest req) {
        EmailRules.requireValidIfPresent(req.subjectEmail(), "Subject email");
        validateRopa(req.ropaId());
        PrivacyConsent row = new PrivacyConsent();
        row.setDisplayId(nextDisplayId());
        row.setSubjectName(req.subjectName().trim());
        row.setSubjectEmail(blankToNull(req.subjectEmail()));
        row.setSubjectReference(blankToNull(req.subjectReference()));
        row.setPurpose(req.purpose().trim());
        row.setRopaId(req.ropaId());
        row.setConsentMethod(req.consentMethod());
        row.setConsentedAt(req.consentedAt() != null ? req.consentedAt() : Instant.now());
        row.setStatus(ConsentStatus.ACTIVE);
        row.setEvidenceNotes(blankToNull(req.evidenceNotes()));
        row.setCreatedByUserId(user.getId());
        consentRepository.save(row);
        audit(user, AuditActionType.CREATED, row);
        return ConsentResponse.from(row);
    }

    @Transactional
    public ConsentResponse withdraw(User user, long id) {
        PrivacyConsent row = require(id);
        if (row.getStatus() == ConsentStatus.WITHDRAWN) {
            return ConsentResponse.from(row);
        }
        row.setStatus(ConsentStatus.WITHDRAWN);
        row.setWithdrawnAt(Instant.now());
        consentRepository.save(row);
        audit(user, AuditActionType.UPDATED, row);
        return ConsentResponse.from(row);
    }

    private PrivacyConsent require(long id) {
        return consentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consent record not found"));
    }

    private void validateRopa(Long ropaId) {
        if (ropaId != null && ropaRepository.findById(ropaId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ROPA not found");
        }
    }

    private String nextDisplayId() {
        int year = Year.now().getValue();
        String prefix = "CON-" + year + "-";
        long count = consentRepository.countByDisplayIdStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void audit(User user, AuditActionType action, PrivacyConsent row) {
        auditTrailService.record(
                action, "PRIVACY_CONSENT", user.getId(), primaryRole(user),
                "PRIVACY_CONSENT", String.valueOf(row.getId()), row.getDisplayId(),
                "status", null, row.getStatus().name(), null, null);
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
