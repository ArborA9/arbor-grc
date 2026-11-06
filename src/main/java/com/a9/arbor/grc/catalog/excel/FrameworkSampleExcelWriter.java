package com.a9.arbor.grc.catalog.excel;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Builds a downloadable Arbor Controls template with a few sample rows per framework type. */
@Component
public class FrameworkSampleExcelWriter {

    public byte[] writeSample(FrameworkTypeCode type) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(ArborFrameworkExcelParser.SHEET_NAME);
            Row header = sheet.createRow(0);
            for (int i = 0; i < ArborFrameworkExcelParser.HEADERS.length; i++) {
                header.createCell(i).setCellValue(ArborFrameworkExcelParser.HEADERS[i]);
            }

            String[][] samples = sampleRows(type);
            for (int i = 0; i < samples.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int c = 0; c < samples[i].length; c++) {
                    row.createCell(c).setCellValue(samples[i][c]);
                }
            }
            for (int i = 0; i < ArborFrameworkExcelParser.HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build sample Excel", e);
        }
    }

    private static String[][] sampleRows(FrameworkTypeCode type) {
        String prefix = switch (type) {
            case UAE_IA -> "M";
            case NCEMA_V3 -> "G";
            case UAE_PDPL -> "A";
            default -> "A";
        };
        boolean withPriority = type == FrameworkTypeCode.UAE_IA;
        return new String[][]{
                {"1", "", prefix + ".1", type.displayName() + " — Domain 1", "", "Domain", "", "10"},
                {"2", prefix + ".1", prefix + ".1.1", "Control group 1", "", "Group", "", "20"},
                {"3", prefix + ".1.1", prefix + ".1.1.1", "Example control",
                        "Replace this definition with the official control text from " + type.displayName() + ".",
                        "Management", withPriority ? "P1" : "", "30"},
                {"4", prefix + ".1.1.1", prefix + ".1.1.1.a", "Example sub-control",
                        "Optional level-4 implementation requirement — delete if unused.",
                        "Technical", withPriority ? "P2" : "", "40"},
                {"3", prefix + ".1.1", prefix + ".1.1.2", "Second example control",
                        "Another control under the same group. Keep codes unique within the workbook.",
                        "Management", withPriority ? "P3" : "", "50"},
        };
    }
}
