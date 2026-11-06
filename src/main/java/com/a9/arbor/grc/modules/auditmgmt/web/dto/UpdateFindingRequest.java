package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;

public record UpdateFindingRequest(
        FindingSeverity severity,
        String title,
        String description,
        String recommendedAction,
        String controlRef
) {
}
