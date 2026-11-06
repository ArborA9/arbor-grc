package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.RiskTemplate;

public record RiskTemplateResponse(
        long id,
        long frameworkId,
        Long frameworkNodeId,
        String sourceRiskId,
        String title,
        String description,
        String category,
        String typicalThreats,
        String suggestedProbability,
        String suggestedImpact,
        String suggestedControlEffectiveness,
        String treatmentOption,
        String treatmentRecommendation,
        int displayOrder,
        int versionNumber
) {
    public static RiskTemplateResponse from(RiskTemplate t) {
        return new RiskTemplateResponse(
                t.getId(),
                t.getFrameworkId(),
                t.getFrameworkNodeId(),
                t.getSourceRiskId(),
                t.getTitle(),
                t.getDescription(),
                t.getCategory(),
                t.getTypicalThreats(),
                t.getSuggestedProbability(),
                t.getSuggestedImpact(),
                t.getSuggestedControlEffectiveness(),
                t.getTreatmentOption(),
                t.getTreatmentRecommendation(),
                t.getDisplayOrder(),
                t.getVersionNumber());
    }
}
