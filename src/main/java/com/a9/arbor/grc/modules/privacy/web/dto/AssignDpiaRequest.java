package com.a9.arbor.grc.modules.privacy.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignDpiaRequest(
        @NotNull Long userId,
        LocalDate dueDate
) {
}
