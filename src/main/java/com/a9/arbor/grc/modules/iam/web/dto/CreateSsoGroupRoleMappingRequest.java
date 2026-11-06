package com.a9.arbor.grc.modules.iam.web.dto;

import com.a9.arbor.grc.modules.iam.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSsoGroupRoleMappingRequest(
        @NotBlank String idpGroupName,
        @NotNull Role role
) {
}
