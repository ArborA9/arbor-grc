package com.a9.arbor.grc.modules.capa.web.dto;

import java.time.Instant;

/** One Admin return (reject) on a CAPA line. Newest first in API lists. */
public record CapaReturnHistoryItem(
        Long id,
        Instant rejectedAt,
        Long rejectedByUserId,
        String rejectedByName,
        String reason
) {
}
