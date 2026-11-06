package com.a9.arbor.grc.modules.bcms.web.dto;

import java.time.LocalDate;

public record UpdateBiaCycleRequest(
        String displayId,
        LocalDate issueDate,
        LocalDate nextReviewDate,
        String versionLabel,
        String objective,
        String scopeText,
        String mbco
) {
}
