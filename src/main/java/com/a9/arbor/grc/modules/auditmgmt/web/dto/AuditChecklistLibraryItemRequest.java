package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import jakarta.validation.constraints.Size;

public record AuditChecklistLibraryItemRequest(
        Long id,
        @Size(max = 80) String clauseRef,
        @Size(max = 120) String section,
        @Size(max = 512) String title,
        String testFocus,
        @Size(max = 120) String soaApplicability,
        @Size(max = 300) String evidenceLives
) {
}
