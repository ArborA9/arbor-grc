package com.a9.arbor.grc.modules.bcms.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiaProcessImportWorkbookTest {

    private final BiaProcessImportWorkbook workbook = new BiaProcessImportWorkbook();

    @Test
    void templateIsSingleProcessSheetAndBlank() {
        var file = workbook.template("BIA-2026-1-1", java.util.List.of("owner@example.com"));
        assertTrue(file.filename().contains("BIA-2026-1-1"));
        var parsed = workbook.parse(file.bytes());
        assertEquals(1, parsed.processes().size());
        assertTrue(BiaProcessImportWorkbook.isSampleCode(parsed.processes().get(0).processCode()));
        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(file.bytes()))) {
            assertEquals("Processes", wb.getSheetName(0));
            assertEquals(org.apache.poi.ss.usermodel.SheetVisibility.HIDDEN,
                    wb.getSheetVisibility(wb.getSheetIndex("Reference")));
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void monotonicRejectsDropAndAcceptsNonDecreasing() {
        assertNotNull(BiaProcessImportWorkbook.monotonicError(new Integer[]{2, 3, 2, 4, 5}));
        assertNull(BiaProcessImportWorkbook.monotonicError(new Integer[]{1, 2, 3, 4, 5}));
        assertNull(BiaProcessImportWorkbook.monotonicError(new Integer[]{3, 3, 3, 4, 5}));
        assertNotNull(BiaProcessImportWorkbook.monotonicError(new Integer[]{1, 2, null, 4, 5}));
    }
}
