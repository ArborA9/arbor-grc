package com.a9.arbor.grc.modules.gap.web.dto;

public record GapQuestionAnswerResponse(
        Long questionId,
        Long customQuestionId,
        boolean custom,
        String questionCode,
        String questionType,
        String questionText,
        String evidenceGuidance,
        int versionNumber,
        String answer,
        String note
) {
}
