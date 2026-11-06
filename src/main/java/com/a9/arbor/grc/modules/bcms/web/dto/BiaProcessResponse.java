package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.BiaImpactCell;
import com.a9.arbor.grc.modules.bcms.domain.BiaMor;
import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.bcms.domain.BiaReviewOutcome;
import com.a9.arbor.grc.modules.bcms.pack.BiaImpactEngine;
import com.a9.arbor.grc.modules.bcms.pack.BiaStructuredJson;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BiaProcessResponse(
        Long id,
        Long cycleId,
        int sortOrder,
        String processCode,
        String processName,
        String description,
        String flowTrigger,
        String flowSteps,
        String flowOutput,
        String thirdPartyActivities,
        String supplierName,
        Long ownerOrgUnitId,
        Long ownerUserId,
        String frequency,
        String peakPeriod,
        String depStaff,
        String depPremises,
        String depIt,
        String depEquipment,
        String depRecords,
        String depThirdParty,
        String relatedRiskCode,
        String championNotes,
        List<BiaPersonRef> depStaffPeople,
        List<BiaAssetDependency> depAssets,
        Integer peakScore,
        Integer criticalityTier,
        String categoryLabel,
        Integer rtoHours,
        Integer maoHours,
        Integer recoveryPriority,
        Integer criticalRank,
        boolean morRequired,
        String rtoCaption,
        boolean completenessOk,
        String completenessLabel,
        List<String> completenessMissing,
        BiaReviewOutcome reviewOutcome,
        String reviewOutcomeLabel,
        Long submittedByUserId,
        Instant submittedAt,
        Long reviewerUserId,
        String reviewerName,
        LocalDate reviewDate,
        String reviewComment,
        Long createdByUserId,
        boolean createdByAdmin,
        boolean headVerified,
        List<BiaImpactCellResponse> impact,
        BiaMorResponse mor
) {
    public static BiaProcessResponse from(
            BiaProcess p,
            List<BiaImpactCell> cells,
            BiaMor mor,
            List<String> missing) {
        List<BiaImpactCellResponse> impact = cells.stream()
                .map(c -> new BiaImpactCellResponse(
                        c.getCategory().name(), c.getBand().name(), c.getRating(), c.getRemarks()))
                .toList();
        BiaMorResponse morDto = mor == null ? null : BiaMorResponse.from(mor);
        BiaReviewOutcome outcome = p.getReviewOutcome();
        return new BiaProcessResponse(
                p.getId(), p.getCycleId(), p.getSortOrder(), p.getProcessCode(), p.getProcessName(),
                p.getDescription(), p.getFlowTrigger(), p.getFlowSteps(), p.getFlowOutput(),
                p.getThirdPartyActivities(), p.getSupplierName(), p.getOwnerOrgUnitId(), p.getOwnerUserId(),
                p.getFrequency(),
                p.getPeakPeriod(), p.getDepStaff(), p.getDepPremises(), p.getDepIt(), p.getDepEquipment(),
                p.getDepRecords(), p.getDepThirdParty(), p.getRelatedRiskCode(), p.getChampionNotes(),
                BiaStructuredJson.people(p.getDepStaff()), BiaStructuredJson.assets(p.getDepIt()),
                p.getPeakScore(), p.getCriticalityTier(), p.getCategoryLabel(), p.getRtoHours(), p.getMaoHours(),
                p.getRecoveryPriority(), p.getCriticalRank(), p.isMorRequired(), p.getRtoCaption(),
                p.isCompletenessOk(), completenessLabel(p.getCompletenessLabel()), missing == null ? List.of() : missing,
                outcome, outcome != null ? outcome.display() : null,
                p.getSubmittedByUserId(), p.getSubmittedAt(), p.getReviewerUserId(), p.getReviewerName(),
                p.getReviewDate(), p.getReviewComment(),
                p.getCreatedByUserId(), p.isCreatedByAdmin(),
                p.isHeadVerified(),
                impact, morDto);
    }

    /** Older rows stored the longer label. The checklist flag is unchanged. */
    private static String completenessLabel(String stored) {
        if ("Complete - ready for review".equals(stored)) {
            return BiaImpactEngine.COMPLETE_READY;
        }
        return stored;
    }
}
