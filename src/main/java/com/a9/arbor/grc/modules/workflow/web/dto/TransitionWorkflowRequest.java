package com.a9.arbor.grc.modules.workflow.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransitionWorkflowRequest(
        @NotBlank @Size(max = 64) String action,
        @Size(max = 2000) String comment
) {
}
