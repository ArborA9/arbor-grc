package com.a9.arbor.grc.modules.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Board-pack layout for the Executive GRC Summary. Internal keeps treatment,
 * finding, and vendor figures. External is the shorter copy.
 */
public final class ExecutiveSummaryPdf {

    private static final float MARGIN = 36f;
    private static final float HEADER_H = 102f;
    private static final float CARD_H = 74f;
    private static final float CARD_GAP = 8f;
    private static final float ROW_H = 20f;
    private static final float FOOTER_LIMIT = 52f;
    private static final DateTimeFormatter WHEN = DateTimeFormatter
            .ofPattern("d MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    private static final float[] GOLD = rgb(184, 134, 11);
    private static final float[] INK = rgb(28, 25, 23);
    private static final float[] WHITE = rgb(255, 255, 255);
    private static final float[] SAND = rgb(250, 247, 242);
    private static final float[] SAND_DEEP = rgb(245, 237, 216);
    private static final float[] MUTED = rgb(82, 82, 91);
    private static final float[] HAIR = rgb(214, 206, 194);

    private ExecutiveSummaryPdf() {
    }

    public static byte[] write(String orgName,
                               String preparedBy,
                               boolean external,
                               ExecutiveSummaryBrief brief,
                               String classification) {
        try (PDDocument doc = new PDDocument()) {
            String title = external ? "Executive GRC Summary (External)" : "Executive GRC Summary (Internal)";
            doc.getDocumentInformation().setTitle(title);
            doc.getDocumentInformation().setAuthor(PdfText.oneLine(dash(orgName)));

            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            Canvas canvas = new Canvas(doc, regular, bold, orgName, preparedBy, external, brief);
            canvas.draw();
            canvas.closeStream();
            canvas.stampFooters(classification);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate Executive GRC Summary", e);
        }
    }

    private static float[] rgb(int r, int g, int b) {
        return new float[]{r / 255f, g / 255f, b / 255f};
    }

    private static String dash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String one = PdfText.oneLine(value);
        return one.isEmpty() ? "-" : one;
    }

    private static final class Canvas {
        private final PDDocument doc;
        private final PDType1Font regular;
        private final PDType1Font bold;
        private final String orgName;
        private final String preparedBy;
        private final boolean external;
        private final ExecutiveSummaryBrief brief;
        private final String generatedAt;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        Canvas(PDDocument doc, PDType1Font regular, PDType1Font bold,
               String orgName, String preparedBy, boolean external, ExecutiveSummaryBrief brief) {
            this.doc = doc;
            this.regular = regular;
            this.bold = bold;
            this.orgName = dash(orgName);
            this.preparedBy = dash(preparedBy);
            this.external = external;
            this.brief = brief;
            this.generatedAt = WHEN.format(Instant.now());
        }

        void draw() throws IOException {
            newPage(true);
            kpiRow(
                    tile("Overall compliance", brief.overallCompliance(),
                            "-".equals(brief.overallCompliance()) ? "None scored" : "Scored assessments"),
                    tile("Open IS risks", brief.openIsRisks(), "Not closed"),
                    tile("Open ERM risks", brief.openErmRisks(), "Not closed"),
                    tile("BIA approved", brief.biaApproved(), "Verified processes"));
            y -= 10f;
            if (external) {
                metricStrip(
                        pair("Open CAPAs", brief.openCapas()),
                        pair("Overdue CAPAs", brief.overdueCapas()));
                y -= 8f;
                text("External summary. Treatment status, findings, and vendor posture are omitted.",
                        regular, 8f, MUTED, 12f);
            } else {
                kpiRow(
                        tile("Critical risks", brief.criticalRisks(), "High or very high"),
                        tile("Overdue treatments", brief.overdueTreatments(), "Past due"),
                        tile("Open CAPAs", brief.openCapas(), "Not closed"),
                        tile("Overdue CAPAs", brief.overdueCapas(), "Past due"));
                y -= 10f;
                metricStrip(
                        pair("Open audit findings", brief.openFindings()),
                        pair("High-risk vendors", brief.highRiskVendors()));
            }
            y -= 16f;
            uaeCallout();
            y -= 16f;
            sectionTitle("Compliance by framework");
            tableHeader();
            if (brief.frameworks().isEmpty()) {
                ensure(ROW_H);
                text("No scored assessments yet.", regular, 9f, MUTED, ROW_H);
            } else {
                boolean zebra = false;
                for (ExecutiveSummaryBrief.FrameworkScore row : brief.frameworks()) {
                    if (y - ROW_H < FOOTER_LIMIT) {
                        newPage(false);
                        tableHeader();
                    }
                    tableRow(row, zebra);
                    zebra = !zebra;
                }
            }
        }

