package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessGrant;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessLog;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessLogAction;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.repo.AuditAccessGrantRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.AuditAccessLogRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AccessViewLogResponse;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuditAccessLogService {

    private static final int MAX_LOG_ROWS = 500;

    private final AuditAccessLogRepository logRepository;
    private final AuditAccessGrantRepository grantRepository;
    private final GrcAuditRepository auditRepository;
    private final UserRepository userRepository;

    public AuditAccessLogService(AuditAccessLogRepository logRepository,
                                 AuditAccessGrantRepository grantRepository,
                                 GrcAuditRepository auditRepository,
                                 UserRepository userRepository) {
        this.logRepository = logRepository;
        this.grantRepository = grantRepository;
        this.auditRepository = auditRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(User viewer, long auditId, String moduleCode, String recordRef,
                       AuditAccessLogAction action, String documentName, String classification) {
        AuditAccessGrant grant = grantRepository.findByAuditIdAndUserIdAndRevokedFalse(auditId, viewer.getId())
                .filter(g -> g.activeOn(LocalDate.now()))
                .orElse(null);
        if (grant == null) {
            return;
        }
        AuditAccessLog row = new AuditAccessLog();
        row.setGrantId(grant.getId());
        row.setUserId(viewer.getId());
        row.setViewedAt(Instant.now());
        row.setModuleCode(moduleCode);
        row.setRecordRef(recordRef);
        row.setAction(action);
        row.setDocumentName(documentName);
        row.setClassification(classification);
        row.setWithinScope("OK");
        logRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<AuditAccessLog> listForGrant(long grantId) {
        return logRepository.findByGrantIdOrderByViewedAtDesc(grantId);
    }

    public long countForGrant(long grantId) {
        return logRepository.countByGrantId(grantId);
    }

    @Transactional(readOnly = true)
    public List<AccessViewLogResponse> listAllRecent() {
        List<AuditAccessLog> rows = logRepository.findAllByOrderByViewedAtDesc();
        if (rows.size() > MAX_LOG_ROWS) {
            rows = rows.subList(0, MAX_LOG_ROWS);
        }
        Set<Long> grantIds = rows.stream().map(AuditAccessLog::getGrantId).collect(Collectors.toSet());
        Set<Long> userIds = rows.stream().map(AuditAccessLog::getUserId).collect(Collectors.toSet());
        Map<Long, AuditAccessGrant> grants = grantRepository.findAllById(grantIds).stream()
                .collect(Collectors.toMap(AuditAccessGrant::getId, g -> g));
        Set<Long> auditIds = grants.values().stream().map(AuditAccessGrant::getAuditId).collect(Collectors.toSet());
        Map<Long, GrcAudit> audits = auditRepository.findAllById(auditIds).stream()
                .collect(Collectors.toMap(GrcAudit::getId, a -> a));
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return rows.stream().map(row -> {
            AuditAccessGrant grant = grants.get(row.getGrantId());
            GrcAudit audit = grant == null ? null : audits.get(grant.getAuditId());
            User user = users.get(row.getUserId());
            return AccessViewLogResponse.from(
                    row,
                    grant == null ? "-" : grant.getDisplayId(),
                    grant == null ? null : grant.getAuditId(),
                    audit == null || audit.getDisplayId() == null ? "-" : audit.getDisplayId(),
                    audit == null ? "-" : audit.getName(),
                    user == null ? "-" : (user.getFullName() == null || user.getFullName().isBlank()
                            ? user.getEmail() : user.getFullName()));
        }).toList();
    }
}
