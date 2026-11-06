package com.a9.arbor.grc.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateFrameworkNodeRequest(
        Long parentId,
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 512) String title,
        String definition,
        @Size(max = 64) String controlType,
        @Size(max = 8) String priority,
        @Size(max = 32) String recordType,
        @Size(max = 128) String officialRef,
        boolean inSoa,
        boolean inGapAssessment,
        boolean inAuditChecklist,
        boolean inRiskRegister,
        Map<String, String> attributes
) {
}
