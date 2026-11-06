package com.a9.arbor.grc.modules.policy.web.dto;

import java.time.Instant;

public record PolicyVersionResponse(
        Long id,
        Long policyId,
        int majorVersion,
        int minorVersion,
        String versionLabel,
        String bodyText,
        boolean hasFile,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Instant approvedAt,
        Long approvedByUserId,
        Instant createdAt
) {
}
