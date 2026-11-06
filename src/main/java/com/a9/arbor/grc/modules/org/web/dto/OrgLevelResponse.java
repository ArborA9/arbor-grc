package com.a9.arbor.grc.modules.org.web.dto;

import com.a9.arbor.grc.modules.org.domain.OrgLevelConfig;

import java.time.Instant;

public record OrgLevelResponse(int level, String label, Instant updatedAt) {
    public static OrgLevelResponse from(OrgLevelConfig c) {
        return new OrgLevelResponse(c.getLevelNum(), c.getLabel(), c.getUpdatedAt());
    }
}
