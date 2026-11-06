package com.a9.arbor.grc.modules.policy.web.dto;

import com.a9.arbor.grc.modules.policy.domain.PolicyAckScopeType;
import com.a9.arbor.grc.modules.policy.domain.PolicyCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdatePolicyRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull PolicyCategory category,
        Long orgUnitId,
        @Size(max = 50000) String bodyText,
        LocalDate reviewDate,
        Boolean ackRequired,
        PolicyAckScopeType ackScopeType,
        LocalDate ackDeadline,
        Long ownerUserId,
        List<Long> frameworkIds,
        List<Long> ackOrgUnitIds,
        List<Long> ackUserIds
) {
}
