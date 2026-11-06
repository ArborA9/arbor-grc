package com.a9.arbor.grc.modules.capa.web.dto;

import java.time.LocalDate;

/**
 * Admin update request for CAPA Tracker - Excel columns E, G-N, Q-T
 */
public record UpdateCapaTrackerRequest(
        // E: Clause Ref (manual entry)
        String clauseRef,
        // G: Correction (immediate fix)
        String correction,
        // H: Root Cause
        String rootCause,
        // I: Corrective Action Plan
        String correctiveActionPlan,
        // K: Agreed Target Date
        LocalDate agreedTargetDate,
        // L: Assigned Contributor (user ID)
        Long assignedContributorUserId,
        // M: Contributor Department
        String contributorDepartment,
        // N: Evidence Required
        String evidenceRequired,
        // Q: Review Remarks (Admin)
        String reviewRemarks,
        // R: Evidence of Completion (Admin)
        String evidenceOfCompletion,
        // S: Verification Date (Admin)
        LocalDate verificationDate,
        // T: Verification Result (Admin)
        String verificationResult
) {
}
