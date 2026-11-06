package com.a9.arbor.grc.modules.incident.web.dto;

import com.a9.arbor.grc.modules.incident.domain.IncidentPriority;
import com.a9.arbor.grc.modules.incident.domain.IncidentRecord;
import com.a9.arbor.grc.modules.incident.domain.IncidentStatus;
import com.a9.arbor.grc.modules.incident.domain.IncidentType;

import java.time.Instant;

public record IncidentResponse(
        Long id,
        String displayId,
        String title,
        IncidentType incidentType,
        String description,
        Instant occurredAt,
        String affectedArea,
        IncidentPriority priority,
        IncidentStatus status,
        Long linkedRiskId,
        Long linkedGapAssessmentId,
        Long orgUnitId,
        Long reporterUserId,
        Long ownerUserId,
        String rootCause,
        String containmentActions,
        String resolutionNotes,
        Instant responseDueAt,
        Instant resolutionDueAt,
        Instant respondedAt,
        Instant resolvedAt,
        Long resolvedByUserId,
        Instant closedAt,
        Long closedByUserId,
        Long workTaskId,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static IncidentResponse from(IncidentRecord i) {
        return new IncidentResponse(
                i.getId(),
                i.getDisplayId(),
                i.getTitle(),
                i.getIncidentType(),
                i.getDescription(),
                i.getOccurredAt(),
                i.getAffectedArea(),
                i.getPriority(),
                i.getStatus(),
                i.getLinkedRiskId(),
                i.getLinkedGapAssessmentId(),
                i.getOrgUnitId(),
                i.getReporterUserId(),
                i.getOwnerUserId(),
                i.getRootCause(),
                i.getContainmentActions(),
                i.getResolutionNotes(),
                i.getResponseDueAt(),
                i.getResolutionDueAt(),
                i.getRespondedAt(),
                i.getResolvedAt(),
                i.getResolvedByUserId(),
                i.getClosedAt(),
                i.getClosedByUserId(),
                i.getWorkTaskId(),
                i.getCreatedByUserId(),
                i.getCreatedAt(),
                i.getUpdatedAt());
    }
}
