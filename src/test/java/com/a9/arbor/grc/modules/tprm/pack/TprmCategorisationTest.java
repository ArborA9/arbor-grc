package com.a9.arbor.grc.modules.tprm.pack;

import com.a9.arbor.grc.modules.tprm.domain.TprmCertType;
import com.a9.arbor.grc.modules.tprm.domain.TprmCriticality;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TprmCategorisationTest {

    @Test
    void cspSecretPrivilegedIsTier1Critical() {
        TprmCategorisation.Result r = TprmCategorisation.calculate(new TprmCategorisation.Input(
                "Secret",
                "Privileged or administrative access",
                "A critical service stops, with no workaround",
                "Vendor-owned infrastructure, worked remotely / off site",
                "Large volumes, or special-category data",
                "Sole source, replacement would take more than six months",
                List.of(),
                null));
        assertEquals(24, r.inherent());
        assertEquals(0, r.assuranceCredit());
        assertEquals(24, r.adjusted());
        assertEquals("TIER_1", r.calculatedTier());
        assertEquals("TIER_1", r.effectiveTier());
        assertEquals(TprmCriticality.CRITICAL, r.criticality());
        assertEquals(0.9, r.postureTarget());
        assertEquals(12, r.reassessmentMonths());
        assertFalse(r.elevationFlag());
    }

    @Test
    void nonItSensitiveReadOnlyMinimalIsTier4NonCritical() {
        TprmCategorisation.Result r = TprmCategorisation.calculate(new TprmCategorisation.Input(
                "Sensitive",
                "Read-only or view access, or physical access to a critical area",
                "Minimal operational effect",
                "Organization-provided assets, worked remotely / off site",
                "No personal data",
                "Readily replaceable, commodity service",
                List.of(),
                null));
        assertEquals(10, r.inherent());
        assertEquals(10, r.adjusted());
        assertEquals("TIER_4", r.calculatedTier());
        assertEquals(TprmCriticality.NON_CRITICAL, r.criticality());
        assertEquals(0.6, r.postureTarget());
        assertEquals(36, r.reassessmentMonths());
    }

    @Test
    void certsCapAssuranceCreditAtThree() {
        int credit = TprmCategorisation.assuranceCredit(List.of(
                TprmCertType.ISO_27001,
                TprmCertType.SOC2_TYPE_II,
                TprmCertType.UAE_IA,
                TprmCertType.ISO_42001,
                TprmCertType.NONE));
        assertEquals(3, credit);
    }

    @Test
    void elevationFlagWhenAccessIsFourButTierIsLow() {
        TprmCategorisation.Result r = TprmCategorisation.calculate(new TprmCategorisation.Input(
                "Secret",
                "No access of any kind",
                "Minimal operational effect",
                "Organization-provided assets, worked on Organization premises",
                "No personal data",
                "Readily replaceable, commodity service",
                List.of(),
                null));
        assertEquals(9, r.inherent());
        assertEquals("TIER_4", r.calculatedTier());
        assertTrue(r.elevationFlag());
    }

    @Test
    void oneLevelOverrideRaisesEffectiveTier() {
        TprmCategorisation.Result r = TprmCategorisation.calculate(new TprmCategorisation.Input(
                "Sensitive",
                "Read-only or view access, or physical access to a critical area",
                "Minimal operational effect",
                "Organization-provided assets, worked remotely / off site",
                "No personal data",
                "Readily replaceable, commodity service",
                List.of(),
                "TIER_3"));
        assertEquals("TIER_4", r.calculatedTier());
        assertEquals("TIER_3", r.effectiveTier());
        assertEquals(TprmCriticality.NON_CRITICAL, r.criticality());
        assertEquals(0.7, r.postureTarget());
    }
}
