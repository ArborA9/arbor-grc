package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.TprmQuestionnaire;

import java.time.Instant;
import java.util.List;

public record QuestionnaireResponse(
        Long id,
        String name,
        int controlCount,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        List<ItemView> items
) {
    public record ItemView(
            Long id,
            Long controlId,
            String controlCode,
            String domain,
            String requirement,
            String requiredEvidence,
            boolean evidenceMandatory,
            int sortOrder
    ) {
    }

    public static QuestionnaireResponse from(TprmQuestionnaire q, List<ItemView> items) {
        return new QuestionnaireResponse(
                q.getId(), q.getName(), items.size(), q.getCreatedByUserId(),
                q.getCreatedAt(), q.getUpdatedAt(), items);
    }
}
