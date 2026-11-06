package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.ConsentMethod;
import com.a9.arbor.grc.modules.privacy.domain.ConsentStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyConsent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record ConsentResponse(
        Long id,
        String displayId,
        String subjectName,
        String subjectEmail,
        String subjectReference,
        String purpose,
        Long ropaId,
        ConsentMethod consentMethod,
        Instant consentedAt,
        Instant withdrawnAt,
        ConsentStatus status,
        String evidenceNotes,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ConsentResponse from(PrivacyConsent e) {
        return new ConsentResponse(
                e.getId(),
                e.getDisplayId(),
                e.getSubjectName(),
                e.getSubjectEmail(),
                e.getSubjectReference(),
                e.getPurpose(),
                e.getRopaId(),
                e.getConsentMethod(),
                e.getConsentedAt(),
                e.getWithdrawnAt(),
                e.getStatus(),
                e.getEvidenceNotes(),
                e.getCreatedByUserId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
