package com.a9.arbor.grc.modules.bcms.service;

import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlan;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanProcedure;
import com.a9.arbor.grc.modules.export.ReportPdfTableRenderer;
import com.a9.arbor.grc.modules.iam.domain.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Simple text-only PDF export for a recovery plan (mirrors {@code AuditReportPdfWriter}'s layout, without report branding config). */
@Component
public class RecoveryPlanPdfWriter {

    private static final float MARGIN = 50f;
    private static final float LINE = 14f;
    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    public byte[] write(String orgName, RecoveryPlan plan, User owner, List<BiaProcess> linkedBias,
                        List<RecoveryPlanProcedure> procedures) {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PageWriter page = new PageWriter(doc, bodyFont);
            page.newPage();
            page.writeTitle(titleFont, "Recovery Plan");
            page.blank();
            page.writeLine("Organisation: " + safe(orgName));
            page.writeLine("Plan: " + safe(plan.getName()) + " (v" + plan.getVersionNum() + ")");
            page.writeLine("Type: " + plan.getPlanType());
            page.writeLine("Status: " + plan.getStatus());
            if (plan.getReviewDate() != null) {
                page.writeLine("Review date: " + plan.getReviewDate());
            }
            page.writeLine("Generated: " + DT.format(Instant.now()));
            page.blank();

            if (plan.getScope() != null) {
                page.writeHeading(titleFont, "Scope");
                page.writeParagraph(plan.getScope());
                page.blank();
            }
            if (plan.getCrisisTeam() != null) {
                page.writeHeading(titleFont, "Crisis Management Team");
                page.writeParagraph(plan.getCrisisTeam());
                page.blank();
            }
            if (plan.getEscalationContacts() != null) {
                page.writeHeading(titleFont, "Escalation Contacts");
                page.writeParagraph(plan.getEscalationContacts());
                page.blank();
            }
            if (plan.getCommunicationTree() != null) {
                page.writeHeading(titleFont, "Communication Tree");
                page.writeParagraph(plan.getCommunicationTree());
                page.blank();
            }
            if (plan.getResourceRequirements() != null) {
                page.writeHeading(titleFont, "Resource Requirements");
                page.writeParagraph(plan.getResourceRequirements());
                page.blank();
            }
            if (plan.getAlternateSiteDetails() != null) {
                page.writeHeading(titleFont, "Alternate Site");
                page.writeParagraph(plan.getAlternateSiteDetails());
                page.blank();
            }

            page.newPage();
            page.writeHeading(titleFont, "Linked Business Impact Analyses");
            if (linkedBias.isEmpty()) {
                page.writeLine("None linked.");
            } else {
                List<String[]> rows = new ArrayList<>();
                for (BiaProcess bia : linkedBias) {
                    rows.add(new String[]{
                            safe(bia.getProcessName()),
                            bia.getCategoryLabel() != null ? bia.getCategoryLabel() : "-",
                            safe(bia.getRtoHours()),
                            safe(bia.getMaoHours())
                    });
                }
                page.writeTable(
                        new String[]{"Process", "Criticality", "RTO (h)", "MAO (h)"},
                        rows,
                        new float[]{2.4f, 1.2f, 0.9f, 0.9f});
            }
            page.blank();

            page.writeHeading(titleFont, "Recovery Procedures");
            if (procedures.isEmpty()) {
                page.writeLine("No procedures documented.");
            } else {
                List<String[]> rows = new ArrayList<>();
                for (RecoveryPlanProcedure proc : procedures) {
                    rows.add(new String[]{
                            safe(proc.getProcessName()),
                            truncate(proc.getProcedureText(), 200)
                    });
                }
                page.writeTable(
                        new String[]{"Process", "Procedure"},
                        rows,
                        new float[]{1.4f, 3.6f});
            }

            page.newPage();
            page.writeHeading(titleFont, "Approval");
            page.blank();
            page.writeLine("Owner: " + signerName(owner));
            page.writeLine("Approved: " + fmtInstant(plan.getApprovedAt()));
            page.closeStream();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate recovery plan PDF", e);
        }
    }

    private static String signerName(User user) {
        if (user == null) {
            return "-";
        }
        return user.getFullName() + " <" + user.getEmail() + ">";
    }

    private static String fmtInstant(Instant instant) {
        return instant == null ? "-" : DT.format(instant);
    }

    private static String safe(String s) {
        return s == null ? "-" : s;
    }

    private static String safe(Integer i) {
        return i == null ? "-" : i.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private final class PageWriter {
        private final PDDocument doc;
        private final PDType1Font bodyFont;
        private final PDType1Font headingFont;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        PageWriter(PDDocument doc, PDType1Font bodyFont) {
            this.doc = doc;
            this.bodyFont = bodyFont;
            this.headingFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        }

        void newPage() throws IOException {
            closeStream();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = page.getMediaBox().getHeight() - MARGIN;
        }

        void writeTitle(PDType1Font font, String text) throws IOException {
            stream.beginText();
            stream.setFont(font, 18);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(text);
            stream.endText();
            y -= LINE * 2;
        }

        void writeHeading(PDType1Font font, String text) throws IOException {
            stream.beginText();
            stream.setFont(font, 14);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(text);
            stream.endText();
            y -= LINE * 1.5f;
        }

        void writeLine(String text) throws IOException {
            ensureSpace(1);
            stream.beginText();
            stream.setFont(bodyFont, 10);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(truncate(text, 110));
            stream.endText();
            y -= LINE;
        }

        void writeParagraph(String text) throws IOException {
            for (String line : text.split("\\R")) {
                writeLine(line);
            }
        }

        void blank() {
            y -= LINE / 2;
        }

        void writeTable(String[] headers, List<String[]> rows, float[] colWeights) throws IOException {
            ReportPdfTableRenderer.TableCursor cursor =
                    new ReportPdfTableRenderer.TableCursor(page, stream, y);
            cursor = ReportPdfTableRenderer.draw(
                    cursor,
                    MARGIN,
                    MARGIN + 20,
                    bodyFont,
                    headingFont,
                    headers,
                    rows,
                    colWeights,
                    () -> {
                        newPage();
                        return new ReportPdfTableRenderer.TableCursor(page, stream, y);
                    });
            y = cursor.y();
        }

        void ensureSpace(int lines) throws IOException {
            if (y - lines * LINE < MARGIN + 20) {
                newPage();
            }
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }
    }
}
