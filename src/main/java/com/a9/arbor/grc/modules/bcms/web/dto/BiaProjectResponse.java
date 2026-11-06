package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.BiaProject;
import com.a9.arbor.grc.modules.bcms.domain.BiaProjectStatus;

import java.time.Instant;

public record BiaProjectResponse(
        Long id,
        int year,
        String name,
        BiaProjectStatus status,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        int departmentCount
) {
    public static BiaProjectResponse from(BiaProject p, int departmentCount) {
        return new BiaProjectResponse(
                p.getId(), p.getYear(), p.getName(), p.getStatus(),
                p.getCreatedByUserId(), p.getCreatedAt(), p.getUpdatedAt(), departmentCount);
    }
}
