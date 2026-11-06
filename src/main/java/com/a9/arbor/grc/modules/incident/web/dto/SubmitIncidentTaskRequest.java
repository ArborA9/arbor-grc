package com.a9.arbor.grc.modules.incident.web.dto;

import jakarta.validation.constraints.Size;

/** My Tasks / owner progress update; set markResolved=true to resolve. */
public record SubmitIncidentTaskRequest(
        @Size(max = 8000) String progressNotes,
        @Size(max = 8000) String containmentActions,
        @Size(max = 8000) String rootCause,
        Boolean markResolved
) {
}
