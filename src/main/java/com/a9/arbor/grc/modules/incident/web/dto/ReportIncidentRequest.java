package com.a9.arbor.grc.modules.incident.web.dto;

import com.a9.arbor.grc.modules.incident.domain.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Contributor simplified report (§14.2). Priority defaults to P3 until Admin adjusts. */
public record ReportIncidentRequest(
        @NotBlank @Size(max = 512) String title,
        @NotNull IncidentType incidentType,
        @Size(max = 8000) String description,
        Instant occurredAt,
        @Size(max = 512) String affectedArea
) {
}
