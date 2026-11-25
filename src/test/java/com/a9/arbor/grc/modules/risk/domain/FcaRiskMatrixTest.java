package com.a9.arbor.grc.modules.risk.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FcaRiskMatrixTest {

    @Test
    void lookupMatchesApprovedFcaGrid() {
        RiskLevel[][] expected = {
                {RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH, RiskLevel.VERY_HIGH},
                {RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH},
                {RiskLevel.LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.MEDIUM, RiskLevel.HIGH},
                {RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.MEDIUM},
                {RiskLevel.VERY_LOW, RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.LOW, RiskLevel.MEDIUM},
        };
        RiskLevel[] likeli = {
                RiskLevel.VERY_HIGH, RiskLevel.HIGH, RiskLevel.MEDIUM, RiskLevel.LOW, RiskLevel.VERY_LOW
        };
        RiskLevel[] impacts = {
                RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH
        };
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                assertEquals(expected[r][c], FcaRiskMatrix.lookup(likeli[r], impacts[c]),
                        likeli[r] + " × " + impacts[c]);
            }
        }
    }

    @Test
    void formerProductBandMismatchesAreOneBandHigher() {
        // Unlikely × High: product 8 was Moderate (LOW); FCA High (MEDIUM)
        assertEquals(RiskLevel.MEDIUM, FcaRiskMatrix.lookup(RiskLevel.LOW, RiskLevel.HIGH));
        // Extremely Unlikely × Very High: product 5 was Moderate; FCA High
        assertEquals(RiskLevel.MEDIUM, FcaRiskMatrix.lookup(RiskLevel.VERY_LOW, RiskLevel.VERY_HIGH));
        // Almost Certain × Very Low: product 5 was Moderate; FCA High
        assertEquals(RiskLevel.MEDIUM, FcaRiskMatrix.lookup(RiskLevel.VERY_HIGH, RiskLevel.VERY_LOW));
        // Likely × Low: product 8 was Moderate; FCA High
        assertEquals(RiskLevel.MEDIUM, FcaRiskMatrix.lookup(RiskLevel.HIGH, RiskLevel.LOW));
    }

    @Test
    void appetiteAndAutoCloseUseRatingOrdinal() {
        assertTrue(FcaRiskMatrix.withinAppetite(RiskLevel.LOW, RiskLevel.LOW));
        assertFalse(FcaRiskMatrix.withinAppetite(RiskLevel.MEDIUM, RiskLevel.LOW));
        assertTrue(FcaRiskMatrix.autoClosable(RiskLevel.VERY_LOW));
        assertFalse(FcaRiskMatrix.autoClosable(RiskLevel.LOW));
        assertEquals(EvaluationDecision.AUTO_CLOSE,
                FcaRiskMatrix.evaluation(RiskLevel.VERY_LOW, RiskLevel.LOW));
        assertEquals(EvaluationDecision.MONITOR,
                FcaRiskMatrix.evaluation(RiskLevel.LOW, RiskLevel.LOW));
        assertEquals(EvaluationDecision.TREATMENT_REQUIRED,
                FcaRiskMatrix.evaluation(RiskLevel.HIGH, RiskLevel.LOW));
    }
}
