package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.DpiaQuestionnaire;
import com.a9.arbor.grc.modules.privacy.domain.DpiaQuestionnaireQuestion;
import com.a9.arbor.grc.modules.privacy.domain.DpiaResponseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class DpiaQuestionnaireDtos {

    private DpiaQuestionnaireDtos() {
    }

    public record CreateQuestionnaireRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description) {
    }

    public record UpdateQuestionnaireRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            Boolean active) {
    }

    public record UpsertQuestionRequest(
            @NotBlank @Size(max = 16) String code,
            @NotBlank @Size(max = 255) String sectionName,
            @NotBlank @Size(max = 255) String principle,
            @NotBlank String questionText,
            String guidance,
            @NotNull DpiaResponseType responseType,
            List<String> options,
            Integer sortOrder,
            Boolean required) {
    }

    public record QuestionResponse(
            Long id,
            Long questionnaireId,
            String code,
            String sectionName,
            String principle,
            String questionText,
            String guidance,
            DpiaResponseType responseType,
            List<String> options,
            int sortOrder,
            boolean required) {

        public static QuestionResponse from(DpiaQuestionnaireQuestion q, List<String> options) {
            return new QuestionResponse(q.getId(), q.getQuestionnaireId(), q.getCode(), q.getSectionName(),
                    q.getPrinciple(), q.getQuestionText(), q.getGuidance(), q.getResponseType(),
                    options, q.getSortOrder(), q.isRequired());
        }
    }

    public record QuestionnaireResponse(
            Long id,
            String name,
            String description,
            boolean systemDefault,
            boolean active,
            int questionCount,
            Long createdByUserId,
            Instant createdAt,
            Instant updatedAt,
            List<QuestionResponse> questions) {

        public static QuestionnaireResponse summary(DpiaQuestionnaire q, int questionCount) {
            return new QuestionnaireResponse(q.getId(), q.getName(), q.getDescription(), q.isSystemDefault(),
                    q.isActive(), questionCount, q.getCreatedByUserId(), q.getCreatedAt(), q.getUpdatedAt(), null);
        }

        public static QuestionnaireResponse detail(DpiaQuestionnaire q, List<QuestionResponse> questions) {
            return new QuestionnaireResponse(q.getId(), q.getName(), q.getDescription(), q.isSystemDefault(),
                    q.isActive(), questions.size(), q.getCreatedByUserId(), q.getCreatedAt(), q.getUpdatedAt(),
                    questions);
        }
    }
}
