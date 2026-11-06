package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessLog;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditAccessLogAction;

import java.time.Instant;

public record AccessLogResponse(
        Long id,
        Long grantId,
        Long userId,
        Instant viewedAt,
        String moduleCode,
        String recordRef,
        AuditAccessLogAction action,
        String documentName,
        String classification,
        String withinScope
) {
    public static AccessLogResponse from(AuditAccessLog row) {
        return new AccessLogResponse(
                row.getId(), row.getGrantId(), row.getUserId(), row.getViewedAt(),
                row.getModuleCode(), row.getRecordRef(), row.getAction(),
                row.getDocumentName(), row.getClassification(), row.getWithinScope());
    }
}
