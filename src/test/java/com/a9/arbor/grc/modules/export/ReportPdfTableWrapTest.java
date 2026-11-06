package com.a9.arbor.grc.modules.export;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportPdfTableWrapTest {

    @Test
    void findingsColumnsKeepTheFullDomainAndTitle() throws Exception {
        String domain = "Network & Application Security";
        String title = "D5-01 - Encryption of Organization records in transit and at rest";
        byte[] pdf = ReportPdfSupport.write("test", "TPRM Findings Tracker", "Risk Admin", "CONFIDENTIAL", page -> {
            page.writeHeading("TPRM Findings Tracker");
            page.writeTable(
                    new String[]{"Vendor", "Control ID", "Domain", "Title", "Status", "Due"},
                    List.of(
                            new String[]{"Kishore", "D5-01", domain, title, "CLOSED", "2028-09-06"},
                            new String[]{"ABC Corp -2", "D2-03", "Data Protection & Privacy",
                                    "D2-03 - Least privilege and separation of duties for administrators",
                                    "OPEN", "2028-08-26"}),
                    new float[]{1.2f, 1.05f, 1.65f, 2.55f, 0.7f, 0.95f});
        });
        Files.createDirectories(Path.of("target"));
        Files.write(Path.of("target/tprm-findings-sample.pdf"), pdf);
        String text = text(pdf).replaceAll("\\s+", " ");
        assertTrue(text.contains(domain), text);
        assertTrue(text.contains("Encryption of Organization records in transit and at rest"), text);
        assertTrue(text.contains("Data Protection & Privacy"), text);
        assertTrue(text.contains("Least privilege and separation of duties for administrators"), text);
        assertTrue(text.contains("Control ID"), text);
        assertFalse(text.contains("..."), text);
    }

    private static String text(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
