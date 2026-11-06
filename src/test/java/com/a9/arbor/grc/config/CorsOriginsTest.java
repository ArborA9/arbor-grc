package com.a9.arbor.grc.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsOriginsTest {

    @Test
    void splitsCommaSeparatedOrigins() {
        assertEquals(
                List.of("https://app.arborgrc.com", "https://www.arborgrc.com"),
                CorsOrigins.split(List.of("https://app.arborgrc.com, https://www.arborgrc.com")));
    }

    @Test
    void dropsLocalhostOnlyWhenAsked() {
        List<String> raw = List.of("http://localhost:5173", "https://app.arborgrc.com");
        assertEquals(List.of("https://app.arborgrc.com"), CorsOrigins.effective(raw, true));
        assertEquals(raw, CorsOrigins.effective(raw, false));
    }

    @Test
    void recognizesLocalDevHosts() {
        assertTrue(CorsOrigins.isLocalDev("http://localhost:5173"));
        assertTrue(CorsOrigins.isLocalDev("http://127.0.0.1:5173"));
        assertFalse(CorsOrigins.isLocalDev("https://uat.arborgrc.com"));
        assertFalse(CorsOrigins.isLocalDev("https://*.arborgrc.com"));
    }
}
