package com.a9.arbor.grc.modules.soa.web.dto;

import com.a9.arbor.grc.modules.soa.domain.Soa;
import com.a9.arbor.grc.modules.soa.domain.SoaStatus;

import java.time.Instant;

public record SoaSummaryResponse(
        Long id,
        Long projectId,
        Long frameworkId,
        String frameworkCode,
        String frameworkName,
        String frameworkVersion,
        SoaStatus status,
        int totalControls,
        int applicableCount,
        int notApplicableCount,
        int unansweredCount,
        Instant completedAt,
        Instant updatedAt
) {
    public static SoaSummaryResponse from(Soa soa, int total, int applicable, int notApplicable, int unanswered) {
        return new SoaSummaryResponse(
                soa.getId(),
                soa.getProjectId(),
                soa.getFrameworkId(),
                soa.getFrameworkCode(),
                soa.getFrameworkName(),
                soa.getFrameworkVersion(),
                soa.getStatus(),
                total,
                applicable,
                notApplicable,
                unanswered,
                soa.getCompletedAt(),
                soa.getUpdatedAt());
    }
}
