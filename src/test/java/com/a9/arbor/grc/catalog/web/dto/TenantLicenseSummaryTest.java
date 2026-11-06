package com.a9.arbor.grc.catalog.web.dto;

import com.a9.arbor.grc.catalog.domain.LicenseDates;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.domain.TenantModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantLicenseSummaryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Test
    void emptyRowsAreNone() {
        TenantLicenseSummary summary = TenantLicenseSummary.from(List.of(), TODAY);
        assertEquals(0, summary.licensedCount());
        assertEquals("NONE", summary.status());
        assertFalse(summary.writeLocked());
        assertNull(summary.earliestExpiresOn());
    }

    @Test
    void mixedDatesUseEarliestAndExpireLock() {
        TenantModule audit = enabled(ModuleCode.AUDIT, null);
        TenantModule isms = enabled(ModuleCode.ISMS, TODAY.minusDays(1));
        TenantModule erm = enabled(ModuleCode.ERM, TODAY.plusDays(14));
        TenantLicenseSummary summary = TenantLicenseSummary.from(List.of(audit, isms, erm), TODAY);
        assertEquals(3, summary.licensedCount());
        assertTrue(summary.writeLocked());
        assertEquals("EXPIRED", summary.status());
        assertTrue(summary.mixedExpiry());
        assertEquals(TODAY.minusDays(1), summary.earliestExpiresOn());
        assertEquals(List.of("AUDIT", "ERM", "ISMS"), summary.licensedCodes());
    }

    @Test
    void sharedFutureDateIsActive() {
        LocalDate expiry = TODAY.plusDays(200);
        TenantModule audit = enabled(ModuleCode.AUDIT, expiry);
        TenantModule isms = enabled(ModuleCode.ISMS, expiry);
        TenantLicenseSummary summary = TenantLicenseSummary.from(List.of(audit, isms), TODAY);
        assertEquals("ACTIVE", summary.status());
        assertFalse(summary.writeLocked());
        assertFalse(summary.mixedExpiry());
        assertEquals(expiry, summary.earliestExpiresOn());
        assertEquals(200, summary.daysRemaining());
    }

    private static TenantModule enabled(ModuleCode code, LocalDate expiresOn) {
        TenantModule row = new TenantModule();
        row.setModuleCode(code);
        row.setEnabled(true);
        row.setExpiresAt(LicenseDates.toExpiryInstant(expiresOn));
        return row;
    }
}
