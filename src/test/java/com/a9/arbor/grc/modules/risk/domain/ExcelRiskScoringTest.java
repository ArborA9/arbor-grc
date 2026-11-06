package com.a9.arbor.grc.modules.risk.domain;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcelRiskScoringTest {

    @Test
    void maxProductFollowsGrid() {
        assertEquals(9, ExcelRiskScoring.maxProduct(MatrixGridSize.THREE));
        assertEquals(16, ExcelRiskScoring.maxProduct(MatrixGridSize.FOUR));
        assertEquals(25, ExcelRiskScoring.maxProduct(MatrixGridSize.FIVE));
    }

    @Test
    void requireProductScoreAllowsNullAndBounds() {
        assertDoesNotThrow(() -> ExcelRiskScoring.requireProductScore(null, MatrixGridSize.FIVE, "Residual Risk Score"));
        assertDoesNotThrow(() -> ExcelRiskScoring.requireProductScore(1, MatrixGridSize.THREE, "Residual Risk Score"));
        assertDoesNotThrow(() -> ExcelRiskScoring.requireProductScore(9, MatrixGridSize.THREE, "Residual Risk Score"));
        assertThrows(ResponseStatusException.class,
                () -> ExcelRiskScoring.requireProductScore(0, MatrixGridSize.FIVE, "Residual Risk Score"));
        assertThrows(ResponseStatusException.class,
                () -> ExcelRiskScoring.requireProductScore(10, MatrixGridSize.THREE, "Residual Risk Score"));
        assertThrows(ResponseStatusException.class,
                () -> ExcelRiskScoring.requireProductScore(99, MatrixGridSize.FIVE, "Residual Risk Score"));
    }

    @Test
    void fiveByFiveBandsAndAppetite() {
        assertEquals("VERY_LOW", ExcelRiskScoring.rating(MatrixGridSize.FIVE, 4));
        assertEquals("LOW", ExcelRiskScoring.rating(MatrixGridSize.FIVE, 5));
        assertEquals("LOW", ExcelRiskScoring.rating(MatrixGridSize.FIVE, 8));
        assertEquals("MEDIUM", ExcelRiskScoring.rating(MatrixGridSize.FIVE, 9));
        assertEquals("MEDIUM", ExcelRiskScoring.rating(MatrixGridSize.FIVE, 12));
        assertEquals("HIGH", ExcelRiskScoring.rating(MatrixGridSize.FIVE, 13));
        assertEquals("HIGH", ExcelRiskScoring.rating(MatrixGridSize.FIVE, 19));
        assertEquals("VERY_HIGH", ExcelRiskScoring.rating(MatrixGridSize.FIVE, 20));
        assertEquals(8, ExcelRiskScoring.appetiteMaxResidual(MatrixGridSize.FIVE, RiskLevel.LOW));
        assertEquals(12, ExcelRiskScoring.appetiteMaxResidual(MatrixGridSize.FIVE, RiskLevel.MEDIUM));
        assertEquals(4, ExcelRiskScoring.autoCloseMaxScore(MatrixGridSize.FIVE));
    }

    @Test
    void fourByFourBandsAndAppetite() {
        assertEquals("VERY_LOW", ExcelRiskScoring.rating(MatrixGridSize.FOUR, 2));
        assertEquals("LOW", ExcelRiskScoring.rating(MatrixGridSize.FOUR, 4));
        assertEquals("MEDIUM", ExcelRiskScoring.rating(MatrixGridSize.FOUR, 6));
        assertEquals("HIGH", ExcelRiskScoring.rating(MatrixGridSize.FOUR, 12));
        assertEquals("VERY_HIGH", ExcelRiskScoring.rating(MatrixGridSize.FOUR, 16));
        assertEquals(4, ExcelRiskScoring.appetiteMaxResidual(MatrixGridSize.FOUR, RiskLevel.LOW));
        assertEquals(2, ExcelRiskScoring.autoCloseMaxScore(MatrixGridSize.FOUR));
    }

    @Test
    void threeByThreeBandsAndAppetite() {
        assertEquals("VERY_LOW", ExcelRiskScoring.rating(MatrixGridSize.THREE, 1));
        assertEquals("LOW", ExcelRiskScoring.rating(MatrixGridSize.THREE, 2));
        assertEquals("MEDIUM", ExcelRiskScoring.rating(MatrixGridSize.THREE, 3));
        assertEquals("HIGH", ExcelRiskScoring.rating(MatrixGridSize.THREE, 6));
        assertEquals("VERY_HIGH", ExcelRiskScoring.rating(MatrixGridSize.THREE, 9));
        assertEquals(2, ExcelRiskScoring.appetiteMaxResidual(MatrixGridSize.THREE, RiskLevel.LOW));
        assertEquals(6, ExcelRiskScoring.appetiteMaxResidual(MatrixGridSize.THREE, RiskLevel.HIGH));
        assertEquals(1, ExcelRiskScoring.autoCloseMaxScore(MatrixGridSize.THREE));
    }

    @Test
    void requireProductScoreCapsAtInherent() {
        assertDoesNotThrow(() -> ExcelRiskScoring.requireProductScore(
                16, MatrixGridSize.FIVE, 16, "Residual Risk Score"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ExcelRiskScoring.requireProductScore(17, MatrixGridSize.FIVE, 16, "Residual Risk Score"));
        assertEquals("Residual Risk Score must be a whole number from 1 to 16", ex.getReason());
    }
}
