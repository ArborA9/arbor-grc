package com.a9.arbor.grc.modules.gap.service;

import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapImplementationStatus;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UaeIaGapScoringTest {

    @Test
    void controlStatusAveragesOnlyScoredLeavesLikeExcel() {
        GapItem implemented = leafOn(41L, GapImplementationStatus.FULLY_IMPLEMENTED);
        GapItem blank = leafOn(42L, null);
        GapItem blank2 = leafOn(43L, null);
        UaeIaGapScoring.ControlRollup rollup = UaeIaGapScoring.rollupControl(List.of(implemented, blank, blank2));
        assertEquals(new BigDecimal("100.00"), rollup.statusPercent());
        assertTrue(rollup.started());
    }

    @Test
    void unstartedControlContributesZero() {
        UaeIaGapScoring.ControlRollup rollup = UaeIaGapScoring.rollupControl(List.of(leafOn(41L, null), leafOn(42L, null)));
        assertEquals(new BigDecimal("0.00"), rollup.statusPercent());
        assertFalse(rollup.started());
    }

    @Test
    void naLeavesAreExcludedFromControlAverage() {
        GapItem implemented = leafOn(41L, GapImplementationStatus.FULLY_IMPLEMENTED);
        implemented.setApplicability(GapApplicability.APPLICABLE);
        GapItem na = leafOn(42L, GapImplementationStatus.NOT_IMPLEMENTED);
        na.setApplicability(GapApplicability.NOT_APPLICABLE);
        UaeIaGapScoring.ControlRollup rollup = UaeIaGapScoring.rollupControl(List.of(implemented, na));
        assertEquals(new BigDecimal("100.00"), rollup.statusPercent());
    }

    @Test
    void overallAveragesEveryApplicableControlIncludingUnstarted() {
        // M1.1.1: one of three L4s implemented -> control 100% (Excel AVERAGEIF)
        // M2.1.1: not started -> 0
        // T9.1.1: not started -> 0
        // Overall = 100 / 3
        Map<Long, FrameworkNodeResponse> nodes = nodes();
        List<GapItem> items = List.of(
                leafOn(41L, GapImplementationStatus.FULLY_IMPLEMENTED),
                leafOn(42L, null),
                leafOn(43L, null),
                leafOn(51L, null),
                leafOn(61L, null));

        GapLayerScoreSummary summary = UaeIaGapScoring.compute(items, nodes);
        assertEquals(new BigDecimal("33.33"), summary.overallUaeIa().score());
        assertEquals(3, summary.overallUaeIa().applicableCount());
        assertEquals(1, summary.overallUaeIa().scoredCount());
        assertEquals(new BigDecimal("50.00"), summary.management().score());
        assertEquals(2, summary.management().applicableCount());
        assertEquals(1, summary.management().scoredCount());
        assertEquals(new BigDecimal("0.00"), summary.technical().score());
        assertEquals(1, summary.technical().applicableCount());
        assertEquals(0, summary.technical().scoredCount());
        assertNull(summary.technical().rag());
    }

    private static Map<Long, FrameworkNodeResponse> nodes() {
        FrameworkNodeResponse m1 = node(1L, null, 1, "M1");
        FrameworkNodeResponse m11 = node(10L, 1L, 2, "M1.1");
        FrameworkNodeResponse m111 = node(11L, 10L, 3, "M1.1.1");
        FrameworkNodeResponse m1111 = node(41L, 11L, 4, "M1.1.1.1");
        FrameworkNodeResponse m1112 = node(42L, 11L, 4, "M1.1.1.2");
        FrameworkNodeResponse m1113 = node(43L, 11L, 4, "M1.1.1.3");
        FrameworkNodeResponse m2 = node(2L, null, 1, "M2");
        FrameworkNodeResponse m21 = node(20L, 2L, 2, "M2.1");
        FrameworkNodeResponse m211 = node(21L, 20L, 3, "M2.1.1");
        FrameworkNodeResponse m2111 = node(51L, 21L, 4, "M2.1.1.1");
        FrameworkNodeResponse t9 = node(9L, null, 1, "T9");
        FrameworkNodeResponse t91 = node(90L, 9L, 2, "T9.1");
        FrameworkNodeResponse t911 = node(31L, 90L, 3, "T9.1.1");
        FrameworkNodeResponse t9111 = node(61L, 31L, 4, "T9.1.1.1");
        return Map.ofEntries(
                Map.entry(1L, m1), Map.entry(10L, m11), Map.entry(11L, m111),
                Map.entry(41L, m1111), Map.entry(42L, m1112), Map.entry(43L, m1113),
                Map.entry(2L, m2), Map.entry(20L, m21), Map.entry(21L, m211), Map.entry(51L, m2111),
                Map.entry(9L, t9), Map.entry(90L, t91), Map.entry(31L, t911), Map.entry(61L, t9111));
    }

    private static FrameworkNodeResponse node(long id, Long parentId, int level, String code) {
        return new FrameworkNodeResponse(
                id, parentId, level, code, code, null, null, null, 0, null, null,
                true, true, true, true, false, Map.of());
    }

    private static GapItem leafOn(long nodeId, GapImplementationStatus status) {
        GapItem item = new GapItem();
        item.setFrameworkNodeId(nodeId);
        item.setNodeCode("n" + nodeId);
        item.setTitle("t");
        item.setApplicability(GapApplicability.APPLICABLE);
        item.setImplementationStatus(status);
        return item;
    }
}
