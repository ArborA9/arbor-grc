package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessLog;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessLogAction;

import java.time.Instant;

public record AccessViewLogResponse(
        Long id,
        Long grantId,
        String grantDisplayId,
        Long auditId,
        String auditDisplayId,
        String auditName,
        Long userId,
        String userName,
        Instant viewedAt,
        String moduleCode,
        String recordRef,
        AuditAccessLogAction action,
        String documentName,
        String classification,
        String withinScope
) {
    public static AccessViewLogResponse from(
            AuditAccessLog row,
            String grantDisplayId,
            Long auditId,
            String auditDisplayId,
            String auditName,
            String userName) {
        return new AccessViewLogResponse(
                row.getId(), row.getGrantId(), grantDisplayId, auditId, auditDisplayId, auditName,
                row.getUserId(), userName, row.getViewedAt(), row.getModuleCode(), row.getRecordRef(),
                row.getAction(), row.getDocumentName(), row.getClassification(), row.getWithinScope());
    }
}
