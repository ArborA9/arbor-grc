package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.FindingStatus;
import com.a9.arbor.grc.modules.tprm.domain.TprmFinding;

import java.time.Instant;
import java.time.LocalDate;

public record FindingResponse(
        Long id,
        Long vendorId,
        Long assessmentId,
        Long itemId,
        String controlCode,
        String domain,
        String title,
        Long ownerUserId,
        LocalDate dueDate,
        FindingStatus status,
        Instant createdAt,
        Instant closedAt
) {
    public static FindingResponse from(TprmFinding f) {
        return new FindingResponse(f.getId(), f.getVendorId(), f.getAssessmentId(), f.getItemId(),
                f.getControlCode(), f.getDomain(), f.getTitle(), f.getOwnerUserId(), f.getDueDate(),
                f.getStatus(), f.getCreatedAt(), f.getClosedAt());
    }
}
