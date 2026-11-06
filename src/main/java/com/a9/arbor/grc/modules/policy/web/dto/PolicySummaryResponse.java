package com.a9.arbor.grc.modules.policy.web.dto;

import com.a9.arbor.grc.modules.policy.domain.Policy;
import com.a9.arbor.grc.modules.policy.domain.PolicyAckScopeType;
import com.a9.arbor.grc.modules.policy.domain.PolicyCategory;
import com.a9.arbor.grc.modules.policy.domain.PolicyStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PolicySummaryResponse(
        Long id,
        String title,
        PolicyCategory category,
        PolicyStatus status,
        Long orgUnitId,
        int majorVersion,
        int minorVersion,
        String versionLabel,
        LocalDate reviewDate,
        boolean ackRequired,
        PolicyAckScopeType ackScopeType,
        LocalDate ackDeadline,
        Long ownerUserId,
        Long createdByUserId,
        Long currentVersionId,
        int ackCount,
        int ackExpected,
        Instant updatedAt
) {
    public static PolicySummaryResponse from(Policy p, int ackCount, int ackExpected) {
        return new PolicySummaryResponse(
                p.getId(),
                p.getTitle(),
                p.getCategory(),
                p.getStatus(),
                p.getOrgUnitId(),
                p.getMajorVersion(),
                p.getMinorVersion(),
                "v" + p.getMajorVersion() + "." + p.getMinorVersion(),
                p.getReviewDate(),
                p.isAckRequired(),
                p.getAckScopeType(),
                p.getAckDeadline(),
                p.getOwnerUserId(),
                p.getCreatedByUserId(),
                p.getCurrentVersionId(),
                ackCount,
                ackExpected,
                p.getUpdatedAt());
    }
}
