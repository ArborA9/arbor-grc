package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateMilestoneRequest(
        @NotBlank @Size(max = 512) String milestoneTitle,
        LocalDate dueDate
) {
}
