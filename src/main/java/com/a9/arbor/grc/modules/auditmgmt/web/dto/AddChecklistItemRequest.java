package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AddChecklistItemRequest(
        @NotBlank String clauseRef,
        String section,
        @NotBlank String title,
        String testFocus,
        String soaApplicability,
        String evidenceModule
) {
}