        private void newPage(boolean cover) throws IOException {
            closeStream();
            page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();
            fill(0, 0, pageW, pageH, SAND);
            if (cover) {
                fill(0, pageH - HEADER_H, pageW, HEADER_H, INK);
                fill(0, pageH - 5f, pageW, 5f, GOLD);
                draw(MARGIN, pageH - 28f, "ARBOR GRC", bold, 8f, GOLD);
                draw(MARGIN, pageH - 52f, "Executive GRC Summary", bold, 18f, WHITE);
                draw(MARGIN, pageH - 72f,
                        external ? "External summary" : "Internal board pack",
                        regular, 9f, SAND_DEEP);
                float right = pageW - MARGIN;
                rightText(generatedAt, regular, 8f, SAND_DEEP, right, pageH - 30f, 220f);
                rightText(orgName, bold, 9f, WHITE, right, pageH - 48f, 230f);
                rightText(preparedBy, regular, 8f, SAND_DEEP, right, pageH - 66f, 230f);
                y = pageH - HEADER_H - 16f;
            } else {
                fill(0, pageH - 4f, pageW, 4f, GOLD);
                draw(MARGIN, pageH - 22f, "Executive GRC Summary", bold, 9f, INK);
                rightText(orgName, regular, 8f, MUTED, pageW - MARGIN, pageH - 21f, 220f);
                y = pageH - 36f;
            }
        }

        private void kpiRow(Tile a, Tile b, Tile c, Tile d) throws IOException {
            float width = contentWidth();
            float cardW = (width - 3 * CARD_GAP) / 4f;
            float top = y;
            float bottom = top - CARD_H;
            Tile[] tiles = {a, b, c, d};
            for (int i = 0; i < tiles.length; i++) {
                float x = MARGIN + i * (cardW + CARD_GAP);
                card(x, bottom, cardW, tiles[i]);
            }
            y = bottom;
        }

        private void card(float x, float bottom, float cardW, Tile tile) throws IOException {
            fill(x, bottom, cardW, CARD_H, WHITE);
            strokeRect(x, bottom, cardW, CARD_H, HAIR);
            fill(x, bottom + CARD_H - 3f, cardW, 3f, GOLD);
            float inner = cardW - 16f;
            draw(x + 8f, bottom + CARD_H - 18f, fit(tile.label(), regular, 7.5f, inner), regular, 7.5f, MUTED);
            draw(x + 8f, bottom + 30f, fit(tile.value(), bold, 16f, inner), bold, 16f, INK);
            draw(x + 8f, bottom + 12f, fit(tile.caption(), regular, 7f, inner), regular, 7f, MUTED);
        }

        private void metricStrip(Pair left, Pair right) throws IOException {
            float h = 36f;
            float bottom = y - h;
            float width = contentWidth();
            fill(MARGIN, bottom, width, h, WHITE);
            strokeRect(MARGIN, bottom, width, h, HAIR);
            fill(MARGIN, bottom, 3f, h, GOLD);
            float mid = MARGIN + width / 2f;
            stripPair(MARGIN + 14f, bottom, left);
            stripPair(mid + 8f, bottom, right);
            y = bottom;
        }

        private void stripPair(float x, float bottom, Pair pair) throws IOException {
            draw(x, bottom + 20f, fit(pair.label(), regular, 7.5f, 180f), regular, 7.5f, MUTED);
            draw(x, bottom + 8f, fit(pair.value(), bold, 11f, 160f), bold, 11f, INK);
        }

        private void uaeCallout() throws IOException {
            float h = 44f;
            ensure(h + 4f);
            float bottom = y - h;
            float width = contentWidth();
            fill(MARGIN, bottom, width, h, WHITE);
            strokeRect(MARGIN, bottom, width, h, HAIR);
            fill(MARGIN, bottom, 3f, h, GOLD);
            draw(MARGIN + 14f, bottom + 28f, "UAE IA latest score", regular, 8f, MUTED);
            draw(MARGIN + 14f, bottom + 12f, fit(brief.uaeIaScore(), bold, 13f, 120f), bold, 13f, INK);
            String code = brief.uaeIaCode() == null || brief.uaeIaCode().isBlank() || "-".equals(brief.uaeIaScore())
                    ? "No scored UAE IA assessment"
                    : brief.uaeIaCode();
            rightText(code, regular, 8f, MUTED, MARGIN + width - 12f, bottom + 16f, 220f);
            y = bottom;
        }

        private void sectionTitle(String title) throws IOException {
            ensure(28f);
            draw(MARGIN, y - 12f, title, bold, 11f, INK);
            strokeH(MARGIN, y - 18f, 36f, GOLD);
            y -= 28f;
        }

        private void tableHeader() throws IOException {
            ensure(ROW_H + 4f);
            float bottom = y - ROW_H;
            float width = contentWidth();
            fill(MARGIN, bottom, width, ROW_H, SAND_DEEP);
            float codeX = MARGIN + width * 0.62f;
            float scoreRight = MARGIN + width - 8f;
            draw(MARGIN + 8f, bottom + 6f, "Framework", bold, 8f, INK);
            draw(codeX, bottom + 6f, "Code", bold, 8f, INK);
            rightText("Score", bold, 8f, INK, scoreRight, bottom + 6f, 70f);
            y = bottom;
        }

