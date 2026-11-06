package com.a9.arbor.grc.modules.gap.pack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GapContributorTextTest {

    @Test
    void blankIsAllowed() {
        assertTrue(GapContributorText.isAllowed(null));
        assertTrue(GapContributorText.isAllowed(""));
        assertTrue(GapContributorText.isAllowed("   "));
        assertDoesNotThrow(() -> GapContributorText.requireAllowed(null));
    }

    @Test
    void proseAndArabicAreAllowed() {
        assertTrue(GapContributorText.isAllowed("Context and current practice for this control."));
        assertTrue(GapContributorText.isAllowed("ISO 27001:2022 - clause 4.1 (yes / no)"));
        assertTrue(GapContributorText.isAllowed("تم تنفيذ الإجراء في القسم."));
        assertTrue(GapContributorText.isAllowed("Don't use the old process; use the new one!"));
    }

    @Test
    void symbolsAreRejected() {
        assertFalse(GapContributorText.isAllowed("Context @#$ and practice."));
        assertFalse(GapContributorText.isAllowed("50% complete"));
        assertFalse(GapContributorText.isAllowed("<script>alert(1)</script>"));
        assertThrows(IllegalArgumentException.class,
                () -> GapContributorText.requireAllowed("ok # no"));
    }
}
