package com.a9.arbor.grc.modules.bcms.web.dto;

import jakarta.validation.constraints.NotNull;

public record SendBiaToHeadRequest(
        @NotNull Long bcHeadUserId,
        String notes
) {
}
