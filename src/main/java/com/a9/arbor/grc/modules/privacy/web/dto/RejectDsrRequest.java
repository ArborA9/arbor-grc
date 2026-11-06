package com.a9.arbor.grc.modules.privacy.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDsrRequest(
        @NotBlank @Size(max = 2000) String rejectionReason
) {
}
