package com.a9.arbor.grc.modules.risk.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FcaControlEffectivenessTest {

    @Test
    void designPerformanceGrid() {
        assertEquals(5, FcaControlEffectiveness.score(
                FcaControlEffectiveness.Axis.SATISFACTORY, FcaControlEffectiveness.Axis.SATISFACTORY));
        assertEquals(4, FcaControlEffectiveness.score(
                FcaControlEffectiveness.Axis.SATISFACTORY, FcaControlEffectiveness.Axis.NEEDS_IMPROVEMENT));
        assertEquals(2, FcaControlEffectiveness.score(
                FcaControlEffectiveness.Axis.SATISFACTORY, FcaControlEffectiveness.Axis.UNSATISFACTORY));
        assertEquals(3, FcaControlEffectiveness.score(
                FcaControlEffectiveness.Axis.NEEDS_IMPROVEMENT, FcaControlEffectiveness.Axis.SATISFACTORY));
        assertEquals(1, FcaControlEffectiveness.score(
                FcaControlEffectiveness.Axis.UNSATISFACTORY, FcaControlEffectiveness.Axis.SATISFACTORY));
    }

    @Test
    void consolidateCapsWhenAnyPerformanceUnsatisfactory() {
        assertEquals(4, FcaControlEffectiveness.consolidate(List.of(5, 4, 3), false));
        assertEquals(3, FcaControlEffectiveness.consolidate(List.of(5, 4, 2), true));
        assertEquals(3, FcaControlEffectiveness.consolidate(List.of(5, 5, 5, 5, 2), true));
    }

    @Test
    void wordLabelsMatchGrid() {
        assertEquals("Very strong", FcaControlEffectiveness.word(5));
        assertEquals("Strong", FcaControlEffectiveness.word(4));
        assertEquals("Moderate", FcaControlEffectiveness.word(3));
        assertEquals("Weak", FcaControlEffectiveness.word(2));
        assertEquals("Ineffective", FcaControlEffectiveness.word(1));
    }
}
