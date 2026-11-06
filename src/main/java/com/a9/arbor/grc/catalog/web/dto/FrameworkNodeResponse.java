package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.FrameworkNode;

import java.util.Map;

public record FrameworkNodeResponse(
        Long id,
        Long parentId,
        int level,
        String code,
        String title,
        String definition,
        String controlType,
        String priority,
        int displayOrder,
        String recordType,
        String officialRef,
        boolean inSoa,
        boolean inGapAssessment,
        boolean inAuditChecklist,
        boolean inRiskRegister,
        boolean retired,
        Map<String, String> attributes
) {
    public static FrameworkNodeResponse from(FrameworkNode n) {
        return from(n, Map.of());
    }

    public static FrameworkNodeResponse from(FrameworkNode n, Map<String, String> attributes) {
        return new FrameworkNodeResponse(
                n.getId(),
                n.getParentId(),
                n.getLevel(),
                n.getNodeCode(),
                n.getTitle(),
                n.getDefinitionText(),
                n.getControlType(),
                n.getPriority(),
                n.getDisplayOrder(),
                n.getRecordType(),
                n.getOfficialRef(),
                n.isInSoa(),
                n.isInGapAssessment(),
                n.isInAuditChecklist(),
                n.isInRiskRegister(),
                n.isRetired(),
                attributes == null || attributes.isEmpty() ? Map.of() : Map.copyOf(attributes));
    }
}
