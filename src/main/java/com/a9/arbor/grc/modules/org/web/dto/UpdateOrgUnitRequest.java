package com.a9.arbor.grc.modules.org.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrgUnitRequest(
        @NotBlank @Size(max = 200) String name,
        Long headUserId
) {
}
