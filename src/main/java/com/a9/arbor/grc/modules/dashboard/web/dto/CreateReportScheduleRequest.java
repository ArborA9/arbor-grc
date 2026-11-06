package com.a9.arbor.grc.modules.dashboard.web.dto;

import com.a9.arbor.grc.modules.dashboard.domain.ReportScheduleFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateReportScheduleRequest(
        @NotBlank String reportId,
        @NotBlank String format,
        @NotNull ReportScheduleFrequency frequency,
        @NotEmpty @Size(max = 3) List<Long> recipientUserIds,
        Long soaId,
        Long gapId,
        Long auditId,
        Long exerciseId,
        String variant
) {
}
