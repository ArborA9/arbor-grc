package com.a9.arbor.grc.modules.gap.web.dto;

import com.a9.arbor.grc.catalog.E2eCatalogMarker;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.domain.GapItemReviewStatus;
import com.a9.arbor.grc.modules.gap.domain.GapQuestionAssignmentMode;
import com.a9.arbor.grc.modules.gap.service.GapQuestionAssignmentSupport;
import com.a9.arbor.grc.modules.gap.service.SamaMaturityGates;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public record GapItemResponse(
        Long id,
        Long frameworkNodeId,
        String nodeCode,
        String title,
        String definitionText,
        String priority,
        GapApplicability applicability,
        boolean applicabilityOverride,
        String overrideJustification,
        GapImplementationStatus implementationStatus,
        Integer maturity,
        /** SAMA: five gate answers (true/false/null). Null for non-SAMA. */
        List<Boolean> maturityGates,
        String observations,
        String gapDescription,
        String contributorComments,
        Integer score,
        GapItemReviewStatus reviewStatus,
        GapImplementationStatus finalImplementationStatus,
        Integer finalMaturity,
        String adminReviewNotes,
        String sendBackReason,
        Instant contributorSubmittedAt,
        Instant adminReviewedAt,
        Long adminReviewedByUserId,
        Long assignedToUserId,
        Long workTaskId,
        GapQuestionAssignmentMode questionAssignmentMode,
        List<Long> assignedFrameworkQuestionIds,
        List<Long> assignedCustomQuestionIds,
        Instant updatedAt,
        /** Assignment due date from the contributor task. Null on list rows. */
        LocalDate dueDate,
        /** Catalog clause (or top ancestor) code. Null when the framework has no parent node. */
        String domainCode,
        /** Catalog clause title. Null when the parent has no title of its own. */
        String domainLabel,
        /** What it means, from the framework row. Tenant admin can edit it on the control. */
        String whatItMeans,
        /** Evidence guidance from the framework row. Tenant admin can edit it on the control. */
        String evidenceGuidance,
        /** False while the admin decision is a draft the contributor cannot see. */
        boolean releasedToContributor,
        /** Admin mark for later work. Independent of workflow. */
        boolean flaggedForLater
) {
    public static GapItemResponse from(GapItem item) {
        return from(item, null, null, null);
    }

    public static GapItemResponse from(GapItem item, String definitionText, String priority) {
        return from(item, definitionText, priority, null);
    }

    public static GapItemResponse from(GapItem item, String definitionText, String priority, LocalDate dueDate) {
        return from(item, definitionText, priority, dueDate, null, null);
    }

    public static GapItemResponse from(
            GapItem item,
            String definitionText,
            String priority,
            LocalDate dueDate,
            String domainCode,
            String domainLabel) {
        Boolean[] gates = SamaMaturityGates.fromJson(item.getMaturityGatesJson());
        boolean anyGate = false;
        for (Boolean g : gates) {
            if (g != null) {
                anyGate = true;
                break;
            }
        }
        GapQuestionAssignmentMode mode = item.getQuestionAssignmentMode() != null
                ? item.getQuestionAssignmentMode()
                : GapQuestionAssignmentMode.ALL;
        List<Long> fwIds = mode == GapQuestionAssignmentMode.SELECTED
                ? GapQuestionAssignmentSupport.parseIds(item.getAssignedFrameworkQuestionIdsJson())
                : List.of();
        List<Long> cuIds = mode == GapQuestionAssignmentMode.SELECTED
                ? GapQuestionAssignmentSupport.parseIds(item.getAssignedCustomQuestionIdsJson())
                : List.of();
        return new GapItemResponse(
                item.getId(),
                item.getFrameworkNodeId(),
                item.getNodeCode(),
                E2eCatalogMarker.strip(item.getTitle()),
                E2eCatalogMarker.strip(definitionText),
                priority,
                item.getApplicability(),
                item.isApplicabilityOverride(),
                item.getOverrideJustification(),
                item.getImplementationStatus(),
                item.getMaturity(),
                anyGate || item.getMaturityGatesJson() != null ? Arrays.asList(gates) : null,
                item.getObservations(),
                item.getGapDescription(),
                item.getContributorComments(),
                item.getScore(),
                item.getReviewStatus(),
                item.getFinalImplementationStatus(),
                item.getFinalMaturity(),
                item.getAdminReviewNotes(),
                item.getSendBackReason(),
                item.getContributorSubmittedAt(),
                item.getAdminReviewedAt(),
                item.getAdminReviewedByUserId(),
                item.getAssignedToUserId(),
                item.getWorkTaskId(),
                mode,
                fwIds,
                cuIds,
                item.getUpdatedAt(),
                dueDate,
                domainCode,
                domainLabel,
                item.getWhatItMeans(),
                item.getEvidenceGuidance(),
                item.isReleasedToContributor(),
                item.isFlaggedForLater());
    }

    /** Show framework text when this control has not stored its own copy yet. */
    public GapItemResponse withMeaning(String whatItMeans, String evidenceGuidance) {
        if (java.util.Objects.equals(this.whatItMeans, whatItMeans)
                && java.util.Objects.equals(this.evidenceGuidance, evidenceGuidance)) {
            return this;
        }
        return new GapItemResponse(
                id,
                frameworkNodeId,
                nodeCode,
                title,
                definitionText,
                priority,
                applicability,
                applicabilityOverride,
                overrideJustification,
                implementationStatus,
                maturity,
                maturityGates,
                observations,
                gapDescription,
                contributorComments,
                score,
                reviewStatus,
                finalImplementationStatus,
                finalMaturity,
                adminReviewNotes,
                sendBackReason,
                contributorSubmittedAt,
                adminReviewedAt,
                adminReviewedByUserId,
                assignedToUserId,
                workTaskId,
                questionAssignmentMode,
                assignedFrameworkQuestionIds,
                assignedCustomQuestionIds,
                updatedAt,
                dueDate,
                domainCode,
                domainLabel,
                whatItMeans,
                evidenceGuidance,
                releasedToContributor,
                flaggedForLater);
    }

    /** Register / tree row: no catalog definition or assignment payloads. */
    public static GapItemResponse fromList(GapItem item, String priority) {
        return fromList(item, priority, null, null);
    }

    public static GapItemResponse fromList(GapItem item, String priority, String domainCode, String domainLabel) {
        GapQuestionAssignmentMode mode = item.getQuestionAssignmentMode() != null
                ? item.getQuestionAssignmentMode()
                : GapQuestionAssignmentMode.ALL;
        return new GapItemResponse(
                item.getId(),
                item.getFrameworkNodeId(),
                item.getNodeCode(),
                E2eCatalogMarker.strip(item.getTitle()),
                null,
                priority,
                item.getApplicability(),
                item.isApplicabilityOverride(),
                null,
                item.getImplementationStatus(),
                item.getMaturity(),
                null,
                item.getObservations(),
                null,
                null,
                item.getScore(),
                item.getReviewStatus(),
                item.getFinalImplementationStatus(),
                item.getFinalMaturity(),
                null,
                null,
                item.getContributorSubmittedAt(),
                item.getAdminReviewedAt(),
                item.getAdminReviewedByUserId(),
                item.getAssignedToUserId(),
                item.getWorkTaskId(),
                mode,
                List.of(),
                List.of(),
                item.getUpdatedAt(),
                item.getPendingDueDate(),
                domainCode,
                domainLabel,
                item.getWhatItMeans(),
                item.getEvidenceGuidance(),
                item.isReleasedToContributor(),
                item.isFlaggedForLater());
    }
}
