package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.evidence.web.dto.EvidenceSummaryResponse;

public record AuditPortalEvidenceItemResponse(
        Long id,
        String title,
        String classification,
        String status,
        String linkedNodeCode,
        String linkedRecordType,
        String linkedRecordId
) {
    public static AuditPortalEvidenceItemResponse from(EvidenceSummaryResponse evidence,
                                                       String linkedNodeCode,
                                                       String linkedRecordType,
                                                       String linkedRecordId) {
        return new AuditPortalEvidenceItemResponse(
                evidence.id(),
                evidence.title(),
                evidence.classification(),
                evidence.status(),
                linkedNodeCode,
                linkedRecordType,
                linkedRecordId);
    }
}
