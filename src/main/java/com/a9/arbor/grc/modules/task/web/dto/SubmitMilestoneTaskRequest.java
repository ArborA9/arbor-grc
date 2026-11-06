package com.a9.arbor.grc.modules.task.web.dto;

import jakarta.validation.constraints.Size;

public record SubmitMilestoneTaskRequest(
        @Size(max = 8000) String progressNotes
) {
}
