package com.a9.arbor.grc.modules.org.web.dto;

import com.a9.arbor.grc.modules.org.domain.OrgUnit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record OrgUnitResponse(
        Long id,
        String name,
        int level,
        Long parentId,
        Long headUserId,
        boolean active,
        String source,
        Instant createdAt,
        Instant updatedAt,
        List<OrgUnitResponse> children
) {
    public static OrgUnitResponse leaf(OrgUnit u) {
        return new OrgUnitResponse(
                u.getId(),
                u.getName(),
                u.getLevelNum(),
                u.getParentId(),
                u.getHeadUserId(),
                u.isActive(),
                u.getSource() == null ? "MANUAL" : u.getSource(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                new ArrayList<>());
    }

    public OrgUnitResponse withChildren(List<OrgUnitResponse> kids) {
        return new OrgUnitResponse(
                id, name, level, parentId, headUserId, active, source, createdAt, updatedAt, kids);
    }
}
