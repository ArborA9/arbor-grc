package com.a9.arbor.grc.modules.gap.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpsertGapAnswersRequest(
        @NotNull List<Answer> answers
) {
    public record Answer(
            Long questionId,
            Long customQuestionId,
            String answer,
            String note
    ) {
    }
}
