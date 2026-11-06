package com.a9.arbor.grc.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpaRoutesTest {

    @Test
    void contributorDashboardRefreshIsAnAppPage() {
        assertTrue(SpaRoutes.isDocumentPath("/contributor-dashboard"));
        assertTrue(SpaRoutes.isDocumentPath("/contributor-dashboard/"));
        assertTrue(SpaRoutes.isDocumentPath("/contributor-dashboard?nav=contributor-dashboard"));
    }

    @Test
    void existingAppPagesStayServed() {
        assertTrue(SpaRoutes.isDocumentPath("/dashboard"));
        assertTrue(SpaRoutes.isDocumentPath("/assessments"));
        assertTrue(SpaRoutes.isDocumentPath("/soa"));
        assertTrue(SpaRoutes.isDocumentPath("/signin"));
        assertTrue(SpaRoutes.isDocumentPath("/signin/reset"));
    }

    @Test
    void apiAndUnknownPathsStayOffTheShell() {
        assertFalse(SpaRoutes.isDocumentPath("/api/health"));
        assertFalse(SpaRoutes.isDocumentPath("/api/dashboard/contributor"));
        assertFalse(SpaRoutes.isDocumentPath("/not-a-page"));
        assertFalse(SpaRoutes.isDocumentPath("/contributor-dashboard/extra"));
        assertFalse(SpaRoutes.isDocumentPath("/assets/index.js"));
    }
}
