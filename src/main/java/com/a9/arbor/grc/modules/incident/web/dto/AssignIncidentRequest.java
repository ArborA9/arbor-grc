package com.a9.arbor.grc.modules.incident.web.dto;

import jakarta.validation.constraints.NotNull;

public record AssignIncidentRequest(
        @NotNull Long userId
) {
}
