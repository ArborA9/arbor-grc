package com.a9.arbor.grc.modules.capa.web.dto;

import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.domain.CapaPriority;
import com.a9.arbor.grc.modules.capa.domain.CapaRegister;
import com.a9.arbor.grc.modules.capa.domain.CapaSourceType;
import com.a9.arbor.grc.modules.capa.domain.CapaStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * CAPA response DTO - fields ordered per Excel Audit_Module_MVP_v2.xlsx CAPA Tracker (A-T)
 */
public record CapaResponse(
        // A: CAPA ID
        Long id,
        // B: Finding ID (source reference)
        String sourceFindingDisplayCode,
        // C: Finding Title
        String title,
        // D: Classification (severity)
        String severity,
        // E: Clause Ref
        String controlRef,
        // F: Date Raised
        Instant createdAt,
        // G: Correction (immediate fix)
        String correction,
        // H: Root Cause
        String rootCause,
        // I: Corrective Action Plan
        String correctiveAction,
        // J: Date Assigned
        Instant assignedAt,
        // K: Agreed Target Date
        LocalDate targetDate,
        // L: Assigned Contributor (owner)
        Long ownerUserId,
        // M: Contributor Department
        String contributorDepartment,
        // N: Evidence Required
        String evidenceRequired,
        // O: Response Description (Contributor)
        String responseDescription,
        // P: Upload Evidence note (Contributor)
        String contributorEvidenceNote,
        // Q: Review Remarks (Admin)
        String reviewRemarks,
        // R: Evidence of Completion (Admin)
        String evidenceOfCompletion,
        // S: Verification Date (Admin)
        LocalDate verificationDate,
        // T: Verification Result (Admin)
        String verificationResult,
        // Additional fields not in Excel tracker
        CapaRegister registerCode,
        CapaSourceType sourceType,
        Long sourceRecordId,
        String description,
        String recommendedAction,
        CapaPriority priority,
        CapaStatus status,
        String preventiveAction,
        String progressNotes,
        String rejectionReason,
        Long workTaskId,
        Instant closedAt,
        Instant updatedAt,
        Long sourceAuditId,
        String sourceAuditName,
        String conductedBy,
        Long capaRegisterId,
        String sourceOther,
        List<CapaReturnHistoryItem> returnHistory
) {
    public static CapaResponse from(Capa c) {
        return from(c, null, null, null, List.of());
    }

    public static CapaResponse from(Capa c,
                                    String sourceFindingDisplayCode,
                                    Long sourceAuditId,
                                    String sourceAuditName) {
        return from(c, sourceFindingDisplayCode, sourceAuditId, sourceAuditName, List.of());
    }

    public static CapaResponse from(Capa c,
                                    String sourceFindingDisplayCode,
                                    Long sourceAuditId,
                                    String sourceAuditName,
                                    List<CapaReturnHistoryItem> returnHistory) {
        Long auditId = sourceAuditId != null ? sourceAuditId : c.getAuditId();
        List<CapaReturnHistoryItem> history =
                returnHistory == null ? List.of() : List.copyOf(returnHistory);
        String findingCode = sourceFindingDisplayCode;
        if ((findingCode == null || findingCode.isBlank()) && c.getExternalFindingCode() != null) {
            findingCode = c.getExternalFindingCode();
        }
        return new CapaResponse(
                // Excel A-T order
                c.getId(),
                findingCode,
                c.getTitle(),
                c.getSeverity(),
                c.getControlRef(),
                c.getCreatedAt(),
                c.getCorrection(),
                c.getRootCause(),
                c.getCorrectiveAction(),
                c.getAssignedAt(),
                c.getTargetDate(),
                c.getOwnerUserId(),
                c.getContributorDepartment(),
                c.getEvidenceRequired(),
                c.getResponseDescription(),
                c.getContributorEvidenceNote(),
                c.getReviewRemarks(),
                c.getEvidenceOfCompletion(),
                c.getVerificationDate(),
                c.getVerificationResult(),
                // Additional fields
                c.getRegisterCode(),
                c.getSourceType(),
                c.getSourceRecordId(),
                c.getDescription(),
                c.getRecommendedAction(),
                c.getPriority(),
                c.getStatus(),
                c.getPreventiveAction(),
                c.getProgressNotes(),
                c.getRejectionReason(),
                c.getWorkTaskId(),
                c.getClosedAt(),
                c.getUpdatedAt(),
                auditId,
                sourceAuditName,
                c.getConductedBy(),
                c.getCapaRegisterId(),
                c.getSourceOther(),
                history
        );
    }
}
