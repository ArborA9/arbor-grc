package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertAuditChecklistLibraryRequest(
        @NotBlank @Size(max = 200) String name,
        List<@Valid AuditChecklistLibraryItemRequest> items
) {
}
