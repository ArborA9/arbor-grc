package com.a9.arbor.grc.modules.evidence.web.dto;

import com.a9.arbor.grc.modules.evidence.domain.Evidence;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceLink;
import com.a9.arbor.grc.modules.evidence.domain.EvidenceVersion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EvidenceDetailResponse(
        long id,
        String title,
        String classification,
        String status,
        LocalDate expiresOn,
        int currentVersion,
        long createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        List<EvidenceVersionResponse> versions,
        List<EvidenceLinkResponse> links
) {
    public static EvidenceDetailResponse from(Evidence e,
                                              List<EvidenceVersion> versions,
                                              List<EvidenceLink> links) {
        return new EvidenceDetailResponse(
                e.getId(),
                e.getTitle(),
                e.getClassification().name(),
                e.getStatus().name(),
                e.getExpiresOn(),
                e.getCurrentVersion(),
                e.getCreatedByUserId(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                versions.stream().map(EvidenceVersionResponse::from).toList(),
                links.stream().map(EvidenceLinkResponse::from).toList()
        );
    }
}
