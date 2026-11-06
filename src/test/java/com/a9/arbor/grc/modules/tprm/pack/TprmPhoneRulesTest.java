package com.a9.arbor.grc.modules.tprm.pack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TprmPhoneRulesTest {

    @Test
    void uaeRequiresExactlyNineNationalDigits() {
        assertEquals(9, TprmPhoneRules.nationalLength("+971"));
        assertTrue(TprmPhoneRules.isValid("+971501234567"));
        assertTrue(TprmPhoneRules.isValid("+971123456789"));
        assertFalse(TprmPhoneRules.isValid("+97150123456"));
        assertFalse(TprmPhoneRules.isValid("+9715012345678"));
    }

    @Test
    void listedCountryAcceptsExactNationalLength() {
        assertEquals(10, TprmPhoneRules.nationalLength("+91"));
        assertTrue(TprmPhoneRules.isValid("+919876543210"));
        assertTrue(TprmPhoneRules.isValid("+911111111111"));
        assertFalse(TprmPhoneRules.isValid("+91987654321"));

        assertEquals(10, TprmPhoneRules.nationalLength("+44"));
        assertTrue(TprmPhoneRules.isValid("+441111111111"));
        assertFalse(TprmPhoneRules.isValid("+44111111111"));

        assertEquals(8, TprmPhoneRules.nationalLength("+974"));
        assertTrue(TprmPhoneRules.isValid("+97411111111"));
        assertFalse(TprmPhoneRules.isValid("+9741111111"));

        assertEquals(10, TprmPhoneRules.nationalLength("+1"));
        assertTrue(TprmPhoneRules.isValid("+11111111111"));
        assertFalse(TprmPhoneRules.isValid("+1111111111"));
    }

    @Test
    void rejectsUnknownDialAndGarbage() {
        assertFalse(TprmPhoneRules.isValid("not-a-phone"));
        assertFalse(TprmPhoneRules.isValid("+8613812345678"));
        assertFalse(TprmPhoneRules.isValid("+971"));
    }
}
