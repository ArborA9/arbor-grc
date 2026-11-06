package com.a9.arbor.grc.modules.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws bordered tables with header band and zebra rows (PDFBox: Reports hub §19.3).
 */
public final class ReportPdfTableRenderer {

    private static final float HEADER_HEIGHT = 18f;
    private static final float ROW_HEIGHT = 16f;
    private static final float LINE = 10f;
    private static final float CELL_PAD = 4f;
    private static final float HEADER_FONT_SIZE = 8.5f;
    private static final float BODY_FONT_SIZE = 8f;

    @FunctionalInterface
    public interface NewPageAction {
        TableCursor newPage() throws IOException;
    }

    public record TableCursor(PDPage page, PDPageContentStream stream, float y) {
    }

    private ReportPdfTableRenderer() {
    }

    public static TableCursor draw(TableCursor cursor,
                            float margin,
                            float footerReserve,
                            PDType1Font bodyFont,
                            PDType1Font headerFont,
                            String[] headers,
                            List<String[]> rows,
                            float[] colWeights,
                            NewPageAction onNewPage) throws IOException {
        if (headers == null || headers.length == 0) {
            return cursor;
        }
        float tableWidth = cursor.page().getMediaBox().getWidth() - 2 * margin;
        float[] colWidths = columnWidths(colWeights, headers.length, tableWidth);
        float minY = margin + footerReserve;
        TableCursor c = cursor;

        float blockTop = c.y();
        c = drawHeaderRow(c, margin, tableWidth, colWidths, headers, headerFont);

        for (int i = 0; i < rows.size(); i++) {
            List<List<String>> wrapped = wrapRow(rows.get(i), headers.length, colWidths, bodyFont, BODY_FONT_SIZE);
            float rowHeight = rowHeight(wrapped);
            if (c.y() - rowHeight < minY && c.y() < blockTop - 1f) {
                drawTableSides(c.stream(), margin, c.y(), blockTop, tableWidth, colWidths);
                c = onNewPage.newPage();
                blockTop = c.y();
                c = drawHeaderRow(c, margin, tableWidth, colWidths, headers, headerFont);
            }
            c = drawDataRow(c, margin, tableWidth, colWidths, wrapped, bodyFont, rowHeight, i % 2 == 1);
        }

        drawTableSides(c.stream(), margin, c.y(), blockTop, tableWidth, colWidths);
        return new TableCursor(c.page(), c.stream(), c.y() - 8f);
    }

    private static TableCursor drawHeaderRow(TableCursor c,
                                             float margin,
                                             float tableWidth,
                                             float[] colWidths,
                                             String[] headers,
                                             PDType1Font headerFont) throws IOException {
        List<List<String>> wrapped = wrapRow(headers, headers.length, colWidths, headerFont, HEADER_FONT_SIZE);
        float headerHeight = Math.max(HEADER_HEIGHT, 6f + maxLines(wrapped) * LINE);
        float yBottom = c.y() - headerHeight;
        c.stream().setNonStrokingColor(0.93f);
        c.stream().addRect(margin, yBottom, tableWidth, headerHeight);
        c.stream().fill();
        c.stream().setStrokingColor(0.55f);
        c.stream().setLineWidth(0.75f);
        c.stream().addRect(margin, yBottom, tableWidth, headerHeight);
        c.stream().stroke();

        float x = margin;
        for (int i = 0; i < wrapped.size(); i++) {
            drawWrapped(c.stream(), headerFont, HEADER_FONT_SIZE, wrapped.get(i), x, yBottom, headerHeight);
            x += colWidths[i];
        }
        return new TableCursor(c.page(), c.stream(), yBottom);
    }

    private static TableCursor drawDataRow(TableCursor c,
                                           float margin,
                                           float tableWidth,
                                           float[] colWidths,
                                           List<List<String>> wrapped,
                                           PDType1Font bodyFont,
                                           float rowHeight,
                                           boolean alt) throws IOException {
        float yBottom = c.y() - rowHeight;
        if (alt) {
            c.stream().setNonStrokingColor(0.97f);
            c.stream().addRect(margin, yBottom, tableWidth, rowHeight);
            c.stream().fill();
        }
        c.stream().setStrokingColor(0.85f);
        c.stream().setLineWidth(0.35f);
        c.stream().moveTo(margin, yBottom);
        c.stream().lineTo(margin + tableWidth, yBottom);
        c.stream().stroke();

        float x = margin;
        for (int i = 0; i < wrapped.size(); i++) {
            drawWrapped(c.stream(), bodyFont, BODY_FONT_SIZE, wrapped.get(i), x, yBottom, rowHeight);
            x += colWidths[i];
        }
        return new TableCursor(c.page(), c.stream(), yBottom);
    }

