package com.a9.arbor.grc.modules.gap.web.dto;

public record GapCustomQuestionResponse(
        Long id,
        String questionText,
        int sortOrder
) {
}
