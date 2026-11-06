package com.a9.arbor.grc.modules.dashboard.web.dto;

import com.a9.arbor.grc.modules.dashboard.domain.ReportDelivery;

import java.time.Instant;

public record ReportDeliveryResponse(
        long id,
        Long scheduleId,
        String reportId,
        String filename,
        String contentType,
        Instant deliveredAt
) {
    public static ReportDeliveryResponse from(ReportDelivery d) {
        return new ReportDeliveryResponse(
                d.getId(),
                d.getScheduleId(),
                d.getReportId(),
                d.getFilename(),
                d.getContentType(),
                d.getDeliveredAt());
    }
}
