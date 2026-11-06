package com.a9.arbor.grc.modules.tprm.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveAnswersRequest(
        @NotEmpty @Valid List<SaveAnswerItem> answers
) {
}
