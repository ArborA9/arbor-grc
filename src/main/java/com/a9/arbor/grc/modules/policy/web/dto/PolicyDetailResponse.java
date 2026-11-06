package com.a9.arbor.grc.modules.policy.web.dto;

import com.a9.arbor.grc.modules.policy.domain.PolicyAckScopeType;
import com.a9.arbor.grc.modules.policy.domain.PolicyCategory;
import com.a9.arbor.grc.modules.policy.domain.PolicyStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PolicyDetailResponse(
        PolicySummaryResponse policy,
        String bodyDraft,
        boolean hasDraftFile,
        String draftFilename,
        String rejectReason,
        String reviewNotes,
        List<PolicyFrameworkResponse> frameworks,
        List<Long> ackOrgUnitIds,
        List<Long> ackUserIds,
        PolicyVersionResponse currentVersion,
        boolean currentUserAcknowledged
) {
}
