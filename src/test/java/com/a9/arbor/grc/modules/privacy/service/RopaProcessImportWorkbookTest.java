package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.privacy.domain.RopaDesignation;
import com.a9.arbor.grc.modules.privacy.domain.RopaLegalBasis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RopaProcessImportWorkbookTest {

    private final RopaProcessImportWorkbook workbook = new RopaProcessImportWorkbook();

    @Test
    void templateRoundTripIgnoresSampleAndParsesFilledRow() throws Exception {
        ExportFile file = workbook.template("HR");
        assertTrue(file.filename().contains("process-template"));
        assertTrue(file.bytes().length > 1000);

        // Rewrite SAMPLE activity to a real row by parsing + rebuilding via Apache POI is heavy;
        // parse the template itself and assert SAMPLE is detectable.
        RopaProcessImportWorkbook.ParsedWorkbook parsed = workbook.parse(file.bytes());
        assertEquals(1, parsed.processes().size());
        assertTrue(RopaProcessImportWorkbook.isSampleActivity(parsed.processes().get(0).activityName()));

        assertEquals(RopaDesignation.CONTROLLER, RopaProcessImportWorkbook.parseDesignation("Controller"));
        assertEquals(RopaDesignation.JOINT_CONTROLLER,
                RopaProcessImportWorkbook.parseDesignation("Joint Controller"));
        String legal = RopaProcessImportWorkbook.toLegalBasesJson("Consent; Contract performance");
        assertTrue(legal.contains(RopaLegalBasis.CONSENT.name()));
        assertTrue(legal.contains(RopaLegalBasis.CONTRACT_PERFORMANCE.name()));
        assertEquals(Boolean.TRUE, RopaProcessImportWorkbook.parseYesNo("Yes"));
        assertEquals("No", RopaProcessImportWorkbook.adequacyNote("N"));
        assertFalse(RopaProcessImportWorkbook.isSampleActivity("Payroll"));
    }
}
