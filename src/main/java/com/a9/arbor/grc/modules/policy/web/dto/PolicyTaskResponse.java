package com.a9.arbor.grc.modules.policy.web.dto;

import com.a9.arbor.grc.modules.policy.domain.PolicyCategory;

import java.time.LocalDate;

/** Compact payload for My Tasks policy acknowledgement. */
public record PolicyTaskResponse(
        Long policyId,
        String title,
        PolicyCategory category,
        String versionLabel,
        String bodyText,
        boolean hasFile,
        String originalFilename,
        LocalDate ackDeadline,
        boolean alreadyAcknowledged
) {
}
