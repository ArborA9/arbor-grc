package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignBiaRequest(
        @NotNull Long userId,
        LocalDate dueDate
) {
}
