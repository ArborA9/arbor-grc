package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.EngagementScopeModule;

import java.util.List;

public record AuditorMyAccessResponse(
        List<EngagementScopeModule> modules,
        List<AccessGrantResponse> grants
) {
}
