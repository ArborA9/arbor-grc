package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.DsrCloseOutcome;
import com.a9.arbor.grc.modules.privacy.domain.DsrRequestType;
import com.a9.arbor.grc.modules.privacy.domain.DsrStatus;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDsr;

import java.time.Instant;
import java.time.LocalDate;

public record DsrResponse(
        Long id,
        String displayId,
        DsrRequestType requestType,
        String subjectName,
        String subjectEmail,
        String subjectReference,
        String description,
        DsrStatus status,
        Long assignedUserId,
        Instant receivedAt,
        LocalDate dueAt,
        Instant closedAt,
        DsrCloseOutcome closeOutcome,
        String rejectionReason,
        Instant day20NotifiedAt,
        Instant day28NotifiedAt,
        Instant overdueNotifiedAt,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static DsrResponse from(PrivacyDsr e) {
        return new DsrResponse(
                e.getId(),
                e.getDisplayId(),
                e.getRequestType(),
                e.getSubjectName(),
                e.getSubjectEmail(),
                e.getSubjectReference(),
                e.getDescription(),
                e.getStatus(),
                e.getAssignedUserId(),
                e.getReceivedAt(),
                e.getDueAt(),
                e.getClosedAt(),
                e.getCloseOutcome(),
                e.getRejectionReason(),
                e.getDay20NotifiedAt(),
                e.getDay28NotifiedAt(),
                e.getOverdueNotifiedAt(),
                e.getCreatedByUserId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
