package com.a9.arbor.grc.modules.incident.web.dto;

import com.a9.arbor.grc.modules.incident.domain.IncidentPriority;
import com.a9.arbor.grc.modules.incident.domain.IncidentType;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateIncidentRequest(
        @Size(max = 512) String title,
        IncidentType incidentType,
        @Size(max = 8000) String description,
        Instant occurredAt,
        @Size(max = 512) String affectedArea,
        IncidentPriority priority,
        Long linkedRiskId,
        Long linkedGapAssessmentId,
        Long orgUnitId,
        @Size(max = 8000) String rootCause,
        @Size(max = 8000) String containmentActions,
        @Size(max = 8000) String resolutionNotes
) {
}
