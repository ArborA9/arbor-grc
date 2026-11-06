package com.a9.arbor.grc.modules.risk.web.dto;

import jakarta.validation.constraints.NotNull;

public record OwnerReassignRequest(@NotNull Long userId) {
}
