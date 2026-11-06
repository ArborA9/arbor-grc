package com.a9.arbor.grc.modules.incident.web.dto;

import jakarta.validation.constraints.Size;

public record ResolveIncidentRequest(
        @Size(max = 8000) String resolutionNotes,
        @Size(max = 8000) String rootCause,
        @Size(max = 8000) String containmentActions
) {
}
