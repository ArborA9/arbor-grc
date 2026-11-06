package com.a9.arbor.grc.modules.export;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutiveSummaryPdfTest {

    @Test
    void internalBoardPackKeepsAttentionFiguresAndFrameworkTable() throws Exception {
        byte[] pdf = ExecutiveSummaryPdf.write(
                "Villa 515", "Amina Khan <amina@villa.test>", false, sample(6), "CONFIDENTIAL");
        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target/executive-internal.pdf"), pdf);
        String text = text(pdf);
        assertTrue(text.contains("Executive GRC Summary"));
        assertTrue(text.contains("Internal board pack"));
        assertTrue(text.contains("Villa 515"));
        assertTrue(text.contains("Overall compliance"));
        assertTrue(text.contains("72.5%"));
        assertTrue(text.contains("ISO/IEC 27001"));
        assertTrue(text.contains("UAE IA latest score"));
        assertTrue(text.contains("Critical risks"));
        assertTrue(text.contains("High-risk vendors"));
        assertTrue(text.contains("Open audit findings"));
        assertFalse(text.contains("NCEMA"));
        assertFalse(text.contains("Open DSRs"));
    }

    @Test
    void externalSummaryOmitsTreatmentFindingsAndVendors() throws Exception {
        byte[] pdf = ExecutiveSummaryPdf.write(
                "Villa 515", "Amina Khan", true, sample(4), "CONFIDENTIAL");
        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target/executive-external.pdf"), pdf);
        String text = text(pdf);
        assertTrue(text.contains("External summary"));
        assertTrue(text.contains("Open CAPAs"));
        assertTrue(text.contains("Treatment status, findings, and vendor posture are omitted."));
        assertFalse(text.contains("Critical risks"));
        assertFalse(text.contains("Overdue treatments"));
        assertFalse(text.contains("High-risk vendors"));
        assertFalse(text.contains("Open audit findings"));
    }

    @Test
    void longFrameworkListContinuesOnAnotherPage() throws Exception {
        byte[] pdf = ExecutiveSummaryPdf.write("Arbor", "Owner", false, sample(40), "CONFIDENTIAL");
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertTrue(doc.getNumberOfPages() >= 2);
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("Page 1 of "));
            assertTrue(text.contains("Page 2 of "));
            assertTrue(text.contains("FW-40"));
        }
    }

    private static String text(byte[] pdf) throws Exception {
        assertTrue(pdf.length > 800);
        assertTrue(pdf[0] == '%' && pdf[1] == 'P');
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private static ExecutiveSummaryBrief sample(int frameworks) {
        List<ExecutiveSummaryBrief.FrameworkScore> rows = new ArrayList<>();
        for (int i = 1; i <= frameworks; i++) {
            String code = "FW-" + i;
            String name = i == 1 ? "ISO/IEC 27001" : "Framework " + i;
            rows.add(new ExecutiveSummaryBrief.FrameworkScore(code, name, (60 + (i % 30)) + ".0%"));
        }
        return new ExecutiveSummaryBrief(
                "72.5%",
                "81.0%",
                "UAE-IA",
                "12",
                "4",
                "3",
                "2",
                "7",
                "1",
                "5",
                "2",
                "64%",
                rows);
    }
}
