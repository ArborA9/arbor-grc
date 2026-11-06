package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.BreachSeverity;
import com.a9.arbor.grc.modules.privacy.domain.BreachStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyBreach;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record BreachResponse(
        Long id,
        String displayId,
        String title,
        String description,
        BreachSeverity severity,
        Instant discoveredAt,
        Instant reportDeadlineAt,
        Instant reportedAt,
        String authorityReference,
        String affectedSubjectsEstimate,
        BreachStatus status,
        Instant hour48NotifiedAt,
        Instant overdueNotifiedAt,
        Instant closedAt,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static BreachResponse from(PrivacyBreach e) {
        return new BreachResponse(
                e.getId(),
                e.getDisplayId(),
                e.getTitle(),
                e.getDescription(),
                e.getSeverity(),
                e.getDiscoveredAt(),
                e.getReportDeadlineAt(),
                e.getReportedAt(),
                e.getAuthorityReference(),
                e.getAffectedSubjectsEstimate(),
                e.getStatus(),
                e.getHour48NotifiedAt(),
                e.getOverdueNotifiedAt(),
                e.getClosedAt(),
                e.getCreatedByUserId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
