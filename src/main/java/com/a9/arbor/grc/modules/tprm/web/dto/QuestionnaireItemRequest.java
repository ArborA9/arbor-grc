package com.a9.arbor.grc.modules.tprm.web.dto;

import jakarta.validation.constraints.Size;

public record QuestionnaireItemRequest(
        Long id,
        @Size(max = 40) String controlCode,
        @Size(max = 80) String domain,
        @Size(max = 2000) String requirement,
        @Size(max = 2000) String requiredEvidence,
        Boolean evidenceMandatory
) {
}
