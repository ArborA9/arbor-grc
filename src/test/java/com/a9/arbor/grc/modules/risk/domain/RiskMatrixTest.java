package com.a9.arbor.grc.modules.risk.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskMatrixTest {

    @Test
    void residualCeFollowsClientFiveByFive() {
        // Rows: CE Very High → Very Low; cols: Inherent Very Low → Very High (§13.3).
        assertEquals(RiskLevel.VERY_LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_HIGH, RiskLevel.VERY_LOW));
        assertEquals(RiskLevel.VERY_LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_HIGH, RiskLevel.LOW));
        assertEquals(RiskLevel.LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_HIGH, RiskLevel.MEDIUM));
        assertEquals(RiskLevel.LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_HIGH, RiskLevel.HIGH));
        assertEquals(RiskLevel.LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_HIGH, RiskLevel.VERY_HIGH));

        assertEquals(RiskLevel.VERY_LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.HIGH, RiskLevel.VERY_LOW));
        assertEquals(RiskLevel.VERY_LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.HIGH, RiskLevel.LOW));
        assertEquals(RiskLevel.LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.HIGH, RiskLevel.MEDIUM));
        assertEquals(RiskLevel.MEDIUM, RiskMatrix.residualFromControlEffectiveness(RiskLevel.HIGH, RiskLevel.HIGH));
        assertEquals(RiskLevel.MEDIUM, RiskMatrix.residualFromControlEffectiveness(RiskLevel.HIGH, RiskLevel.VERY_HIGH));

        assertEquals(RiskLevel.VERY_LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.MEDIUM, RiskLevel.VERY_LOW));
        assertEquals(RiskLevel.VERY_LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.MEDIUM, RiskLevel.LOW));
        assertEquals(RiskLevel.LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.MEDIUM, RiskLevel.MEDIUM));
        assertEquals(RiskLevel.MEDIUM, RiskMatrix.residualFromControlEffectiveness(RiskLevel.MEDIUM, RiskLevel.HIGH));
        assertEquals(RiskLevel.HIGH, RiskMatrix.residualFromControlEffectiveness(RiskLevel.MEDIUM, RiskLevel.VERY_HIGH));

        assertEquals(RiskLevel.VERY_LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.LOW, RiskLevel.VERY_LOW));
        assertEquals(RiskLevel.LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.LOW, RiskLevel.LOW));
        assertEquals(RiskLevel.MEDIUM, RiskMatrix.residualFromControlEffectiveness(RiskLevel.LOW, RiskLevel.MEDIUM));
        assertEquals(RiskLevel.HIGH, RiskMatrix.residualFromControlEffectiveness(RiskLevel.LOW, RiskLevel.HIGH));
        assertEquals(RiskLevel.VERY_HIGH, RiskMatrix.residualFromControlEffectiveness(RiskLevel.LOW, RiskLevel.VERY_HIGH));

        assertEquals(RiskLevel.VERY_LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_LOW, RiskLevel.VERY_LOW));
        assertEquals(RiskLevel.LOW, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_LOW, RiskLevel.LOW));
        assertEquals(RiskLevel.MEDIUM, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_LOW, RiskLevel.MEDIUM));
        assertEquals(RiskLevel.HIGH, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_LOW, RiskLevel.HIGH));
        assertEquals(RiskLevel.VERY_HIGH, RiskMatrix.residualFromControlEffectiveness(RiskLevel.VERY_LOW, RiskLevel.VERY_HIGH));
    }

    @Test
    void autoClosableWhenResidualVeryLowOrLow() {
        assertEquals(true, RiskMatrix.autoClosable(RiskLevel.VERY_LOW));
        assertEquals(true, RiskMatrix.autoClosable(RiskLevel.LOW));
        assertEquals(false, RiskMatrix.autoClosable(RiskLevel.MEDIUM));
        assertEquals(false, RiskMatrix.autoClosable(RiskLevel.HIGH));
        assertEquals(false, RiskMatrix.autoClosable(RiskLevel.VERY_HIGH));
    }
}
