package com.a9.arbor.grc.modules.policy.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPolicyRequest(
        @NotBlank @Size(max = 4000) String reason
) {
}
