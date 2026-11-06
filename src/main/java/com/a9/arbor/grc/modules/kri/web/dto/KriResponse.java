package com.a9.arbor.grc.modules.kri.web.dto;

import com.a9.arbor.grc.modules.kri.domain.Kri;
import com.a9.arbor.grc.modules.kri.domain.KriDirection;
import com.a9.arbor.grc.modules.kri.domain.KriFrequency;
import com.a9.arbor.grc.modules.kri.domain.KriRag;
import com.a9.arbor.grc.modules.kri.domain.KriStatus;
import com.a9.arbor.grc.modules.kri.domain.KriTrend;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record KriResponse(
        Long id,
        String displayId,
        String name,
        Long linkedRiskId,
        String metricDescription,
        String unit,
        KriFrequency frequency,
        Long ownerUserId,
        KriDirection direction,
        BigDecimal greenThreshold,
        BigDecimal amberThreshold,
        BigDecimal redThreshold,
        BigDecimal currentValue,
        BigDecimal previousValue,
        KriRag rag,
        KriTrend trend,
        LocalDate nextDueDate,
        Instant lastMeasuredAt,
        Instant redAcknowledgedAt,
        Long redAcknowledgedBy,
        KriStatus status,
        Long workTaskId,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        List<KriReadingResponse> recentReadings
) {
    public static KriResponse from(Kri k, List<KriReadingResponse> readings) {
        return new KriResponse(
                k.getId(),
                k.getDisplayId(),
                k.getName(),
                k.getLinkedRiskId(),
                k.getMetricDescription(),
                k.getUnit(),
                k.getFrequency(),
                k.getOwnerUserId(),
                k.getDirection(),
                k.getGreenThreshold(),
                k.getAmberThreshold(),
                k.getRedThreshold(),
                k.getCurrentValue(),
                k.getPreviousValue(),
                k.getRag(),
                k.getTrend(),
                k.getNextDueDate(),
                k.getLastMeasuredAt(),
                k.getRedAcknowledgedAt(),
                k.getRedAcknowledgedBy(),
                k.getStatus(),
                k.getWorkTaskId(),
                k.getCreatedByUserId(),
                k.getCreatedAt(),
                k.getUpdatedAt(),
                readings == null ? List.of() : readings);
    }

    public static KriResponse from(Kri k) {
        return from(k, List.of());
    }
}
