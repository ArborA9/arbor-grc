package com.a9.arbor.grc.modules.soa.web.dto;

import com.a9.arbor.grc.modules.soa.domain.SoaApplicability;
import com.a9.arbor.grc.modules.soa.domain.SoaItem;

import java.time.Instant;

public record SoaItemResponse(
        Long id,
        Long frameworkNodeId,
        String nodeCode,
        String title,
        String priority,
        String recordType,
        boolean applicabilityLocked,
        SoaApplicability applicability,
        String justification,
        String remarks,
        Long updatedByUserId,
        Instant updatedAt
) {
    public static SoaItemResponse from(SoaItem item) {
        return from(item, null, null, false);
    }

    public static SoaItemResponse from(SoaItem item, String priority) {
        return from(item, priority, null, false);
    }

    public static SoaItemResponse from(SoaItem item, String priority, String recordType,
                                       boolean applicabilityLocked) {
        return new SoaItemResponse(
                item.getId(),
                item.getFrameworkNodeId(),
                item.getNodeCode(),
                item.getTitle(),
                priority,
                recordType,
                applicabilityLocked,
                item.getApplicability(),
                item.getJustification(),
                item.getRemarks(),
                item.getUpdatedByUserId(),
                item.getUpdatedAt());
    }
}
