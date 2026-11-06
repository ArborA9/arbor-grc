package com.a9.arbor.grc.modules.privacy.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateDsrRequest(
        Long assignedUserId,
        String description,
        @Size(max = 255) String subjectName,
        @Size(max = 255) String subjectEmail,
        @Size(max = 255) String subjectReference
) {
}
