package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultIsRiskLibraryTest {

    @Test
    void classpathWorkbooksMatchArborPackCounts() {
        DefaultIsRiskLibrary lib = new DefaultIsRiskLibrary(null);
        assertEquals(123, lib.rowsFor(FrameworkTypeCode.ISO_27001).size());
        assertEquals(7, lib.rowsFor(FrameworkTypeCode.ISO_27017).size());
        assertEquals(134, lib.rowsFor(FrameworkTypeCode.UAE_IA).size());
        assertEquals(46, lib.rowsFor(FrameworkTypeCode.ISO_22301).size());
        assertEquals(63, lib.rowsFor(FrameworkTypeCode.ISO_20000).size());
        assertEquals(70, lib.rowsFor(FrameworkTypeCode.ISO_42001).size());
        assertTrue(lib.rowsFor(FrameworkTypeCode.ISO_27001).stream()
                .anyMatch(r -> "A.5.1".equals(r.controlCode()) && r.title() != null && !r.title().isBlank()));
        assertTrue(lib.rowsFor(FrameworkTypeCode.ISO_27017).stream()
                .anyMatch(r -> r.controlCode().startsWith("CLD.")));
        assertTrue(lib.rowsFor(FrameworkTypeCode.CIS_V8).isEmpty());
        assertFalse(lib.rowsFor(FrameworkTypeCode.ISO_27001).get(0).threats().isBlank());
    }
}
