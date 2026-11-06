package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItemSubmission;
import com.a9.arbor.grc.modules.gap.service.SamaMaturityGates;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public record GapItemSubmissionResponse(
        Long id,
        int submissionNumber,
        GapImplementationStatus implementationStatus,
        Integer maturity,
        List<Boolean> maturityGates,
        String observations,
        String gapDescription,
        String contributorComments,
        Integer score,
        Long submittedByUserId,
        Instant submittedAt
) {
    public static GapItemSubmissionResponse from(GapItemSubmission s) {
        Boolean[] gates = SamaMaturityGates.fromJson(s.getMaturityGatesJson());
        boolean any = s.getMaturityGatesJson() != null;
        return new GapItemSubmissionResponse(
                s.getId(),
                s.getSubmissionNumber(),
                s.getImplementationStatus(),
                s.getMaturity(),
                any ? Arrays.asList(gates) : null,
                s.getObservations(),
                s.getGapDescription(),
                s.getContributorComments(),
                s.getScore(),
                s.getSubmittedByUserId(),
                s.getSubmittedAt());
    }
}
