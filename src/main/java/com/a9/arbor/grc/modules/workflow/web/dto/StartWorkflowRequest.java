package com.a9.arbor.grc.modules.workflow.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StartWorkflowRequest(
        @NotBlank @Size(max = 64) String definitionCode,
        @Size(max = 32) String moduleCode,
        @Size(max = 64) String recordType,
        @NotBlank @Size(max = 64) String recordId,
        @Size(max = 500) String recordName
) {
}
