package com.a9.arbor.grc.modules.risk.web.dto;

import com.a9.arbor.grc.modules.risk.domain.RiskProject;
import com.a9.arbor.grc.modules.risk.domain.RiskProjectSeedStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskProjectStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;

import java.time.Instant;

public record RiskProjectResponse(
        Long id,
        RiskType projectType,
        String name,
        int year,
        RiskProjectStatus status,
        Long frameworkId,
        Long createdByUserId,
        int riskCount,
        int needsActionCount,
        int seedTotal,
        RiskProjectSeedStatus seedStatus,
        Instant createdAt,
        Instant updatedAt
) {
    public static RiskProjectResponse from(RiskProject p, int riskCount) {
        return from(p, riskCount, 0);
    }

    public static RiskProjectResponse from(RiskProject p, int riskCount, int needsActionCount) {
        return new RiskProjectResponse(
                p.getId(),
                p.getProjectType(),
                p.getName(),
                p.getYear(),
                p.getStatus(),
                p.getFrameworkId(),
                p.getCreatedByUserId(),
                riskCount,
                needsActionCount,
                p.getSeedTotal(),
                p.getSeedStatus() != null ? p.getSeedStatus() : RiskProjectSeedStatus.DONE,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
