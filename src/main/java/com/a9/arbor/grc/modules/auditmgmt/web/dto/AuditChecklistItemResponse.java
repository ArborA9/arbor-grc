package com.a9.arbor.grc.modules.auditmgmt.web.dto;

import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistAssessment;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistConclusion;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;

import java.time.Instant;
import java.time.LocalDate;

public record AuditChecklistItemResponse(
        Long id,
        Long checklistId,
        Long frameworkNodeId,
        String nodeCode,
        String title,
        String section,
        String testFocus,
        String soaApplicability,
        String evidenceModule,
        boolean libraryLocked,
        boolean inScope,
        ChecklistAssessment assessment,
        String notes,
        String evidenceExamined,
        String personsInterviewed,
        LocalDate reviewedDate,
        ChecklistConclusion conclusion,
        String findingDescription,
        Long findingId,
        String findingDisplayCode,
        Instant updatedAt
) {
    public static AuditChecklistItemResponse from(GrcAuditChecklistItem i) {
        return from(i, true, null);
    }

    public static AuditChecklistItemResponse from(GrcAuditChecklistItem i, boolean showWorkingNotes) {
        return from(i, showWorkingNotes, null);
    }

    public static AuditChecklistItemResponse from(GrcAuditChecklistItem i, boolean showWorkingNotes,
                                                 String findingDisplayCode) {
        return new AuditChecklistItemResponse(
                i.getId(), i.getChecklistId(), i.getFrameworkNodeId(), i.getNodeCode(), i.getTitle(),
                i.getSection(), i.getTestFocus(), i.getSoaApplicability(), i.getEvidenceModule(),
                i.isLibraryLocked(), i.isInScope(), i.getAssessment(),
                showWorkingNotes ? i.getNotes() : null,
                showWorkingNotes ? i.getEvidenceExamined() : null,
                i.getPersonsInterviewed(), i.getReviewedDate(), i.getConclusion(),
                showWorkingNotes ? i.getFindingDescription() : null,
                i.getFindingId(), findingDisplayCode, i.getUpdatedAt());
    }
}
