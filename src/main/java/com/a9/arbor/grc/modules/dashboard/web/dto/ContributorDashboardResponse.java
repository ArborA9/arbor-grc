package com.a9.arbor.grc.modules.dashboard.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Assigned-to-me workload snapshot. Separate from Admin {@link DashboardSummaryResponse}.
 */
public record ContributorDashboardResponse(
        Instant generatedAt,
        long openCount,
        long overdueCount,
        long dueThisWeekCount,
        long returnedCount,
        long submittedCount,
        List<ModuleCount> byModule,
        List<AgingBucket> aging,
        List<QueueItem> queue,
        List<WeekPoint> weeklySubmitted) {

    public record ModuleCount(
            String moduleCode,
            String label,
            String navTarget,
            long openCount,
            long overdueCount) {
    }

    public record AgingBucket(String id, String label, long count) {
    }

    public record QueueItem(
            long taskId,
            String title,
            String moduleLabel,
            String navTarget,
            String projectName,
            String assignedByName,
            LocalDate dueDate,
            String status,
            boolean returned,
            boolean overdue,
            boolean dueThisWeek,
            boolean waitingOnAdmin,
            boolean finished,
            String waitingOn,
            String agingLabel) {
    }

    public record WeekPoint(String weekStart, long submittedCount) {
    }
}
