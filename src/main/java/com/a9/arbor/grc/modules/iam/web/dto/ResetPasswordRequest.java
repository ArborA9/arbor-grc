package com.a9.arbor.grc.modules.iam.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Open the link from your email to set your password") String token,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
