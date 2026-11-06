package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;

public record AuditPortalRiskItemResponse(
        Long id,
        String displayId,
        String title,
        String nodeCode,
        Long orgUnitId,
        RiskLevel residualRisk,
        RiskStatus status
) {
    public static AuditPortalRiskItemResponse from(RiskRecord risk) {
        return new AuditPortalRiskItemResponse(
                risk.getId(),
                risk.getDisplayId(),
                risk.getTitle(),
                risk.getNodeCode(),
                risk.getOrgUnitId(),
                risk.getResidualRisk(),
                risk.getStatus());
    }
}