        private void tableRow(ExecutiveSummaryBrief.FrameworkScore row, boolean zebra) throws IOException {
            float bottom = y - ROW_H;
            float width = contentWidth();
            if (zebra) {
                fill(MARGIN, bottom, width, ROW_H, WHITE);
            }
            strokeH(MARGIN, bottom, width, HAIR);
            float codeX = MARGIN + width * 0.62f;
            float nameW = width * 0.62f - 16f;
            float codeW = width * 0.22f;
            draw(MARGIN + 8f, bottom + 6f, fit(dash(row.name()), regular, 9f, nameW), regular, 9f, INK);
            draw(codeX, bottom + 6f, fit(dash(row.code()), regular, 8f, codeW), regular, 8f, MUTED);
            rightText(dash(row.score()), bold, 9f, INK, MARGIN + width - 8f, bottom + 6f, 70f);
            y = bottom;
        }

        private void ensure(float needed) throws IOException {
            if (y - needed < FOOTER_LIMIT) {
                newPage(false);
            }
        }

        private float contentWidth() {
            return page.getMediaBox().getWidth() - 2 * MARGIN;
        }

        private void text(String value, PDType1Font font, float size, float[] color, float leading) throws IOException {
            draw(MARGIN, y - size, PdfText.oneLine(value), font, size, color);
            y -= leading;
        }

        private void draw(float x, float baseline, String value, PDType1Font font, float size, float[] color)
                throws IOException {
            String safe = value == null || value.isEmpty() ? "-" : value;
            stream.setNonStrokingColor(color[0], color[1], color[2]);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(x, baseline);
            stream.showText(safe);
            stream.endText();
        }

        private void rightText(String value, PDType1Font font, float size, float[] color,
                               float right, float baseline, float maxWidth) throws IOException {
            String fitted = fit(value, font, size, maxWidth);
            float w = width(font, size, fitted);
            draw(right - w, baseline, fitted, font, size, color);
        }

        private void fill(float x, float yBottom, float w, float h, float[] color) throws IOException {
            stream.setNonStrokingColor(color[0], color[1], color[2]);
            stream.addRect(x, yBottom, w, h);
            stream.fill();
        }

        private void strokeRect(float x, float yBottom, float w, float h, float[] color) throws IOException {
            stream.setStrokingColor(color[0], color[1], color[2]);
            stream.setLineWidth(0.6f);
            stream.addRect(x, yBottom, w, h);
            stream.stroke();
        }

        private void strokeH(float x, float yLine, float w, float[] color) throws IOException {
            stream.setStrokingColor(color[0], color[1], color[2]);
            stream.setLineWidth(0.5f);
            stream.moveTo(x, yLine);
            stream.lineTo(x + w, yLine);
            stream.stroke();
        }

        private String fit(String text, PDType1Font font, float size, float maxWidth) throws IOException {
            String safe = dash(text);
            if (width(font, size, safe) <= maxWidth) {
                return safe;
            }
            String ellipsis = "...";
            int end = safe.length();
            while (end > 0 && width(font, size, safe.substring(0, end) + ellipsis) > maxWidth) {
                end--;
            }
            return end <= 0 ? ellipsis : safe.substring(0, end) + ellipsis;
        }

        private float width(PDType1Font font, float size, String text) throws IOException {
            return font.getStringWidth(text) / 1000f * size;
        }

        void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        void stampFooters(String classification) throws IOException {
            int n = doc.getNumberOfPages();
            String label = "Classification: " + dash(classification);
            for (int i = 0; i < n; i++) {
                PDPage p = doc.getPage(i);
                try (PDPageContentStream footer = new PDPageContentStream(
                        doc, p, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    float pageW = p.getMediaBox().getWidth();
                    footer.setStrokingColor(HAIR[0], HAIR[1], HAIR[2]);
                    footer.setLineWidth(0.4f);
                    footer.moveTo(MARGIN, 34f);
                    footer.lineTo(pageW - MARGIN, 34f);
                    footer.stroke();
                    String pageNo = "Page " + (i + 1) + " of " + n;
                    float pageNoW = regular.getStringWidth(pageNo) / 1000f * 8f;
                    footer.setNonStrokingColor(MUTED[0], MUTED[1], MUTED[2]);
                    footer.beginText();
                    footer.setFont(regular, 8f);
                    footer.newLineAtOffset(MARGIN, 20f);
                    footer.showText(fit(label, regular, 8f, contentWidth() * 0.7f));
                    footer.endText();
                    footer.beginText();
                    footer.setFont(regular, 8f);
                    footer.newLineAtOffset(pageW - MARGIN - pageNoW, 20f);
                    footer.showText(pageNo);
                    footer.endText();
                }
            }
        }
    }

    private record Tile(String label, String value, String caption) {
    }

    private record Pair(String label, String value) {
    }

    private static Tile tile(String label, String value, String caption) {
        return new Tile(label, dash(value), caption);
    }

    private static Pair pair(String label, String value) {
        return new Pair(label, dash(value));
    }
}
