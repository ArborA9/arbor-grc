package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.privacy.domain.BreachSeverity;
import com.a9.arbor.grc.modules.privacy.domain.BreachStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyBreach;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyBreachRepository;
import com.a9.arbor.grc.modules.privacy.web.dto.BreachResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateBreachRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.ReportBreachRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PrivacyBreachService {

    private static final DateTimeFormatter DEADLINE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final PrivacyBreachRepository breachRepository;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    public PrivacyBreachService(PrivacyBreachRepository breachRepository,
                                NotificationService notificationService,
                                AuditTrailService auditTrailService) {
        this.breachRepository = breachRepository;
        this.notificationService = notificationService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<BreachResponse> list() {
        return breachRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(BreachResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BreachResponse get(long id) {
        return BreachResponse.from(require(id));
    }

    @Transactional
    public BreachResponse create(User user, CreateBreachRequest req) {
        Instant discovered = req.discoveredAt();
        PrivacyBreach row = new PrivacyBreach();
        row.setDisplayId(nextDisplayId());
        row.setTitle(req.title().trim());
        row.setDescription(blankToNull(req.description()));
        row.setSeverity(req.severity() != null ? req.severity() : BreachSeverity.MEDIUM);
        row.setDiscoveredAt(discovered);
        row.setReportDeadlineAt(discovered.plusSeconds(72 * 3600L));
        row.setAffectedSubjectsEstimate(blankToNull(req.affectedSubjectsEstimate()));
        row.setStatus(BreachStatus.OPEN);
        row.setCreatedByUserId(user.getId());
        breachRepository.save(row);
        audit(user, AuditActionType.CREATED, row, null, null, BreachStatus.OPEN.name());
        notificationService.trigger(
                "BREACH_RECORDED",
                String.valueOf(row.getId()),
                List.of("ADMIN", "OWNER"),
                templateData(row));
        return BreachResponse.from(row);
    }

    @Transactional
    public BreachResponse startInvestigation(User user, long id) {
        PrivacyBreach row = require(id);
        if (row.getStatus() != BreachStatus.OPEN && row.getStatus() != BreachStatus.OVERDUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Breach is not open for investigation");
        }
        String previous = row.getStatus().name();
        row.setStatus(BreachStatus.INVESTIGATING);
        breachRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, "status", previous, BreachStatus.INVESTIGATING.name());
        return BreachResponse.from(row);
    }

    @Transactional
    public BreachResponse reportToAuthority(User user, long id, ReportBreachRequest req) {
        PrivacyBreach row = require(id);
        if (row.getStatus() == BreachStatus.CLOSED || row.getStatus() == BreachStatus.REPORTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Breach already reported or closed");
        }
        String previous = row.getStatus().name();
        row.setStatus(BreachStatus.REPORTED);
        row.setReportedAt(Instant.now());
        row.setAuthorityReference(blankToNull(req != null ? req.authorityReference() : null));
        breachRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, "status", previous, BreachStatus.REPORTED.name());
        return BreachResponse.from(row);
    }

    @Transactional
    public BreachResponse close(User user, long id) {
        PrivacyBreach row = require(id);
        if (row.getStatus() == BreachStatus.CLOSED) {
            return BreachResponse.from(row);
        }
        String previous = row.getStatus().name();
        row.setStatus(BreachStatus.CLOSED);
        row.setClosedAt(Instant.now());
        breachRepository.save(row);
        audit(user, AuditActionType.UPDATED, row, "status", previous, BreachStatus.CLOSED.name());
        return BreachResponse.from(row);
    }

    static Map<String, String> templateData(PrivacyBreach row) {
        return Map.of(
                "displayId", row.getDisplayId(),
                "title", row.getTitle(),
                "reportDeadlineAt", DEADLINE_FMT.format(row.getReportDeadlineAt()));
    }

    static List<String> recipientsAdminOwner() {
        return List.of("ADMIN", "OWNER");
    }

    private PrivacyBreach require(long id) {
        return breachRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Breach record not found"));
    }

    private String nextDisplayId() {
        int year = Year.now().getValue();
        String prefix = "BRH-" + year + "-";
        long count = breachRepository.countByDisplayIdStartingWith(prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private void audit(User user, AuditActionType action, PrivacyBreach row,
                       String field, String oldVal, String newVal) {
        auditTrailService.record(
                action, "PRIVACY_BREACH", user.getId(), primaryRole(user),
                "PRIVACY_BREACH", String.valueOf(row.getId()), row.getDisplayId(),
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
