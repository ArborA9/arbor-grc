package com.a9.arbor.grc.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseWriteLockFilterTest {

    @Test
    void downloadsAndExportsAreBlockedWhileLocked() {
        assertTrue(LicenseWriteLockFilter.isDownloadOrExport("/api/risks/12/export"));
        assertTrue(LicenseWriteLockFilter.isDownloadOrExport("/api/evidence/9/download"));
        assertTrue(LicenseWriteLockFilter.isDownloadOrExport("/api/soa/3/export.xlsx"));
        assertTrue(LicenseWriteLockFilter.isDownloadOrExport("/api/reports/1/export"));
        assertFalse(LicenseWriteLockFilter.isDownloadOrExport("/api/risks"));
        assertFalse(LicenseWriteLockFilter.isDownloadOrExport("/api/audits/checklist-library/template.xlsx"));
    }

    @Test
    void bannerDismissAndInboxStayAllowed() {
        assertTrue(LicenseWriteLockFilter.isAllowedWhileLocked("/api/license/banner-dismiss", "POST"));
        assertTrue(LicenseWriteLockFilter.isAllowedWhileLocked("/api/notifications/4/read", "POST"));
        assertFalse(LicenseWriteLockFilter.isAllowedWhileLocked("/api/notifications/templates/1/active", "PUT"));
        assertFalse(LicenseWriteLockFilter.isAllowedWhileLocked("/api/risks", "POST"));
    }

    @Test
    void catalogAndAuthBypassTheLock() {
        assertTrue(LicenseWriteLockFilter.isAlwaysOpen("/api/webhooks/malware-scan"));
        assertTrue(LicenseWriteLockFilter.isAlwaysOpen("/api/admin/tenants/acme/modules"));
        assertTrue(LicenseWriteLockFilter.isAlwaysOpen("/api/auth/logout"));
        assertFalse(LicenseWriteLockFilter.isAlwaysOpen("/api/org/units"));
    }
}
