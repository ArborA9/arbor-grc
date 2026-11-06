package com.a9.arbor.grc.modules.evidence.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEvidenceLinkRequest(
        @NotBlank @Size(max = 32) String moduleCode,
        @NotBlank @Size(max = 64) String recordType,
        @NotBlank @Size(max = 64) String recordId,
        @Size(max = 64) String workflowStage
) {
}
