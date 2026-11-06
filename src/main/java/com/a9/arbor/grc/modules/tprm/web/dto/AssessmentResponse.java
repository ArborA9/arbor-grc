package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.TprmAnswer;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessment;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessmentItem;
import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.domain.TprmVerdict;
import com.a9.arbor.grc.modules.tprm.domain.AssessmentStatus;
import com.a9.arbor.grc.modules.tprm.pack.TprmControlDisplay;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AssessmentResponse(
        Long id,
        Long vendorId,
        String vendorName,
        String displayId,
        String title,
        String description,
        AssessmentStatus status,
        Long questionnaireId,
        String questionnaireName,
        String snapshotVendorType,
        String snapshotTier,
        String snapshotCriticality,
        BigDecimal snapshotTarget,
        Integer inherentScore,
        Integer assuranceCredit,
        Integer adjustedScore,
        BigDecimal posturePercent,
        Boolean compliant,
        LocalDate dueDate,
        Instant expiresAt,
        Instant sentAt,
        Instant startedAt,
        Instant submittedAt,
        Instant reviewedAt,
        boolean raiseErmPrompted,
        Long linkedErmRiskId,
        Long createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        List<AssessmentItemView> items,
        String accessToken,
        String otpCode,
        /** Vendor portal only: true when this session is the Vendor primary SPOC. */
        Boolean canSubmit,
        /** Admin preview invites while email is off (portal link per invitee). */
        List<PortalInviteView> portalInvites
) {
    public record PortalInviteView(String email, String accessToken, String otpCode) {
    }

    public record AssessmentItemView(
            Long id,
            Long controlId,
            String controlCode,
            String domain,
            String requirement,
            String description,
            String requiredEvidence,
            boolean evidenceMandatory,
            int weight,
            String weightLabel,
            boolean mandatory,
            String module,
            int sortOrder,
            TprmAnswer vendorAnswer,
            String justification,
            Long evidenceId,
            String evidenceFilename,
            String evidenceRef,
            TprmVerdict reviewVerdict,
            String reviewComment,
            boolean returned,
            /** True when Admin returned this control and the vendor has not changed answer/evidence yet. */
            boolean unchangedSinceReturn,
            List<EvidenceFileView> evidenceFiles
    ) {
        public record EvidenceFileView(Long id, String filename) {
        }

        public static AssessmentItemView from(TprmAssessmentItem item, TprmControl control,
                                              String evidenceFilename,
                                              List<EvidenceFileView> evidenceFiles) {
            boolean evidenceMandatory = item.getEvidenceMandatory() != null
                    ? item.getEvidenceMandatory()
                    : control.isMandatory();
            List<EvidenceFileView> files = evidenceFiles != null ? List.copyOf(evidenceFiles) : List.of();
            String primaryName = evidenceFilename;
            if ((primaryName == null || primaryName.isBlank()) && !files.isEmpty()) {
                primaryName = files.getFirst().filename();
            }
            Long primaryId = item.getEvidenceId();
            if (primaryId == null && !files.isEmpty()) {
                primaryId = files.getFirst().id();
            }
            String currentFp = answerFingerprint(item, files);
            boolean unchangedSinceReturn = item.isReturned()
                    && item.getReturnFingerprint() != null
                    && item.getReturnFingerprint().equals(currentFp);
            return new AssessmentItemView(
                    item.getId(), control.getId(),
                    TprmControlDisplay.code(item, control),
                    TprmControlDisplay.domain(item, control),
                    TprmControlDisplay.requirement(item, control),
                    TprmControlDisplay.first(item.getRequirement(), control.getDescription()),
                    TprmControlDisplay.requiredEvidence(item, control),
                    evidenceMandatory, control.getWeight(), control.getWeightLabel(), control.isMandatory(),
                    control.getModule(), item.getSortOrder(), item.getVendorAnswer(), item.getJustification(),
                    primaryId, primaryName, item.getEvidenceRef(), item.getReviewVerdict(),
                    item.getReviewComment(), item.isReturned(), unchangedSinceReturn, files);
        }

        /** Stable content key for return / resubmit comparison. */
        public static String answerFingerprint(TprmAssessmentItem item, List<EvidenceFileView> files) {
            String answer = item.getVendorAnswer() == null ? "" : item.getVendorAnswer().name();
            String justification = item.getJustification() == null ? "" : item.getJustification().trim();
            String ref = item.getEvidenceRef() == null ? "" : item.getEvidenceRef().trim();
            StringBuilder ids = new StringBuilder();
            if (files != null) {
                files.stream().map(EvidenceFileView::id).sorted().forEach(id -> {
                    if (!ids.isEmpty()) {
                        ids.append(',');
                    }
                    ids.append(id);
                });
            }
            return answer + '\u001f' + justification + '\u001f' + ref + '\u001f' + ids;
        }
    }

    public static AssessmentResponse from(TprmAssessment a, String vendorName, List<AssessmentItemView> items,
                                          String accessToken, String otpCode) {
        return from(a, vendorName, items, accessToken, otpCode, null, List.of());
    }

    public static AssessmentResponse from(TprmAssessment a, String vendorName, List<AssessmentItemView> items,
                                          String accessToken, String otpCode, Boolean canSubmit,
                                          List<PortalInviteView> portalInvites) {
        return from(a, vendorName, null, items, accessToken, otpCode, canSubmit, portalInvites);
    }

    public static AssessmentResponse from(TprmAssessment a, String vendorName, String questionnaireName,
                                          List<AssessmentItemView> items, String accessToken, String otpCode,
                                          Boolean canSubmit, List<PortalInviteView> portalInvites) {
        return new AssessmentResponse(
                a.getId(), a.getVendorId(), vendorName, a.getDisplayId(), a.getTitle(), a.getDescription(), a.getStatus(),
                a.getQuestionnaireId(), questionnaireName,
                a.getSnapshotVendorType(), a.getSnapshotTier(), a.getSnapshotCriticality(), a.getSnapshotTarget(),
                a.getInherentScore(), a.getAssuranceCredit(), a.getAdjustedScore(), a.getPosturePercent(),
                a.getCompliant(), a.getDueDate(), a.getExpiresAt(), a.getSentAt(), a.getStartedAt(),
                a.getSubmittedAt(), a.getReviewedAt(), a.isRaiseErmPrompted(), a.getLinkedErmRiskId(),
                a.getCreatedByUserId(), a.getCreatedAt(), a.getUpdatedAt(), items, accessToken, otpCode, canSubmit,
                portalInvites != null ? portalInvites : List.of());
    }
}
