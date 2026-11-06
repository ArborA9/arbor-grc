package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.TprmAnswer;
import jakarta.validation.constraints.NotNull;

public record SaveAnswerItem(
        @NotNull Long itemId,
        TprmAnswer vendorAnswer,
        String justification,
        String evidenceRef
) {
}
