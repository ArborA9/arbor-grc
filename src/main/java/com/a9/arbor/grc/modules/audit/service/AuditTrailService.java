package com.a9.arbor.grc.modules.audit.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.domain.AuditAuthMethod;
import com.a9.arbor.grc.modules.audit.domain.AuditLog;
import com.a9.arbor.grc.modules.audit.repo.AuditLogRepository;
import com.a9.arbor.grc.modules.audit.web.dto.AuditLogResponse;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.tenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Append-only audit trail. Failures are logged but never fail the business operation.
 */
@Service
public class AuditTrailService {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailService.class);
    private static final int EXPORT_MAX_ROWS = 10_000;

    private final AuditLogRepository auditLogRepository;

    public AuditTrailService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(AuditActionType actionType,
                       String module,
                       Long userId,
                       String userRole,
                       String recordType,
                       String recordId,
                       String recordName,
                       String fieldChanged,
                       String oldValue,
                       String newValue,
                       AuditAuthMethod authMethod,
                       String ipAddress) {
        record(actionType, module, userId, userRole, recordType, recordId, recordName,
                fieldChanged, oldValue, newValue, authMethod, ipAddress, null);
    }

    @Transactional
    public void record(AuditActionType actionType,
                       String module,
                       Long userId,
                       String userRole,
                       String recordType,
                       String recordId,
                       String recordName,
                       String fieldChanged,
                       String oldValue,
                       String newValue,
                       AuditAuthMethod authMethod,
                       String ipAddress,
                       Boolean mfaVerified) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Skipping audit log (no tenant context): {} / {}", module, actionType);
            return;
        }
        try {
            AuditLog row = new AuditLog();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setUserRole(trim(userRole, 255));
            row.setActionType(actionType);
            row.setModule(trim(module, 64));
            row.setRecordType(trim(recordType, 64));
            row.setRecordId(trim(recordId, 64));
            row.setRecordName(trim(recordName, 500));
            row.setFieldChanged(trim(fieldChanged, 255));
            row.setOldValue(trim(oldValue, 2000));
            row.setNewValue(trim(newValue, 2000));
            row.setIpAddress(trim(ipAddress != null ? ipAddress : currentIp(), 64));
            row.setAuthMethod(authMethod);
            row.setMfaVerified(mfaVerified);
            auditLogRepository.save(row);
        } catch (Exception e) {
            log.error("Failed to write audit log for {} {}: {}", module, actionType, e.getMessage());
        }
    }

    public void recordLogin(Long userId, String userRole, String email, AuditAuthMethod authMethod) {
        recordLogin(userId, userRole, email, authMethod, null);
    }

    /** Path C B4: {@code mfaVerified} records whether this login satisfied MFA (SSO claim trust or local TOTP). */
    public void recordLogin(Long userId, String userRole, String email, AuditAuthMethod authMethod, Boolean mfaVerified) {
        record(AuditActionType.LOGIN, "IAM", userId, userRole, "User",
                userId != null ? userId.toString() : null, email,
                null, null, null, authMethod, null, mfaVerified);
    }

    public void recordUserCreated(Long actorUserId, String actorRole, Long createdUserId,
                                  String createdEmail, String authTypeLabel) {
        record(AuditActionType.CREATED, "IAM", actorUserId, actorRole, "User",
                createdUserId != null ? createdUserId.toString() : null, createdEmail,
                "authType", null, authTypeLabel, null, null);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                .map(AuditLogResponse::from);
    }

    /**
     * CSV export (Path B7). Optional {@code module} / {@code actionType} filters.
     * Export is itself logged (req §8).
     */
    @Transactional
    public ExportFile exportCsv(User actor, String module, String actionTypeRaw, Integer limit) {
        String moduleFilter = blankToNull(module);
        AuditActionType actionFilter = parseActionOptional(actionTypeRaw);
        int size = limit == null ? EXPORT_MAX_ROWS : Math.min(Math.max(limit, 1), EXPORT_MAX_ROWS);
        List<AuditLog> rows = auditLogRepository
                .findForExport(moduleFilter, actionFilter, PageRequest.of(0, size))
                .getContent();

        StringBuilder sb = new StringBuilder();
        sb.append("id,createdAt,actionType,module,recordType,recordId,recordName,")
                .append("fieldChanged,oldValue,newValue,userId,userRole,authMethod,ipAddress\n");
        for (AuditLog row : rows) {
            sb.append(csv(row.getId()))
                    .append(',').append(csv(row.getCreatedAt() != null ? row.getCreatedAt().toString() : ""))
                    .append(',').append(csv(row.getActionType() != null ? row.getActionType().name() : ""))
                    .append(',').append(csv(row.getModule()))
                    .append(',').append(csv(row.getRecordType()))
                    .append(',').append(csv(row.getRecordId()))
                    .append(',').append(csv(row.getRecordName()))
                    .append(',').append(csv(row.getFieldChanged()))
                    .append(',').append(csv(row.getOldValue()))
                    .append(',').append(csv(row.getNewValue()))
                    .append(',').append(csv(row.getUserId()))
                    .append(',').append(csv(row.getUserRole()))
                    .append(',').append(csv(row.getAuthMethod() != null ? row.getAuthMethod().name() : ""))
                    .append(',').append(csv(row.getIpAddress()))
                    .append('\n');
        }

        String role = actor.getRoles().stream().map(Enum::name).sorted().findFirst().orElse("UNKNOWN");
        record(AuditActionType.EXPORT, "AUDIT", actor.getId(), role, "AUDIT_LOG",
                null, "audit_log.csv", "export", null,
                "rows=" + rows.size()
                        + (moduleFilter != null ? ";module=" + moduleFilter : "")
                        + (actionFilter != null ? ";action=" + actionFilter.name() : ""),
                null, null);

        String tid = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : "tenant";
        String filename = "audit-trail-" + tid + ".csv";
        return new ExportFile(sb.toString().getBytes(StandardCharsets.UTF_8), filename, "text/csv; charset=UTF-8");
    }

    private static AuditActionType parseActionOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return AuditActionType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid actionType", ex);
        }
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    private static String currentIp() {
        try {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                String forwarded = req.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
            /* non-web thread */
        }
        return null;
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= max ? t : t.substring(0, max);
    }
}
