package com.a9.arbor.grc.modules.policy.web.dto;

import java.time.Instant;

public record PolicyAcknowledgementResponse(
        Long id,
        Long policyVersionId,
        Long userId,
        String userEmail,
        String userFullName,
        Instant acknowledgedAt
) {
}
