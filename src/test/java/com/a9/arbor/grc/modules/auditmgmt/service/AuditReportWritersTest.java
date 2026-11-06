package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.AuditMode;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistConclusion;
import com.a9.arbor.grc.modules.auditmgmt.domain.FindingSeverity;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditStatus;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditReportWritersTest {

    @Test
    void pdfAndExcelIncludeTheWorkingPaperAndSurvivePunctuation() throws Exception {
        AuditReportData data = sample();
        byte[] pdf = new AuditReportPdfWriter().write(data);
        assertTrue(pdf.length > 1500);
        assertEquals('%', (char) pdf[0]);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertTrue(doc.getNumberOfPages() >= 2);
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("INTERNAL AUDIT REPORT"), text.substring(0, Math.min(500, text.length())));
            assertTrue(text.contains("AUD-2026-006"));
            assertTrue(text.contains("Multiple audit Test"));
            assertTrue(text.contains("Executive summary"));
            assertTrue(text.contains("controls - evidence"));
            assertTrue(text.contains("Communication"));
            assertTrue(text.contains("F-001"));
            assertTrue(text.contains("Page 1 of"));
            assertTrue(text.contains("Classification: CONFIDENTIAL"));
        }

        byte[] xlsx = new AuditReportExcelWriter().write(data);
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertTrue(wb.getSheet("Engagement") != null);
            assertTrue(wb.getSheet("Team") != null);
            assertTrue(wb.getSheet("Checklist") != null);
            assertTrue(wb.getSheet("Findings") != null);
            String summary = null;
            var engagement = wb.getSheet("Engagement");
            for (int i = 1; i <= engagement.getLastRowNum(); i++) {
                var row = engagement.getRow(i);
                if (row != null && row.getCell(0) != null
                        && "Executive summary".equals(row.getCell(0).getStringCellValue())) {
                    summary = row.getCell(1).getStringCellValue();
                }
            }
            assertTrue(summary != null && summary.contains("\u2013"), summary);
            assertTrue(summary.contains("\u2026"));
            var header = wb.getSheet("Checklist").getRow(0);
            assertEquals("Clause / Ref", header.getCell(1).getStringCellValue());
            assertEquals("Test focus", header.getCell(4).getStringCellValue());
            assertEquals("Evidence source", header.getCell(6).getStringCellValue());
            assertEquals("Finding description", header.getCell(11).getStringCellValue());
            assertEquals("Auditor note", header.getCell(12).getStringCellValue());
            String focus = wb.getSheet("Checklist").getRow(1).getCell(4).getStringCellValue();
            assertTrue(focus.contains("end-to-end"));
            String description = wb.getSheet("Findings").getRow(1).getCell(5).getStringCellValue();
            assertTrue(description.length() > 120);
            assertTrue(description.contains("\u2014"));
        }
    }

    private static AuditReportData sample() {
        GrcAudit audit = new GrcAudit();
        audit.setId(8L);
        audit.setDisplayId("AUD-2026-006");
        audit.setName("Multiple audit Test");
        audit.setAuditType(AuditType.INTERNAL);
        audit.setAuditMode(AuditMode.COMPLIANCE);
        audit.setStatus(GrcAuditStatus.CLOSED);
        audit.setFrameworkCode("MULTIPLE");
        audit.setFrameworkName("Multiple");
        audit.setFrameworkVersion("");
        audit.setScopeNotes("Head office and the service desk.");
        audit.setExecutiveSummary("Reviewed the service management system. Scope: controls \u2013 evidence examined\u2026");
        audit.setLeadAuditorUserId(3L);

        GrcAuditChecklistItem line = new GrcAuditChecklistItem();
        line.setId(11L);
        line.setChecklistId(1L);
        line.setNodeCode("7.4");
        line.setTitle("Communication");
        line.setSection("7 Support");
        line.setTestFocus("Internal and external communication relevant to the SMS is determined and carried out, end-to-end.");
        line.setSoaApplicability("Mandatory clause");
        line.setEvidenceModule("Gap Assessment (ISO 20000) - Policy library");
        line.setInScope(true);
        line.setConclusion(ChecklistConclusion.MINOR_NC);
        line.setFindingDescription("The communication procedure is not current.");
        line.setNotes("Interviewed the service owner.");
        line.setEvidenceExamined("Procedure SMS-COM-01");
        line.setPersonsInterviewed("Service owner");
        line.setFindingId(21L);

        GrcAuditFinding finding = new GrcAuditFinding();
        finding.setId(21L);
        finding.setAuditId(8L);
        finding.setChecklistItemId(11L);
        finding.setDisplayCode("F-001");
        finding.setSeverity(FindingSeverity.MINOR_NC);
        finding.setTitle("Communication procedure is out of date");
        finding.setDescription("The published procedure still names a retired mailbox. "
                + "Owners were not told \u2014 the replacement channel is not in the register. "
                + "x".repeat(180));
        finding.setRecommendedAction("Update the procedure and tell the service owners.");
        finding.setControlRef("7.4");
        finding.setCreatedAt(Instant.parse("2026-09-26T10:00:00Z"));

        StringBuilder longFocus = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            longFocus.append("Control ").append(i).append(" asks for evidence of planning, operation, and review. ");
        }
        GrcAuditChecklistItem filler = new GrcAuditChecklistItem();
        filler.setId(12L);
        filler.setChecklistId(1L);
        filler.setNodeCode("8.1");
        filler.setTitle("Operational planning and control");
        filler.setTestFocus(longFocus.toString());
        filler.setInScope(true);

        return new AuditReportData(
                "Engagement A",
                "Admin User (admin@example.com)",
                "CONFIDENTIAL",
                Instant.parse("2026-09-27T06:00:00Z"),
                audit,
                "Jeet Auditor",
                "jeet@example.com",
                "Head office",
                "Jeet Auditor (jeet@example.com)",
                "Admin User (admin@example.com)",
                List.of(new AuditReportData.TeamMember("Jeet Auditor", "jeet@example.com", "Lead auditor")),
                List.of("Assets", "BCMS", "SoA"),
                0,
                List.of(new AuditReportData.ChecklistSheet("ISO 20000", List.of(line, filler))),
                List.of(new AuditReportData.FindingLine(finding, "7.4", "CAPA-008 (Open)")),
                List.of(),
                true);
    }
}
