package com.a9.arbor.grc.catalog.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseExpiryMilestoneTest {

    @Test
    void windowsMatchRequestedCadence() {
        assertEquals(LicenseExpiryMilestone.P90, LicenseExpiryMilestone.matching(90).orElseThrow());
        assertEquals(LicenseExpiryMilestone.P90, LicenseExpiryMilestone.matching(61).orElseThrow());
        assertEquals(LicenseExpiryMilestone.P60, LicenseExpiryMilestone.matching(60).orElseThrow());
        assertEquals(LicenseExpiryMilestone.P30, LicenseExpiryMilestone.matching(30).orElseThrow());
        assertEquals(LicenseExpiryMilestone.P14, LicenseExpiryMilestone.matching(14).orElseThrow());
        assertEquals(LicenseExpiryMilestone.P7, LicenseExpiryMilestone.matching(7).orElseThrow());
        assertEquals(LicenseExpiryMilestone.P1, LicenseExpiryMilestone.matching(1).orElseThrow());
        assertEquals(LicenseExpiryMilestone.EXPIRED, LicenseExpiryMilestone.matching(0).orElseThrow());
        assertEquals(LicenseExpiryMilestone.EXPIRED, LicenseExpiryMilestone.matching(-3).orElseThrow());
        assertTrue(LicenseExpiryMilestone.matching(200).isEmpty());
    }

    @Test
    void calendarDaysUseLocalDates() {
        LocalDate today = LocalDate.of(2026, 9, 17);
        assertEquals(14, LicenseExpiryMilestone.daysRemaining(today, LocalDate.of(2026, 10, 1)));
        assertEquals(-1, LicenseExpiryMilestone.daysRemaining(today, LocalDate.of(2026, 9, 16)));
    }
}

class LicenseDatesTest {

    @Test
    void expiryIsLastInclusiveCalendarDay() {
        LocalDate today = LocalDate.of(2026, 9, 17);
        var instant = LicenseDates.toExpiryInstant(today);
        assertFalse(LicenseDates.isExpired(instant, today));
        assertTrue(LicenseDates.isExpired(instant, today.plusDays(1)));
        assertEquals(today, LicenseDates.toExpiryDate(instant));
    }
}
