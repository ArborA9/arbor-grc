package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.risk.domain.RiskCategory;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskBulkImportWorkbookTest {

    private static final List<RiskLevel> FIVE = List.of(RiskLevel.values());
    private static final List<RiskLevel> FOUR =
            List.of(RiskLevel.VERY_LOW, RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH);
    private static final List<RiskLevel> THREE = List.of(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH);

    @Test
    void parseLevelOnFiveByFiveAcceptsOneToFiveAndNames() {
        assertEquals(RiskLevel.VERY_LOW, RiskBulkImportWorkbook.parseLevel("1", FIVE));
        assertEquals(RiskLevel.MEDIUM, RiskBulkImportWorkbook.parseLevel("3", FIVE));
        assertEquals(RiskLevel.VERY_HIGH, RiskBulkImportWorkbook.parseLevel("5", FIVE));
        assertEquals(RiskLevel.MEDIUM, RiskBulkImportWorkbook.parseLevel("3.0", FIVE));
        assertEquals(RiskLevel.MEDIUM, RiskBulkImportWorkbook.parseLevel("MEDIUM", FIVE));
        assertEquals(RiskLevel.VERY_HIGH, RiskBulkImportWorkbook.parseLevel("Very High", FIVE));
        assertEquals(RiskLevel.VERY_LOW, RiskBulkImportWorkbook.parseLevel("Ineffective", FIVE));
        assertEquals(RiskLevel.VERY_HIGH, RiskBulkImportWorkbook.parseLevel("very strong", FIVE));
        assertNull(RiskBulkImportWorkbook.parseLevel("", FIVE));
        assertThrows(IllegalArgumentException.class, () -> RiskBulkImportWorkbook.parseLevel("6", FIVE));
        assertThrows(IllegalArgumentException.class, () -> RiskBulkImportWorkbook.parseLevel("Huge", FIVE));
    }

    @Test
    void parseLevelOnThreeByThreeUsesPositionOnTenantScale() {
        assertEquals(RiskLevel.LOW, RiskBulkImportWorkbook.parseLevel("1", THREE));
        assertEquals(RiskLevel.MEDIUM, RiskBulkImportWorkbook.parseLevel("2", THREE));
        assertEquals(RiskLevel.HIGH, RiskBulkImportWorkbook.parseLevel("3", THREE));
        assertEquals(RiskLevel.HIGH, RiskBulkImportWorkbook.parseLevel("High", THREE));
        assertEquals(RiskLevel.HIGH, RiskBulkImportWorkbook.parseLevel("Strong", THREE));
        assertThrows(IllegalArgumentException.class, () -> RiskBulkImportWorkbook.parseLevel("4", THREE));
        assertThrows(IllegalArgumentException.class, () -> RiskBulkImportWorkbook.parseLevel("Very High", THREE));
        assertThrows(IllegalArgumentException.class, () -> RiskBulkImportWorkbook.parseLevel("Ineffective", THREE));
    }

    @Test
    void parseLevelOnFourByFour() {
        assertEquals(RiskLevel.VERY_LOW, RiskBulkImportWorkbook.parseLevel("1", FOUR));
        assertEquals(RiskLevel.HIGH, RiskBulkImportWorkbook.parseLevel("4", FOUR));
        assertThrows(IllegalArgumentException.class, () -> RiskBulkImportWorkbook.parseLevel("5", FOUR));
    }

    @Test
    void threeByThreeTemplateUsesPlainHeadersAndTenantNames() throws Exception {
        RiskBulkImportWorkbook wb = new RiskBulkImportWorkbook();
        ExportFile file = wb.buildTemplate(RiskType.IS_RISK, List.of("Finance"), List.of(), List.of("owner@x.local"),
                List.of(), List.of(), List.of(), List.of("Phishing"), "P", 3, THREE);
        try (XSSFWorkbook x = new XSSFWorkbook(new ByteArrayInputStream(file.bytes()))) {
            var header = x.getSheet("Risks").getRow(0);
            List<String> headers = new java.util.ArrayList<>();
            for (int i = 0; i < header.getLastCellNum(); i++) headers.add(header.getCell(i).getStringCellValue());
            assertTrue(headers.contains("Likelihood"), headers.toString());
            assertTrue(headers.contains("Control Effectiveness"), headers.toString());
            assertTrue(headers.stream().noneMatch(h -> h.contains("(1-5)")), headers.toString());
            var ref = x.getSheet("Reference");
            assertEquals("Low", ref.getRow(1).getCell(4).getStringCellValue());
            assertEquals("High", ref.getRow(3).getCell(4).getStringCellValue());
            assertEquals("Weak", ref.getRow(1).getCell(11).getStringCellValue());
            assertEquals("Strong", ref.getRow(3).getCell(11).getStringCellValue());
            var r4 = ref.getRow(4);
            assertTrue(r4 == null || r4.getCell(4) == null);
        }
        var sample = wb.parse(file.bytes(), RiskType.IS_RISK).get(0);
        assertEquals(RiskLevel.MEDIUM, RiskBulkImportWorkbook.parseLevel(sample.probability(), THREE));
        assertEquals(RiskLevel.MEDIUM, RiskBulkImportWorkbook.parseLevel(sample.impact(), THREE));
        assertEquals(RiskLevel.MEDIUM, RiskBulkImportWorkbook.parseLevel(sample.controlEffectiveness(), THREE));
    }

    @Test
    void isTemplateHeadersCanonicalizeToRequiredKeys() {
        assertEquals("title", RiskBulkImportWorkbook.canonicalHeader("Risk Title"));
        assertEquals("observed_gap", RiskBulkImportWorkbook.canonicalHeader("Gap / Observation"));
        assertEquals("org_unit_name", RiskBulkImportWorkbook.canonicalHeader("Division / Department"));
        assertEquals("threats", RiskBulkImportWorkbook.canonicalHeader("Associated Threat"));
        assertEquals("risk_owner_email", RiskBulkImportWorkbook.canonicalHeader("Risk Owner (email)"));
        assertEquals("probability", RiskBulkImportWorkbook.canonicalHeader("Likelihood"));
        assertEquals("date_identified", RiskBulkImportWorkbook.canonicalHeader("Date Identified"));
        assertEquals("node_code", RiskBulkImportWorkbook.canonicalHeader("Control Ref"));
        assertEquals("display_id_override", RiskBulkImportWorkbook.canonicalHeader("Risk ID"));
        assertEquals("category", RiskBulkImportWorkbook.canonicalHeader("Risk Category"));
        assertEquals("cause", RiskBulkImportWorkbook.canonicalHeader("Risk Cause / Source"));
        assertEquals("treatment_justification", RiskBulkImportWorkbook.canonicalHeader("Treatment Action Plan"));
        assertEquals("treatment_justification", RiskBulkImportWorkbook.canonicalHeader("Treatment Description"));
        assertEquals("strategic_objective", RiskBulkImportWorkbook.canonicalHeader("Strategic Objective (Manual)"));
        assertEquals("probability", RiskBulkImportWorkbook.canonicalHeader("Likelihood (1-5)"));
        assertEquals("contributor_email", RiskBulkImportWorkbook.canonicalHeader("Risk Custodian"));
        assertEquals("risk_owner_email", RiskBulkImportWorkbook.canonicalHeader("Risk Owner"));
        assertEquals("display_id_override", RiskBulkImportWorkbook.canonicalHeader("display_id_override"));
    }

    @Test
    void parseCategoryAcceptsOnlyCreateFormCategories() {
        assertEquals(RiskCategory.IT, RiskBulkImportWorkbook.parseCategory("it"));
        assertEquals(RiskCategory.REPUTATIONAL, RiskBulkImportWorkbook.parseCategory(" Reputational "));
        assertEquals(RiskCategory.OTHER, RiskBulkImportWorkbook.parseCategory("Other"));
        IllegalArgumentException legal = assertThrows(IllegalArgumentException.class,
                () -> RiskBulkImportWorkbook.parseCategory("Legal"));
        assertTrue(legal.getMessage().startsWith("Risk Category \"Legal\" is not valid"), legal.getMessage());
        assertThrows(IllegalArgumentException.class, () -> RiskBulkImportWorkbook.parseCategory("Environmental"));
    }

    @Test
    void templateSampleRowIsMarkedAndIgnoredOnParse() throws Exception {
        RiskBulkImportWorkbook wb = new RiskBulkImportWorkbook();
        ExportFile file = wb.buildTemplate(RiskType.ERM, List.of("Finance"), List.of(), List.of("owner@x.local"),
                List.of(), List.of(), List.of(), List.of(), "P", 5, FIVE);
        try (XSSFWorkbook x = new XSSFWorkbook(new ByteArrayInputStream(file.bytes()))) {
            assertEquals("SAMPLE", x.getSheet("Risks").getRow(1).getCell(0).getStringCellValue());
            List<String> cats = new java.util.ArrayList<>();
            for (int r = 1; r <= x.getSheet("Reference").getLastRowNum(); r++) {
                var cell = x.getSheet("Reference").getRow(r).getCell(3);
                if (cell != null && !cell.getStringCellValue().isBlank()) cats.add(cell.getStringCellValue());
            }
            assertEquals(List.of("Strategic", "Operational", "Financial", "Compliance", "IT", "Reputational", "Other"),
                    cats);
        }
        List<RiskBulkImportWorkbook.ParsedRiskRow> rows = wb.parse(file.bytes(), RiskType.ERM);
        assertEquals(1, rows.size());
        assertTrue(RiskBulkImportWorkbook.isSampleRow(rows.get(0)));
    }
}
