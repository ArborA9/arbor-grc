package com.a9.arbor.grc.modules.iam.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserOrgUnitsRequest(
        @NotNull List<@NotNull Long> orgUnitIds
) {
}
