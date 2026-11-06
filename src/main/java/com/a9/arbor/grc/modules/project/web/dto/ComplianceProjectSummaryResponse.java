package com.a9.arbor.grc.modules.project.web.dto;

import com.a9.arbor.grc.modules.project.domain.ComplianceProjectStatus;

import java.time.Instant;
import java.util.List;

public record ComplianceProjectSummaryResponse(
        Long id,
        String name,
        int year,
        ComplianceProjectStatus status,
        List<ProjectFrameworkResponse> frameworks,
        boolean includeCloud,
        Instant createdAt,
        Instant updatedAt,
        /** Doc 02 G-14: SoA assessments on this project. */
        int soaCount,
        /** Doc 02 G-14: Gap assessments on this project. */
        int gapAssessmentCount,
        /** Contributor: Gap controls assigned to this user. Admin: 0. */
        int assignedControlCount,
        /** Contributor: assigned controls still waiting on them. Admin: 0. */
        int needsActionCount
) {
}
