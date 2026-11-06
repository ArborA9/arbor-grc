package com.a9.arbor.grc.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateFrameworkNodeRequest(
        @Size(max = 64) String code,
        @NotBlank @Size(max = 512) String title,
        String definition,
        @Size(max = 64) String controlType,
        @Size(max = 8) String priority,
        @Size(max = 32) String recordType,
        @Size(max = 128) String officialRef,
        Boolean inSoa,
        Boolean inGapAssessment,
        Boolean inAuditChecklist,
        Boolean inRiskRegister,
        /** Keys: what_it_means, evidence, default_applicability, sheet_tab, section_type, … */
        Map<String, String> attributes
) {
}
