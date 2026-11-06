package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.Size;

public record CompleteMilestoneRequest(
        @Size(max = 8000) String progressNotes
) {
}
