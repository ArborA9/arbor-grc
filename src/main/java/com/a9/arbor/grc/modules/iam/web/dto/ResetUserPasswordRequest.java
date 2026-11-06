package com.a9.arbor.grc.modules.iam.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
        @NotBlank @Size(min = 8, max = 128) String password
) {
}
