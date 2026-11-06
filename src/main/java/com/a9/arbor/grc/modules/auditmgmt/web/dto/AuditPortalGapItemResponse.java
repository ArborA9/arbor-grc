package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;

public record AuditPortalGapItemResponse(
        Long id,
        Long frameworkNodeId,
        String nodeCode,
        String title,
        GapImplementationStatus status,
        int evidenceCount
) {
    public static AuditPortalGapItemResponse from(GapItem item, GapImplementationStatus effective, int evidenceCount) {
        return new AuditPortalGapItemResponse(
                item.getId(),
                item.getFrameworkNodeId(),
                item.getNodeCode(),
                item.getTitle(),
                effective,
                evidenceCount);
    }
}
