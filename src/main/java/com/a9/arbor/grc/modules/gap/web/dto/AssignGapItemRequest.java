package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record AssignGapItemRequest(
        @NotNull Long userId,
        /** Optional. Admin sets scored status on review / send-back, not as part of assign. */
        GapImplementationStatus implementationStatus,
        LocalDate dueDate,
        /**
         * When true or null: Contributor gets all questions (legacy).
         * When false: only {@code frameworkQuestionIds} / {@code customQuestionIds} (may be empty).
         */
        Boolean assignAllQuestions,
        List<Long> frameworkQuestionIds,
        List<Long> customQuestionIds,
        /**
         * Null or true: create the contributor task now (existing clients).
         * False: store the contributor and due date, and wait for Send.
         */
        Boolean release
) {
}
