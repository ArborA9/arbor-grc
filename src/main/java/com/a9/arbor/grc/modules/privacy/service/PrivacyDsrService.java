package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.common.validation.EmailRules;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.privacy.domain.DsrCloseOutcome;
import com.a9.arbor.grc.modules.privacy.domain.DsrStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDsr;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyDsrRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateDsrRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.DsrResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.RejectDsrRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateDsrRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PrivacyDsrService {

    private final PrivacyDsrRepository dsrRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    public PrivacyDsrService(PrivacyDsrRepository dsrRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             AuditTrailService auditTrailService) {
        this.dsrRepository = dsrRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<DsrResponse> list() {
        return dsrRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(DsrResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DsrResponse get(long id) {
        return DsrResponse.from(require(id));
    }

    @Transactional
    public DsrResponse create(User user, CreateDsrRequest req) {
        validateAssignee(req.assignedUserId());
        EmailRules.requireValidIfPresent(req.subjectEmail(), "Subject email");

        Instant receivedAt = Instant.now();
        LocalDate receivedDate = receivedAt.atZone(ZoneId.systemDefault()).toLocalDate();

        PrivacyDsr row = new PrivacyDsr();
        row.setDisplayId(nextDisplayId());
        row.setRequestType(req.requestType());
        row.setSubjectName(req.subjectName().trim());
        row.setSubjectEmail(blankToNull(req.subjectEmail()));
        row.setSubjectReference(blankToNull(req.subjectReference()));
        row.setDescription(blankToNull(req.description()));
        row.setStatus(DsrStatus.RECEIVED);
        row.setAssignedUserId(req.assignedUserId());
        row.setReceivedAt(receivedAt);
        row.setDueAt(receivedDate.plusDays(30));
        row.setCreatedByUserId(user.getId());
        dsrRepository.save(row);

        audit(user, AuditActionType.CREATED, row, "status", null, DsrStatus.RECEIVED.name());
        notifyDsr(row, "DSR_RECEIVED", recipientsAssignedPlusAdmin(row));
        return DsrResponse.from(row);
    }

    @Transactional
    public DsrResponse update(User user, long id, UpdateDsrRequest req) {
        PrivacyDsr row = require(id);
        assertOpen(row);
        if (req.assignedUserId() != null) {
            validateAssignee(req.assignedUserId());
            row.setAssignedUserId(req.assignedUserId());
        }
        if (req.description() != null) {
            row.setDescription(blankToNull(req.description()));
        }
        if (req.subjectName() != null && !req.subjectName().isBlank()) {
            row.setSubjectName(req.subjectName().trim());
        }
        if (req.subjectEmail() != null) {
            EmailRules.requireValidIfPresent(req.subjectEmail(), "Subject email");
            row.setSubjectEmail(blankToNull(req.subjectEmail()));
        }
        if (req.subjectReference() != null) {
            row.setSubjectReference(blankToNull(req.subjectReference()));
        }
        dsrRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, null, null, null);
        return DsrResponse.from(row);
    }

    @Transactional
    public DsrResponse start(User user, long id) {
        PrivacyDsr row = require(id);
        if (row.getStatus() != DsrStatus.RECEIVED && row.getStatus() != DsrStatus.OVERDUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only RECEIVED or OVERDUE DSR can be started");
        }
        String previous = row.getStatus().name();
        row.setStatus(DsrStatus.IN_PROGRESS);
        dsrRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, "status", previous, DsrStatus.IN_PROGRESS.name());
        return DsrResponse.from(row);
    }

    @Transactional
    public DsrResponse complete(User user, long id) {
        PrivacyDsr row = require(id);
        assertOpen(row);
        String previous = row.getStatus().name();
        row.setStatus(DsrStatus.COMPLETE);
        row.setCloseOutcome(DsrCloseOutcome.COMPLETE);
        row.setClosedAt(Instant.now());
        row.setRejectionReason(null);
        dsrRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, "status", previous, DsrStatus.COMPLETE.name());
        return DsrResponse.from(row);
    }

    @Transactional
    public DsrResponse reject(User user, long id, RejectDsrRequest req) {
        PrivacyDsr row = require(id);
        assertOpen(row);
        String previous = row.getStatus().name();
        row.setStatus(DsrStatus.REJECTED);
        row.setCloseOutcome(DsrCloseOutcome.REJECTED);
        row.setRejectionReason(req.rejectionReason().trim());
        row.setClosedAt(Instant.now());
        dsrRepository.save(row);
        audit(user, AuditActionType.REJECTED, row, "status", previous, DsrStatus.REJECTED.name());
        return DsrResponse.from(row);
    }

    private PrivacyDsr require(long id) {
        return dsrRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DSR not found"));
    }

    private void assertOpen(PrivacyDsr row) {
        if (row.getStatus() == DsrStatus.COMPLETE || row.getStatus() == DsrStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed DSR cannot be modified");
        }
    }

    private void validateAssignee(Long assignedUserId) {
        if (assignedUserId != null && userRepository.findById(assignedUserId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned user not found");
        }
    }

    private String nextDisplayId() {
        int year = Year.now().getValue();
        String prefix = "DSR-" + year + "-";
        long count = dsrRepository.countByDisplayIdStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }

    static List<String> recipientsAssignedPlusAdmin(PrivacyDsr row) {
        List<String> recipients = new ArrayList<>();
        recipients.add("ADMIN");
        if (row.getAssignedUserId() != null) {
            recipients.add(String.valueOf(row.getAssignedUserId()));
        }
        return recipients;
    }

    static List<String> recipientsAdminOwner() {
        return List.of("ADMIN", "OWNER");
    }

    static Map<String, String> dsrTemplateData(PrivacyDsr row) {
        return Map.of(
                "displayId", row.getDisplayId(),
                "requestType", row.getRequestType().name(),
                "dueAt", row.getDueAt().toString(),
                "subjectName", row.getSubjectName() != null ? row.getSubjectName() : "");
    }

    private void notifyDsr(PrivacyDsr row, String trigger, List<String> recipients) {
        notificationService.trigger(trigger, String.valueOf(row.getId()), recipients, dsrTemplateData(row));
    }

    private void audit(User user, AuditActionType action, PrivacyDsr row,
                       String field, String oldVal, String newVal) {
        auditTrailService.record(
                action, "PRIVACY_DSR", user.getId(), primaryRole(user),
                "PRIVACY_DSR", String.valueOf(row.getId()), row.getDisplayId(),
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
