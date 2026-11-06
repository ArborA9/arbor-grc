package com.a9.arbor.grc.modules.tprm.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCollaboratorRequest(
        @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 255) String email
) {
}
