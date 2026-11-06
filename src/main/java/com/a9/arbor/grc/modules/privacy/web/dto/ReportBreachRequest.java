package com.a9.arbor.grc.modules.privacy.web.dto;

import jakarta.validation.constraints.Size;

public record ReportBreachRequest(
        @Size(max = 255) String authorityReference
) {
}
