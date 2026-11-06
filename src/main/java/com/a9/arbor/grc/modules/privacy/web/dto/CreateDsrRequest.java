package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.DsrRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDsrRequest(
        @NotNull DsrRequestType requestType,
        @NotBlank @Size(max = 255) String subjectName,
        @Size(max = 255) String subjectEmail,
        @Size(max = 255) String subjectReference,
        String description,
        Long assignedUserId
) {
}
