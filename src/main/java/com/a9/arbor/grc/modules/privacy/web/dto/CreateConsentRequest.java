package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.ConsentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateConsentRequest(
        @NotBlank @Size(max = 255) String subjectName,
        @Size(max = 255) String subjectEmail,
        @Size(max = 255) String subjectReference,
        @NotBlank @Size(max = 1000) String purpose,
        Long ropaId,
        @NotNull ConsentMethod consentMethod,
        Instant consentedAt,
        @Size(max = 2000) String evidenceNotes
) {
}
