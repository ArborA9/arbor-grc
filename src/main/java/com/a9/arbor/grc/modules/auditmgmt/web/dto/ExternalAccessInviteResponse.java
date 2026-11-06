package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.ExternalAccessStatus;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditExternalAccess;

import java.time.Instant;

public record ExternalAccessInviteResponse(
        Long id,
        Long auditId,
        String inviteeName,
        String inviteeEmail,
        ExternalAccessStatus status,
        Instant expiresAt,
        Instant createdAt,
        String debugAccessLink,
        String debugOtp,
        String debugToken
) {
    public static ExternalAccessInviteResponse from(GrcAuditExternalAccess row) {
        return from(row, null, null, null);
    }

    public static ExternalAccessInviteResponse from(GrcAuditExternalAccess row,
                                                    String debugAccessLink,
                                                    String debugOtp,
                                                    String debugToken) {
        return new ExternalAccessInviteResponse(
                row.getId(), row.getAuditId(), row.getInviteeName(), row.getInviteeEmail(),
                row.getStatus(), row.getExpiresAt(), row.getCreatedAt(),
                debugAccessLink, debugOtp, debugToken);
    }
}
