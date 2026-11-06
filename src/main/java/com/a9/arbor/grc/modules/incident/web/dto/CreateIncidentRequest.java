package com.a9.arbor.grc.modules.incident.web.dto;

import com.a9.arbor.grc.modules.incident.domain.IncidentPriority;
import com.a9.arbor.grc.modules.incident.domain.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Admin full create. */
public record CreateIncidentRequest(
        @NotBlank @Size(max = 512) String title,
        @NotNull IncidentType incidentType,
        @Size(max = 8000) String description,
        Instant occurredAt,
        @Size(max = 512) String affectedArea,
        @NotNull IncidentPriority priority,
        Long linkedRiskId,
        Long linkedGapAssessmentId,
        Long orgUnitId,
        Long ownerUserId,
        @Size(max = 8000) String rootCause,
        @Size(max = 8000) String containmentActions
) {
}
