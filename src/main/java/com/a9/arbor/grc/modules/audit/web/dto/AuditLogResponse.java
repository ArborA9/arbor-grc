package com.a9.arbor.grc.modules.audit.web.dto;

import com.a9.arbor.grc.modules.audit.domain.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        long id,
        String tenantId,
        Long userId,
        String userRole,
        String actionType,
        String module,
        String recordType,
        String recordId,
        String recordName,
        String fieldChanged,
        String oldValue,
        String newValue,
        String ipAddress,
        String authMethod,
        Boolean mfaVerified,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog row) {
        return new AuditLogResponse(
                row.getId(),
                row.getTenantId(),
                row.getUserId(),
                row.getUserRole(),
                row.getActionType().name(),
                row.getModule(),
                row.getRecordType(),
                row.getRecordId(),
                row.getRecordName(),
                row.getFieldChanged(),
                row.getOldValue(),
                row.getNewValue(),
                row.getIpAddress(),
                row.getAuthMethod() != null ? row.getAuthMethod().name() : null,
                row.getMfaVerified(),
                row.getCreatedAt());
    }
}
