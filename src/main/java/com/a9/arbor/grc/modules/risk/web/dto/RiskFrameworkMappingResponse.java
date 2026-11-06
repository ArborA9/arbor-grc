package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskFrameworkMapping;

public record RiskFrameworkMappingResponse(
        Long id,
        Long frameworkId,
        String frameworkCode,
        Long frameworkNodeId,
        String controlRef,
        String customRef
) {
    public static RiskFrameworkMappingResponse from(RiskFrameworkMapping m) {
        return new RiskFrameworkMappingResponse(
                m.getId(),
                m.getFrameworkId(),
                m.getFrameworkCode(),
                m.getFrameworkNodeId(),
                m.getControlRef(),
                m.getCustomRef());
    }
}
