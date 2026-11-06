package com.a9.arbor.grc.catalog.web.dto;

import java.time.Instant;

public record FrameworkTenantAssignmentResponse(
        String tenantId,
        String tenantName,
        boolean moduleLicensed,
        boolean assigned,
        Instant assignedAt
) {
}
