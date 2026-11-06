package com.a9.arbor.grc.modules.privacy.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RopaSheetFieldsTest {

    private static void validate(boolean draft,
                                 String personal,
                                 String sensitive,
                                 String subjects,
                                 String volume,
                                 String frequency,
                                 String bases,
                                 Boolean shareExternal,
                                 String shareExternalDetails) {
        RopaSheetFields.validateContributorFields(
                draft ? null : "Desc",
                personal,
                sensitive,
                subjects,
                volume,
                frequency,
                draft ? null : "Source",
                draft ? null : "Method",
                bases,
                draft ? null : "Internal dept",
                shareExternal,
                shareExternalDetails,
                draft ? null : "UAE",
                draft ? null : "Yes",
                draft ? null : "Clauses",
                draft ? null : "System",
                draft ? null : "12 months",
                draft ? null : "Encryption",
                draft);
    }

    @Test
    void sheetValueSetsMatchWorkbookValidations() {
        assertEquals(10, RopaSheetFields.PERSONAL_DATA.size());
        assertEquals(7, RopaSheetFields.SENSITIVE_DATA.size());
        assertTrue(RopaSheetFields.SENSITIVE_DATA.contains(
                "Data related to the provision of healthcare services that reveals the person's health status"));
        assertEquals(3, RopaSheetFields.DATA_SUBJECT_CATEGORIES.size());
        assertEquals(4, RopaSheetFields.DATA_SUBJECT_VOLUMES.size());
        assertTrue(RopaSheetFields.FREQUENCIES.contains("Ad hoc"));
        assertTrue(RopaSheetFields.FREQUENCIES.contains("One off"));
    }

    @Test
    void acceptsSheetValuesOnFullSave() {
        assertDoesNotThrow(() -> validate(false,
                "[\"Name\",\"Passport\"]",
                "[\"Biometric data\"]",
                "[\"Employees\",\"Other individuals e.g., Third party service providers\"]",
                "1,000-100,000", "Quarterly",
                "[\"CONSENT\",\"CONTRACT_PERFORMANCE\"]",
                true, "External payroll provider"));
    }

    @Test
    void rejectsValueOutsideSheetDropdown() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validate(true, "[\"Name\",\"Fingerprint\"]", null, null, null, null, null, false, null));
        assertTrue(ex.getReason().contains("List of Personal Data"));
    }

    @Test
    void rejectsUnknownVolumeAndFrequency() {
        assertThrows(ResponseStatusException.class,
                () -> validate(true, null, null, null, "500", null, null, false, null));
        assertThrows(ResponseStatusException.class,
                () -> validate(true, null, null, null, null, "Hourly", null, false, null));
    }

    @Test
    void rejectsUnknownLegalBasis() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> validate(true, null, null, null, null, null, "[\"LEGITIMATE_INTERESTS\"]", false, null));
        assertTrue(ex.getReason().contains("Legal basis for processing"));
    }

    @Test
    void fullSaveRequiresExternalDetailWhenYes() {
        assertThrows(ResponseStatusException.class,
                () -> validate(false,
                        "[\"Name\"]", "[\"Biometric data\"]", "[\"Employees\"]",
                        "0-1000", "Daily", "[\"CONSENT\"]", true, " "));
    }

    @Test
    void draftAllowsBlankFields() {
        assertDoesNotThrow(() -> validate(true, null, "", null, null, null, null, null, null));
    }

    @Test
    void fullSaveRejectsBlankRequiredFields() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> RopaSheetFields.validateContributorFields(
                        null, "[\"Name\"]", "[\"Biometric data\"]", "[\"Employees\"]",
                        "0-1000", "Daily", "Source", "Method", "[\"CONSENT\"]",
                        "Internal", false, null, "UAE", "Yes", "Clauses", "System", "12m", "Sec", false));
        assertTrue(ex.getReason().contains("Description of processing"));
    }
}
