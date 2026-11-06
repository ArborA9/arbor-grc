package com.a9.arbor.grc.modules.evidence.web.dto;

import com.a9.arbor.grc.modules.evidence.domain.EvidenceVersion;
import com.a9.arbor.grc.modules.evidence.service.EvidenceVirusScanPolicy;

import java.time.Instant;

public record EvidenceVersionResponse(
        long id,
        int versionNumber,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String virusScanStatus,
        /** Human label: never implies clean when SKIPPED/PENDING. */
        String virusScanLabel,
        long uploadedByUserId,
        Instant uploadedAt
) {
    public static EvidenceVersionResponse from(EvidenceVersion v) {
        return new EvidenceVersionResponse(
                v.getId(),
                v.getVersionNumber(),
                v.getOriginalFilename(),
                v.getContentType(),
                v.getSizeBytes(),
                v.getVirusScanStatus().name(),
                EvidenceVirusScanPolicy.displayLabel(v.getVirusScanStatus()),
                v.getUploadedByUserId(),
                v.getUploadedAt()
        );
    }
}
