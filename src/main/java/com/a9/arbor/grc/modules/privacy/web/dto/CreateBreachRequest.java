package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.BreachSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateBreachRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 8000) String description,
        BreachSeverity severity,
        @NotNull Instant discoveredAt,
        @Size(max = 255) String affectedSubjectsEstimate
) {
}
