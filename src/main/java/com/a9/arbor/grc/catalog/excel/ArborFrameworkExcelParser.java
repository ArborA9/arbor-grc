package com.a9.arbor.grc.catalog.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Canonical Arbor Controls sheet parser used for all standard framework types until
 * dedicated client-layout parsers exist.
 */
@Component
public class ArborFrameworkExcelParser {

    public static final String SHEET_NAME = "Controls";
    public static final String[] HEADERS = {
            "level", "parent_code", "code", "title", "definition", "control_type", "priority", "display_order"
    };

    private final DataFormatter formatter = new DataFormatter();

    public List<ParsedFrameworkRow> parse(InputStream in) {
        List<ImportRowError> errors = new ArrayList<>();
        List<ParsedFrameworkRow> rows = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                throw new ImportValidationException(List.of(
                        new ImportRowError(0, "sheet", "Missing sheet '" + SHEET_NAME + "'")));
            }
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new ImportValidationException(List.of(
                        new ImportRowError(1, "header", "Header row is missing")));
            }
            Map<String, Integer> col = mapHeaders(header, errors);
            if (!errors.isEmpty()) {
                throw new ImportValidationException(errors);
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row, col)) {
                    continue;
                }
                int excelRow = r + 1;
                String levelRaw = cell(row, col.get("level"));
                String parentCode = blankToNull(cell(row, col.get("parent_code")));
                String code = cell(row, col.get("code"));
                String title = cell(row, col.get("title"));
                String definition = blankToNull(cell(row, col.get("definition")));
                String controlType = blankToNull(cell(row, col.get("control_type")));
                String priority = blankToNull(cell(row, col.get("priority")));
                String orderRaw = cell(row, col.get("display_order"));

                Integer level = parseInt(levelRaw, excelRow, "level", errors);
                int displayOrder = orderRaw == null || orderRaw.isBlank()
                        ? excelRow
                        : parseIntOr(orderRaw, excelRow, "display_order", errors, excelRow);

                if (code == null || code.isBlank()) {
                    errors.add(new ImportRowError(excelRow, "code", "code is required"));
                }
                if (title == null || title.isBlank()) {
                    errors.add(new ImportRowError(excelRow, "title", "title is required"));
                }
                if (level != null && (level < 1 || level > 4)) {
                    errors.add(new ImportRowError(excelRow, "level", "level must be 1–4"));
                }
                if (level != null && level == 1 && parentCode != null) {
                    errors.add(new ImportRowError(excelRow, "parent_code", "parent_code must be empty for level 1"));
                }
                if (level != null && level > 1 && (parentCode == null || parentCode.isBlank())) {
                    errors.add(new ImportRowError(excelRow, "parent_code", "parent_code is required for level > 1"));
                }
                if (level != null && level >= 3 && (definition == null || definition.isBlank())) {
                    errors.add(new ImportRowError(excelRow, "definition", "definition is required for level 3–4"));
                }
                if (priority != null && !priority.matches("P[1-4]")) {
                    errors.add(new ImportRowError(excelRow, "priority", "priority must be P1–P4 when set"));
                }

                if (level != null && code != null && !code.isBlank() && title != null && !title.isBlank()) {
                    rows.add(ParsedFrameworkRow.legacy(
                            excelRow, level, parentCode, code.trim(), title.trim(),
                            definition, controlType, priority, displayOrder));
                }
            }
        } catch (ImportValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "file", "Could not read Excel: " + e.getMessage())));
        }

        if (rows.isEmpty() && errors.isEmpty()) {
            errors.add(new ImportRowError(0, "file", "No data rows found in Controls sheet"));
        }

        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }
        ParsedFrameworkRowValidator.validateOrThrow(rows, List.of());
        return rows;
    }

    private Map<String, Integer> mapHeaders(Row header, List<ImportRowError> errors) {
        Map<String, Integer> col = new HashMap<>();
        for (Cell cell : header) {
            String name = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            if (!name.isBlank()) {
                col.put(name, cell.getColumnIndex());
            }
        }
        for (String required : HEADERS) {
            if (!col.containsKey(required)) {
                errors.add(new ImportRowError(1, required, "Missing required column '" + required + "'"));
            }
        }
        return col;
    }

    private boolean isBlankRow(Row row, Map<String, Integer> col) {
        for (String h : HEADERS) {
            String v = cell(row, col.get(h));
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cell(Row row, Integer idx) {
        if (idx == null) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return null;
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    private Integer parseInt(String raw, int excelRow, String field, List<ImportRowError> errors) {
        if (raw == null || raw.isBlank()) {
            errors.add(new ImportRowError(excelRow, field, field + " is required"));
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            errors.add(new ImportRowError(excelRow, field, field + " must be an integer"));
            return null;
        }
    }

    private int parseIntOr(String raw, int excelRow, String field, List<ImportRowError> errors, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            errors.add(new ImportRowError(excelRow, field, field + " must be an integer"));
            return fallback;
        }
    }
}
