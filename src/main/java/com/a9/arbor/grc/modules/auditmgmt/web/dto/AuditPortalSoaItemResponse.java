package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.soa.domain.SoaApplicability;
import com.a9.arbor.grc.modules.soa.domain.SoaItem;

public record AuditPortalSoaItemResponse(
        Long id,
        Long frameworkNodeId,
        String nodeCode,
        String title,
        SoaApplicability applicability,
        String justification
) {
    public static AuditPortalSoaItemResponse from(SoaItem item) {
        return new AuditPortalSoaItemResponse(
                item.getId(),
                item.getFrameworkNodeId(),
                item.getNodeCode(),
                item.getTitle(),
                item.getApplicability(),
                item.getJustification());
    }
}
