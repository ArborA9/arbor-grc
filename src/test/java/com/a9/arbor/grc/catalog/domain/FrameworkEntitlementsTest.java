package com.a9.arbor.grc.catalog.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkEntitlementsTest {

    @Test
    void omittedIdsEntitleLatestPerCodeNotEveryVersion() {
        Set<Long> picked = FrameworkEntitlements.select(
                null, List.of(11L, 12L, 21L), List.of(11L, 21L), "ISMS");
        assertEquals(Set.of(11L, 21L), picked);
    }

    @Test
    void oneFrameworkOnly() {
        Set<Long> picked = FrameworkEntitlements.select(
                List.of(11L), List.of(11L, 21L), List.of(11L, 21L), "ISMS");
        assertEquals(Set.of(11L), picked);
    }

    @Test
    void someButNotAll() {
        Set<Long> picked = FrameworkEntitlements.select(
                List.of(11L, 21L), List.of(11L, 21L, 31L), List.of(11L, 21L, 31L), "ISMS");
        assertEquals(Set.of(11L, 21L), picked);
    }

    @Test
    void allPublishedIds() {
        Set<Long> picked = FrameworkEntitlements.select(
                List.of(11L, 21L, 31L), List.of(11L, 21L, 31L), List.of(11L, 21L, 31L), "ISMS");
        assertEquals(Set.of(11L, 21L, 31L), picked);
    }

    @Test
    void emptyListIsNotTheFullPack() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                FrameworkEntitlements.select(List.of(), List.of(11L, 21L), List.of(11L, 21L), "ISMS"));
        assertTrue(ex.getMessage().contains("Select at least one published framework under ISMS"));
    }

    @Test
    void unknownIdRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                FrameworkEntitlements.select(List.of(99L), List.of(11L), List.of(11L), "ISMS"));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void noPublishedMeansModuleLicenceOnly() {
        assertEquals(Set.of(), FrameworkEntitlements.select(null, List.of(), List.of(), "ERM"));
        assertEquals(Set.of(), FrameworkEntitlements.select(List.of(), List.of(), List.of(), "ERM"));
    }
}
