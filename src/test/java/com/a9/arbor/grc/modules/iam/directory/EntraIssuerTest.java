package com.a9.arbor.grc.modules.iam.directory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntraIssuerTest {

    @Test
    void readsGuidFromV2Issuer() {
        assertEquals("11111111-2222-3333-4444-555555555555",
                EntraIssuer.directoryId(
                        "https://login.microsoftonline.com/11111111-2222-3333-4444-555555555555/v2.0")
                        .orElseThrow());
    }

    @Test
    void readsDomainFromIssuer() {
        assertEquals("adfca.gov.ae",
                EntraIssuer.directoryId("https://login.microsoftonline.com/adfca.gov.ae/v2.0")
                        .orElseThrow());
    }

    @Test
    void rejectsGenericOidcAndMultiTenantAliases() {
        assertTrue(EntraIssuer.directoryId("https://idp.example.com/realms/acme").isEmpty());
        assertTrue(EntraIssuer.directoryId("https://login.microsoftonline.com/common/v2.0").isEmpty());
        assertTrue(EntraIssuer.directoryId(null).isEmpty());
    }

    @Test
    void buildsV2LogoutUrl() {
        String url = EntraIssuer.endSessionUrl(
                "https://login.microsoftonline.com/11111111-2222-3333-4444-555555555555/v2.0",
                "https://uat.arborgrc.com/signin").orElseThrow();
        assertEquals(
                "https://login.microsoftonline.com/11111111-2222-3333-4444-555555555555/oauth2/v2.0/logout"
                        + "?post_logout_redirect_uri=https%3A%2F%2Fuat.arborgrc.com%2Fsignin",
                url);
    }

    @Test
    void logoutUrlRejectsMultiTenantIssuer() {
        assertTrue(EntraIssuer.endSessionUrl(
                "https://login.microsoftonline.com/common/v2.0",
                "https://uat.arborgrc.com/signin").isEmpty());
        assertTrue(EntraIssuer.endSessionUrl(
                "https://login.microsoftonline.com/11111111-2222-3333-4444-555555555555/v2.0",
                "  ").isEmpty());
    }

    @Test
    void sanitizesSearchQuery() {
        assertEquals("Jane Doe", EntraIssuer.sanitizeQuery("  Jane Doe  "));
        assertEquals("Jane OR mail:x", EntraIssuer.sanitizeQuery("Jane\" OR mail:x"));
        assertEquals("", EntraIssuer.sanitizeQuery("   "));
    }
}
