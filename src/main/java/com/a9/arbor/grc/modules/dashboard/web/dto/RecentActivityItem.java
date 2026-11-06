package com.a9.arbor.grc.modules.dashboard.web.dto;

import java.time.Instant;

public record RecentActivityItem(
        Long id,
        String actionType,
        String module,
        String recordName,
        String actorEmail,
        Instant at) {
}
