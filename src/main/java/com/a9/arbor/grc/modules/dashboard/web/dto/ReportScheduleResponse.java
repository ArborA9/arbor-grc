package com.a9.arbor.grc.modules.dashboard.web.dto;

import com.a9.arbor.grc.modules.dashboard.domain.ReportSchedule;
import com.a9.arbor.grc.modules.dashboard.domain.ReportScheduleFrequency;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record ReportScheduleResponse(
        long id,
        String reportId,
        String format,
        ReportScheduleFrequency frequency,
        List<Long> recipientUserIds,
        Long soaId,
        Long gapId,
        Long auditId,
        Long exerciseId,
        String variant,
        Instant nextRunAt,
        Instant lastRunAt,
        boolean active
) {
    public static ReportScheduleResponse from(ReportSchedule s) {
        List<Long> recipients = Arrays.stream(s.getRecipientUserIds().split(","))
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .map(Long::parseLong)
                .collect(Collectors.toList());
        return new ReportScheduleResponse(
                s.getId(),
                s.getReportId(),
                s.getFormat(),
                s.getFrequency(),
                recipients,
                s.getSoaId(),
                s.getGapId(),
                s.getAuditId(),
                s.getExerciseId(),
                s.getVariant(),
                s.getNextRunAt(),
                s.getLastRunAt(),
                s.isActive());
    }
}
