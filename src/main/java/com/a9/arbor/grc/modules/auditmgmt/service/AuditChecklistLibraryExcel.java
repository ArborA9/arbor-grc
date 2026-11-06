package com.a9.arbor.grc.modules.auditmgmt.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Excel SoT checklist columns B-G (library definition; auditor fill is on the project copy). */
@Component
public class AuditChecklistLibraryExcel {

    static final String[] TEMPLATE_HEADERS = {
            "Clause / Annex A Ref",
            "Section",
            "Clause / Control Title",
            "What the Auditor Is Testing",
            "SoA Applicability",
            "Where the Evidence Lives"
    };

    public record Parsed(
            String clauseRef,
            String section,
            String title,
            String testFocus,
            String soaApplicability,
            String evidenceLives
    ) {
    }

    public byte[] writeBlankTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Checklist");
            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                header.createCell(i).setCellValue(TEMPLATE_HEADERS[i]);
                sheet.setColumnWidth(i, 24 * 256);
            }
            sheet.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write audit checklist template", e);
        }
    }

    public List<Parsed> parseTemplate(byte[] bytes) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook has no sheets");
            }
            Row header = sheet.getRow(0);
            Map<String, Integer> cols = headerIndex(header);
            requireCol(cols, "clause / annex a ref", "clause ref", "clause / annex a ref");
            Integer clauseCol = col(cols, "clause / annex a ref", "clause ref", "clause / control ref");
            DataFormatter fmt = new DataFormatter();
            List<Parsed> out = new ArrayList<>();
            LinkedHashMap<String, String> seen = new LinkedHashMap<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String clause = cell(fmt, row, clauseCol);
                if (clause == null || clause.isBlank()) {
                    continue;
                }
                String trimmed = clause.trim();
                String key = trimmed.toLowerCase(Locale.ROOT);
                if (seen.put(key, trimmed) != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Clause ref \"" + trimmed + "\" is duplicated in the workbook");
                }
                String title = cell(fmt, row, col(cols, "clause / control title", "control title", "title"));
                out.add(new Parsed(
                        trimmed,
                        cell(fmt, row, col(cols, "section")),
                        title == null || title.isBlank() ? trimmed : title,
                        cell(fmt, row, col(cols, "what the auditor is testing", "test focus")),
                        cell(fmt, row, col(cols, "soa applicability")),
                        cell(fmt, row, col(cols, "where the evidence lives", "evidence lives"))));
            }
            return out;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read checklist Excel");
        }
    }

    private static Map<String, Integer> headerIndex(Row header) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (header == null) {
            return map;
        }
        DataFormatter fmt = new DataFormatter();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String raw = cell(fmt, header, i);
            if (raw != null && !raw.isBlank()) {
                map.put(raw.trim().toLowerCase(Locale.ROOT), i);
            }
        }
        return map;
    }

    private static Integer col(Map<String, Integer> cols, String... names) {
        for (String name : names) {
            Integer idx = cols.get(name);
            if (idx != null) {
                return idx;
            }
        }
        return -1;
    }

    private static void requireCol(Map<String, Integer> cols, String... names) {
        if (col(cols, names) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing column: Clause / Annex A Ref");
        }
    }

    private static String cell(DataFormatter fmt, Row row, int idx) {
        if (idx < 0) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        String v = fmt.formatCellValue(cell);
        return v == null || v.isBlank() ? null : v.trim();
    }
}
