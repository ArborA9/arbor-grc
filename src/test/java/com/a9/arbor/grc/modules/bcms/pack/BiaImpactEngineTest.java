package com.a9.arbor.grc.modules.bcms.pack;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiaImpactEngineTest {

    @Test
    void packRemarkFirstThreeInFourToEightHours() {
        Integer[][] g = BiaImpactEngine.emptyGrid();
        // Strategic 2,3,3,4,5 — pack J2 example
        g[0] = new Integer[]{2, 3, 3, 4, 5};
        g[1] = new Integer[]{2, 2, 2, 4, 5};
        g[2] = new Integer[]{2, 2, 2, 4, 5};
        g[3] = new Integer[]{2, 2, 2, 4, 5};
        BiaImpactEngine.ImpactResult r = BiaImpactEngine.calculate(g);
        assertEquals(5, r.peak());
        assertEquals(2, r.tier());
        assertEquals("Critical - Tier 2", r.categoryLabel());
        assertEquals(8, r.maoHours());
        assertEquals(6, r.rtoHours());
        assertTrue(r.morRequired());
        assertTrue(r.allRatingsPresent());
    }

    @Test
    void firstThreeInZeroToFourIsTier1() {
        Integer[][] g = filled(2);
        g[0][0] = 3;
        BiaImpactEngine.ImpactResult r = BiaImpactEngine.calculate(g);
        assertEquals(1, r.tier());
        assertEquals(4, r.maoHours());
        assertEquals(3, r.rtoHours());
        assertTrue(r.morRequired());
    }

    @Test
    void firstThreeInEightToTwentyFourIsTier3() {
        Integer[][] g = filled(1);
        g[2][2] = 4;
        BiaImpactEngine.ImpactResult r = BiaImpactEngine.calculate(g);
        assertEquals(3, r.tier());
        assertEquals(24, r.maoHours());
        assertEquals(18, r.rtoHours());
        assertTrue(r.morRequired());
    }

    @Test
    void firstThreeInTwentyFourToFortyEightIsNormalTier4() {
        Integer[][] g = filled(1);
        g[1][3] = 3;
        BiaImpactEngine.ImpactResult r = BiaImpactEngine.calculate(g);
        assertEquals(4, r.tier());
        assertEquals(48, r.maoHours());
        assertEquals(36, r.rtoHours());
        assertFalse(r.morRequired());
    }

    @Test
    void noBandHitsThreeIsNormalTier5() {
        Integer[][] g = filled(2);
        BiaImpactEngine.ImpactResult r = BiaImpactEngine.calculate(g);
        assertEquals(5, r.tier());
        assertEquals(72, r.maoHours());
        assertEquals(54, r.rtoHours());
        assertFalse(r.morRequired());
        assertEquals(2, r.peak());
    }

    @Test
    void emptyGridLeavesCalcNull() {
        BiaImpactEngine.ImpactResult r = BiaImpactEngine.calculate(BiaImpactEngine.emptyGrid());
        assertNull(r.peak());
        assertNull(r.tier());
        assertFalse(r.anyRating());
        assertFalse(r.allRatingsPresent());
    }

    @Test
    void rankLowerTierFirstThenRowOrder() {
        List<BiaImpactEngine.RankResult> ranks = BiaImpactEngine.rank(List.of(
                new BiaImpactEngine.RankInput(10, 0, 3),
                new BiaImpactEngine.RankInput(11, 1, 1),
                new BiaImpactEngine.RankInput(12, 2, 1),
                new BiaImpactEngine.RankInput(13, 3, 5),
                new BiaImpactEngine.RankInput(14, 4, null)));
        assertEquals(11, ranks.get(0).processId());
        assertEquals(1, ranks.get(0).recoveryPriority());
        assertEquals(1, ranks.get(0).criticalRank());
        assertEquals(12, ranks.get(1).processId());
        assertEquals(2, ranks.get(1).criticalRank());
        assertEquals(10, ranks.get(2).processId());
        assertEquals(3, ranks.get(2).criticalRank());
        assertEquals(13, ranks.get(3).processId());
        assertNull(ranks.get(3).criticalRank());
        assertEquals(4, ranks.size());
    }

    @Test
    void completenessRequiresTwentyRatingsAndMorWhenCritical() {
        Integer[][] g = filled(3);
        BiaImpactEngine.ImpactResult impact = BiaImpactEngine.calculate(g);
        assertTrue(impact.morRequired());
        BiaImpactEngine.ProcessFields proc = new BiaImpactEngine.ProcessFields(
                "010.1", "Diagnostics", "desc", "trigger", "steps", "output",
                1L, "Annually", true, "HQ", true, "SharePoint");
        BiaImpactEngine.Completeness incomplete = BiaImpactEngine.completeness(proc, impact, null);
        assertFalse(incomplete.complete());
        BiaImpactEngine.MorFields mor = new BiaImpactEngine.MorFields(
                true, true, true, true, true, true, true);
        BiaImpactEngine.Completeness missingComments = BiaImpactEngine.completeness(
                proc, impact, new BiaImpactEngine.MorFields(true, true, true, true, true, false, true));
        assertFalse(missingComments.complete());
        assertTrue(missingComments.missing().contains("Recovery strategy comments"));
        BiaImpactEngine.Completeness missingSite = BiaImpactEngine.completeness(
                proc, impact, new BiaImpactEngine.MorFields(true, true, true, true, true, true, false));
        assertFalse(missingSite.complete());
        assertTrue(missingSite.missing().contains("Other site"));
        BiaImpactEngine.Completeness ok = BiaImpactEngine.completeness(proc, impact, mor);
        assertTrue(ok.complete());
        assertEquals(BiaImpactEngine.COMPLETE_READY, ok.label());
    }

    private static Integer[][] filled(int v) {
        Integer[][] g = BiaImpactEngine.emptyGrid();
        for (int c = 0; c < 4; c++) {
            for (int b = 0; b < 5; b++) {
                g[c][b] = v;
            }
        }
        return g;
    }
}
