package com.a9.arbor.grc.modules.dashboard.web.dto;

import java.time.Instant;
import java.util.List;

public record DashboardSummaryResponse(
        Instant generatedAt,
        List<DashboardWidget> widgets,
        List<RecentActivityItem> recentActivity) {
}
