package com.a9.arbor.grc.security;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LicenseApiModulesTest {

    @Test
    void exclusivePathsMapToOneModule() {
        assertEquals(ModuleCode.ISMS, LicenseApiModules.exclusiveModule("/api/assets"));
        assertEquals(ModuleCode.ISMS, LicenseApiModules.exclusiveModule("/api/assets/12"));
        assertEquals(ModuleCode.BCMS, LicenseApiModules.exclusiveModule("/api/bcms/bia"));
        assertEquals(ModuleCode.BCMS, LicenseApiModules.exclusiveModule("/api/bcms/dept-profiles"));
        assertEquals(ModuleCode.ERM, LicenseApiModules.exclusiveModule("/api/tprm/vendors"));
        assertEquals(ModuleCode.PRIVACY, LicenseApiModules.exclusiveModule("/api/privacy/dsr"));
        assertEquals(ModuleCode.AUDIT, LicenseApiModules.exclusiveModule("/api/audits/12"));
    }

    @Test
    void mixedAndPlatformPathsAreNotExclusive() {
        assertNull(LicenseApiModules.exclusiveModule("/api/org/units"));
        assertNull(LicenseApiModules.exclusiveModule("/api/users"));
        assertNull(LicenseApiModules.exclusiveModule("/api/risks"));
        assertNull(LicenseApiModules.exclusiveModule("/api/gap-assessments"));
        assertNull(LicenseApiModules.exclusiveModule("/api/soa"));
        assertNull(LicenseApiModules.exclusiveModule("/api/frameworks/3/activate"));
        assertNull(LicenseApiModules.exclusiveModule("/api/evidence"));
        assertNull(LicenseApiModules.exclusiveModule("/api/audits/external-access/x"));
    }
}
