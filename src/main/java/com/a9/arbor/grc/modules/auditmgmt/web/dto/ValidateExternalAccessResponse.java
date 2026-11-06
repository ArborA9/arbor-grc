package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.ExternalAccessStatus;

import java.time.Instant;

public record ValidateExternalAccessResponse(
        boolean valid,
        ExternalAccessStatus status,
        String auditName,
        String inviteeName,
        String inviteeEmail,
        Instant expiresAt,
        String message
) {
}
