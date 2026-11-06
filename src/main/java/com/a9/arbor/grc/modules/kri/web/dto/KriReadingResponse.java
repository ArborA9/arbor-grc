package com.a9.arbor.grc.modules.kri.web.dto;

import com.a9.arbor.grc.modules.kri.domain.KriRag;
import com.a9.arbor.grc.modules.kri.domain.KriReading;

import java.math.BigDecimal;
import java.time.Instant;

public record KriReadingResponse(
        Long id,
        Long kriId,
        BigDecimal value,
        KriRag rag,
        String notes,
        Long recordedByUserId,
        Instant recordedAt
) {
    public static KriReadingResponse from(KriReading r) {
        return new KriReadingResponse(
                r.getId(),
                r.getKriId(),
                r.getValue(),
                r.getRag(),
                r.getNotes(),
                r.getRecordedByUserId(),
                r.getRecordedAt());
    }
}
