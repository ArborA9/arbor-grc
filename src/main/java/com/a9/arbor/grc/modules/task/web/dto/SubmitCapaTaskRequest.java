package com.a9.arbor.grc.modules.task.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SubmitCapaTaskRequest(
        @NotBlank String rootCause,
        @NotBlank String correctiveAction,
        String preventiveAction,
        String progressNotes
) {
}