    private static List<List<String>> wrapRow(String[] row, int colCount, float[] colWidths,
                                              PDType1Font font, float fontSize) throws IOException {
        List<List<String>> wrapped = new ArrayList<>();
        for (int i = 0; i < colCount; i++) {
            String cell = row != null && i < row.length ? row[i] : "";
            float inner = Math.max(8f, colWidths[i] - 2 * CELL_PAD);
            wrapped.add(wrapLines(toPdfSafe(sanitize(cell)), font, fontSize, inner));
        }
        return wrapped;
    }

    private static int maxLines(List<List<String>> wrapped) {
        int lines = 1;
        for (List<String> cell : wrapped) {
            lines = Math.max(lines, cell.size());
        }
        return lines;
    }

    private static float rowHeight(List<List<String>> wrapped) {
        return Math.max(ROW_HEIGHT, 6f + maxLines(wrapped) * LINE);
    }

    /** Word wrap. A single token wider than the cell is split so nothing is dropped. */
    private static List<String> wrapLines(String text, PDType1Font font, float fontSize, float maxWidth)
            throws IOException {
        if (text.isEmpty() || maxWidth <= 0) {
            return List.of("");
        }
        if (stringWidth(font, text, fontSize) <= maxWidth) {
            return List.of(text);
        }
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (stringWidth(font, word, fontSize) > maxWidth) {
                if (!line.isEmpty()) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                appendBroken(lines, word, font, fontSize, maxWidth);
                continue;
            }
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (stringWidth(font, candidate, fontSize) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of(text) : lines;
    }

    private static void appendBroken(List<String> lines, String word, PDType1Font font, float fontSize, float maxWidth)
            throws IOException {
        String rest = word;
        while (!rest.isEmpty()) {
            int n = fitCount(font, fontSize, rest, maxWidth);
            if (n >= rest.length()) {
                lines.add(rest);
                return;
            }
            lines.add(rest.substring(0, n));
            rest = rest.substring(n);
        }
    }

    private static int fitCount(PDType1Font font, float fontSize, String text, float maxWidth) throws IOException {
        int lo = 1;
        int hi = text.length();
        int best = 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (stringWidth(font, text.substring(0, mid), fontSize) <= maxWidth) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }

    private static void drawWrapped(PDPageContentStream stream,
                                    PDType1Font font,
                                    float fontSize,
                                    List<String> lines,
                                    float x,
                                    float yBottom,
                                    float rowHeight) throws IOException {
        float baseline = yBottom + rowHeight - 11f;
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                baseline -= LINE;
                continue;
            }
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.setNonStrokingColor(0.1f);
            stream.newLineAtOffset(x + CELL_PAD, baseline);
            stream.showText(line);
            stream.endText();
            baseline -= LINE;
        }
    }

    private static void drawTableSides(PDPageContentStream stream,
                                       float margin,
                                       float yBottom,
                                       float yTop,
                                       float tableWidth,
                                       float[] colWidths) throws IOException {
        stream.setStrokingColor(0.55f);
        stream.setLineWidth(0.75f);
        stream.addRect(margin, yBottom, tableWidth, yTop - yBottom);
        stream.stroke();

        stream.setStrokingColor(0.85f);
        stream.setLineWidth(0.35f);
        float x = margin;
        for (int i = 0; i < colWidths.length - 1; i++) {
            x += colWidths[i];
            stream.moveTo(x, yBottom);
            stream.lineTo(x, yTop);
            stream.stroke();
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.isBlank()) {
            return "-";
        }
        return s.replace('\n', ' ').replace('\r', ' ').trim();
    }

    /** Standard 14 fonts: keep text WinAnsi-safe for {@link PDPageContentStream#showText(String)}. */
    private static String toPdfSafe(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u2014' || c == '\u2013') {
                sb.append('-');
            } else if (c >= 32 && c <= 126) {
                sb.append(c);
            } else if (c >= 160 && c <= 255) {
                sb.append(c);
            } else {
                sb.append('?');
            }
        }
        return sb.toString();
    }

    private static float stringWidth(PDType1Font font, String text, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private static float[] columnWidths(float[] weights, int cols, float tableWidth) {
        float[] w = new float[cols];
        if (weights == null || weights.length != cols) {
            float each = tableWidth / cols;
            for (int i = 0; i < cols; i++) {
                w[i] = each;
            }
            return w;
        }
        float sum = 0f;
        for (float weight : weights) {
            sum += weight;
        }
        if (sum <= 0) {
            float each = tableWidth / cols;
            for (int i = 0; i < cols; i++) {
                w[i] = each;
            }
            return w;
        }
        for (int i = 0; i < cols; i++) {
            w[i] = tableWidth * (weights[i] / sum);
        }
        return w;
    }
}
