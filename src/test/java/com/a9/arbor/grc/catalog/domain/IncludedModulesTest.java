package com.a9.arbor.grc.catalog.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncludedModulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @Test
    void auditIsOffWithoutAPaidModule() {
        TenantModule audit = row(ModuleCode.AUDIT, true, null);
        TenantModule isms = row(ModuleCode.ISMS, false, null);
        TenantModule out = audit(List.of(audit, isms));
        assertFalse(out.isEnabled());
        assertNull(out.getExpiresAt());
    }

    @Test
    void auditStaysWritableWhileOnePaidModuleIsInTerm() {
        TenantModule out = audit(List.of(
                row(ModuleCode.AUDIT, true, TODAY.minusYears(1)),
                row(ModuleCode.ISMS, true, TODAY.minusDays(3)),
                row(ModuleCode.ERM, true, TODAY.plusDays(30))));
        assertTrue(out.isEnabled());
        assertEquals(TODAY.plusDays(30), LicenseDates.toExpiryDate(out.getExpiresAt()));
        assertFalse(LicenseDates.isExpired(out.getExpiresAt(), TODAY));
    }

    @Test
    void auditExpiresOnlyWhenEveryPaidModuleHasExpired() {
        TenantModule out = audit(List.of(
                row(ModuleCode.AUDIT, true, null),
                row(ModuleCode.ISMS, true, TODAY.minusDays(3)),
                row(ModuleCode.ERM, true, TODAY.minusDays(1))));
        assertTrue(out.isEnabled());
        assertTrue(LicenseDates.isExpired(out.getExpiresAt(), TODAY));
        assertEquals(TODAY.minusDays(1), LicenseDates.toExpiryDate(out.getExpiresAt()));
    }

    @Test
    void openEndedPaidModuleKeepsAuditOpenEnded() {
        TenantModule out = audit(List.of(
                row(ModuleCode.AUDIT, true, null),
                row(ModuleCode.ISMS, true, TODAY.minusDays(3)),
                row(ModuleCode.BCMS, true, null)));
        assertTrue(out.isEnabled());
        assertNull(out.getExpiresAt());
    }

    @Test
    void paidRowsPassThroughUnchanged() {
        TenantModule isms = row(ModuleCode.ISMS, true, TODAY.plusDays(5));
        List<TenantModule> out = IncludedModules.effective(List.of(row(ModuleCode.AUDIT, true, null), isms));
        assertSame(isms, out.get(1));
    }

    private static TenantModule audit(List<TenantModule> rows) {
        return IncludedModules.effective(rows).stream()
                .filter(r -> r.getModuleCode() == ModuleCode.AUDIT)
                .findFirst()
                .orElseThrow();
    }

    private static TenantModule row(ModuleCode code, boolean enabled, LocalDate expiresOn) {
        TenantModule row = new TenantModule();
        row.setModuleCode(code);
        row.setEnabled(enabled);
        row.setExpiresAt(LicenseDates.toExpiryInstant(expiresOn));
        return row;
    }
}
