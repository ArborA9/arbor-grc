package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistAssessment;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistConclusion;

import java.time.LocalDate;

public record UpdateChecklistItemRequest(
        ChecklistAssessment assessment,
        String notes,
        Boolean inScope,
        String evidenceExamined,
        String personsInterviewed,
        LocalDate reviewedDate,
        ChecklistConclusion conclusion,
        String findingDescription,
        String clauseRef,
        String section,
        String title,
        String testFocus,
        String soaApplicability,
        String evidenceModule
) {
}
