package com.a9.arbor.grc.modules.evidence.web.dto;

import com.a9.arbor.grc.modules.evidence.domain.Evidence;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceVersion;

import java.time.Instant;
import java.time.LocalDate;

public record EvidenceSummaryResponse(
        long id,
        String title,
        String classification,
        String status,
        LocalDate expiresOn,
        int currentVersion,
        String latestFilename,
        long latestSizeBytes,
        int linkCount,
        Instant updatedAt,
        /** Present when listed for a specific record link (J3 stage tag). */
        String workflowStage,
        long createdByUserId
) {
    public static EvidenceSummaryResponse from(Evidence e, EvidenceVersion latest, int linkCount) {
        return from(e, latest, linkCount, null);
    }

    public static EvidenceSummaryResponse from(Evidence e, EvidenceVersion latest, int linkCount,
                                               String workflowStage) {
        return new EvidenceSummaryResponse(
                e.getId(),
                e.getTitle(),
                e.getClassification().name(),
                e.getStatus().name(),
                e.getExpiresOn(),
                e.getCurrentVersion(),
                latest != null ? latest.getOriginalFilename() : null,
                latest != null ? latest.getSizeBytes() : 0,
                linkCount,
                e.getUpdatedAt(),
                workflowStage,
                e.getCreatedByUserId()
        );
    }
}
