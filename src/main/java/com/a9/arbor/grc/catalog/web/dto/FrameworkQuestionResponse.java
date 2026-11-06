package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.FrameworkQuestion;

/**
 * @param controlCode the node code the question hangs off
 * @param sourceRef   the ref the client authored it against when that is deeper than
 *                    {@code controlCode}: reference-pack imports only, otherwise null
 */
public record FrameworkQuestionResponse(
        Long id,
        Long frameworkId,
        Long frameworkNodeId,
        String questionCode,
        String controlCode,
        String sourceRef,
        String questionType,
        String questionText,
        String answerOptions,
        boolean isMandatory,
        String evidenceGuidance,
        int displayOrder,
        int versionNumber
) {
    public static FrameworkQuestionResponse from(FrameworkQuestion q) {
        return new FrameworkQuestionResponse(
                q.getId(), q.getFrameworkId(), q.getFrameworkNodeId(), q.getQuestionCode(),
                q.getControlCode(), q.getSourceRef(),
                q.getQuestionType(), q.getQuestionText(),
                q.getAnswerOptions(), q.isMandatory(),
                q.getEvidenceGuidance(),
                q.getDisplayOrder(), q.getVersionNumber());
    }
}
