package com.a9.arbor.grc.catalog.excel;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Parser for Arbor's shared GRC Checklist workbook
 * ({@code GRC_Checklist_with_Control_Descriptions.xlsx}): one sheet per framework,
 * flat leaf rows, synthesized domain/family parents.
 */
@Component
public class GrcChecklistExcelParser {

    public static final Set<String> FRAMEWORK_SHEETS = Set.of(
            "ISO 27001", "ISO 20000", "ISO 27017", "UAE IA");

    private static final Map<FrameworkTypeCode, String> SHEET_BY_TYPE = Map.of(
            FrameworkTypeCode.ISO_27001, "ISO 27001",
            FrameworkTypeCode.ISO_20000, "ISO 20000",
            FrameworkTypeCode.ISO_27017, "ISO 27017",
            FrameworkTypeCode.UAE_IA, "UAE IA");

    private static final Map<String, String> ISO_27001_DOMAIN_TITLES = Map.of(
            "A.5", "Organizational controls",
            "A.6", "People controls",
            "A.7", "Physical controls",
            "A.8", "Technological controls");

    private static final Map<String, String> ISO_20000_CLAUSE_TITLES = Map.of(
            "4", "Context of the organization",
            "5", "Leadership",
            "6", "Planning",
            "7", "Support of the service management system",
            "8", "Operation of the service management system",
            "9", "Performance evaluation",
            "10", "Improvement");

    private static final Map<String, String> UAE_IA_FAMILY_TITLES = Map.ofEntries(
            Map.entry("M1", "M1 — Management"),
            Map.entry("M2", "M2 — Management"),
            Map.entry("M3", "M3 — Management"),
            Map.entry("M4", "M4 — Management"),
            Map.entry("M5", "M5 — Management"),
            Map.entry("M6", "M6 — Management"),
            Map.entry("T1", "T1 — Technical"),
            Map.entry("T2", "T2 — Technical"),
            Map.entry("T3", "T3 — Technical"),
            Map.entry("T4", "T4 — Technical"),
            Map.entry("T5", "T5 — Technical"),
            Map.entry("T6", "T6 — Technical"),
            Map.entry("T7", "T7 — Technical"),
            Map.entry("T8", "T8 — Technical"),
            Map.entry("T9", "T9 — Technical"));

    private final DataFormatter formatter = new DataFormatter();

    public static boolean supportsType(FrameworkTypeCode type) {
        return SHEET_BY_TYPE.containsKey(type);
    }

    public static List<FrameworkTypeCode> supportedTypes() {
        return List.of(
                FrameworkTypeCode.ISO_27001,
                FrameworkTypeCode.ISO_20000,
                FrameworkTypeCode.ISO_27017,
                FrameworkTypeCode.UAE_IA);
    }

    public boolean isChecklistWorkbook(byte[] bytes) {
        try (Workbook workbook = open(bytes)) {
            return isChecklistWorkbook(workbook);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isChecklistWorkbook(Workbook workbook) {
        int hits = 0;
        for (String name : FRAMEWORK_SHEETS) {
            if (workbook.getSheet(name) != null) {
                hits++;
            }
        }
        return hits >= 2;
    }

    /** Parse one framework sheet (type must be one of the four checklist types). */
    public List<ParsedFrameworkRow> parse(byte[] bytes, FrameworkTypeCode type) {
        if (!supportsType(type)) {
            throw new ImportValidationException(List.of(new ImportRowError(0, "type",
                    "GRC Checklist does not contain a sheet for " + type.name())));
        }
        try (Workbook workbook = open(bytes)) {
            if (!isChecklistWorkbook(workbook)) {
                throw new ImportValidationException(List.of(new ImportRowError(0, "file",
                        "Not a GRC Checklist workbook (expected sheets ISO 27001 / ISO 20000 / ISO 27017 / UAE IA)")));
            }
            return parseSheet(workbook, type);
        } catch (ImportValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "file", "Could not read Excel: " + e.getMessage())));
        }
    }

    /** Parse all four framework sheets. */
    public Map<FrameworkTypeCode, List<ParsedFrameworkRow>> parseAll(byte[] bytes) {
        try (Workbook workbook = open(bytes)) {
            if (!isChecklistWorkbook(workbook)) {
                throw new ImportValidationException(List.of(new ImportRowError(0, "file",
                        "Not a GRC Checklist workbook (expected sheets ISO 27001 / ISO 20000 / ISO 27017 / UAE IA)")));
            }
            Map<FrameworkTypeCode, List<ParsedFrameworkRow>> out = new EnumMap<>(FrameworkTypeCode.class);
            for (FrameworkTypeCode type : supportedTypes()) {
                out.put(type, parseSheet(workbook, type));
            }
            return out;
        } catch (ImportValidationException e) {
            throw e;
        } catch (IOException e) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "file", "Could not read Excel: " + e.getMessage())));
        }
    }

    private List<ParsedFrameworkRow> parseSheet(Workbook workbook, FrameworkTypeCode type) {
        String sheetName = SHEET_BY_TYPE.get(type);
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "sheet", "Missing sheet '" + sheetName + "'")));
        }
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(1, "header", "Header row is missing on sheet '" + sheetName + "'")));
        }
        List<ImportRowError> errors = new ArrayList<>();
        Map<String, Integer> col = mapHeaders(header, sheetName, errors);
        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }

        List<LeafRow> leaves = new ArrayList<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int excelRow = r + 1;
            String ref = cell(row, col.get("ref"));
            String title = cell(row, col.get("control"));
            String description = blankToNull(cell(row, col.get("control description")));
            String requirement = blankToNull(cell(row, col.get("control requirement")));
            String controlType = blankToNull(cell(row, col.get("control type")));

            if (isBlank(ref) && isBlank(title) && description == null && requirement == null) {
                continue;
            }
            if (isBlank(ref)) {
                errors.add(new ImportRowError(excelRow, "ref", "Ref is required"));
                continue;
            }
            if (isBlank(title)) {
                errors.add(new ImportRowError(excelRow, "control", "Control title is required"));
                continue;
            }
            String definition = combineDefinition(description, requirement);
            if (definition == null) {
                errors.add(new ImportRowError(excelRow, "definition",
                        "Control Description or Control Requirement is required"));
                continue;
            }
            leaves.add(new LeafRow(excelRow, ref.trim(), title.trim(), definition, controlType));
        }

        if (leaves.isEmpty() && errors.isEmpty()) {
            errors.add(new ImportRowError(0, "sheet", "No data rows on sheet '" + sheetName + "'"));
        }
        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }

        List<ParsedFrameworkRow> rows = switch (type) {
            case ISO_27001 -> buildIso27001(leaves);
            case ISO_20000 -> buildIso20000(leaves);
            case ISO_27017 -> buildIso27017(leaves);
            case UAE_IA -> buildUaeIa(leaves);
            default -> throw new ImportValidationException(List.of(new ImportRowError(0, "type",
                    "Unsupported checklist type: " + type.name())));
        };

        ParsedFrameworkRowValidator.validateOrThrow(rows, List.of());
        return rows;
    }

    private List<ParsedFrameworkRow> buildIso27001(List<LeafRow> leaves) {
        Set<String> domains = new TreeSet<>();
        for (LeafRow leaf : leaves) {
            String domain = iso27001Domain(leaf.ref);
            if (domain == null) {
                throw new ImportValidationException(List.of(new ImportRowError(leaf.excelRow, "ref",
                        "Expected Annex A ref like A.5.1, got: " + leaf.ref)));
            }
            domains.add(domain);
        }
        List<ParsedFrameworkRow> rows = new ArrayList<>();
        int order = 1;
        for (String domain : domains) {
            rows.add(synthetic(0, 1, null, domain,
                    ISO_27001_DOMAIN_TITLES.getOrDefault(domain, domain), order++));
        }
        for (LeafRow leaf : leaves) {
            String domain = iso27001Domain(leaf.ref);
            rows.add(ParsedFrameworkRow.legacy(
                    leaf.excelRow, 2, domain, leaf.ref, leaf.title,
                    leaf.definition, leaf.controlType, null, order++));
        }
        return rows;
    }

    private List<ParsedFrameworkRow> buildIso20000(List<LeafRow> leaves) {
        // Collect every ancestor prefix so 8 → 8.2 → 8.2.1 is valid (level steps of 1).
        Set<String> ancestors = new LinkedHashSet<>();
        for (LeafRow leaf : leaves) {
            String[] parts = leaf.ref.split("\\.");
            for (int i = 1; i < parts.length; i++) {
                ancestors.add(joinParts(parts, i));
            }
        }
        List<String> sortedAncestors = new ArrayList<>(ancestors);
        sortedAncestors.sort(Comparator
                .comparingInt((String c) -> c.split("\\.").length)
                .thenComparing(GrcChecklistExcelParser::clauseSortKey));

        List<ParsedFrameworkRow> rows = new ArrayList<>();
        int order = 1;
        Map<String, Integer> levelByCode = new HashMap<>();
        for (String code : sortedAncestors) {
            int level = code.split("\\.").length;
            String parent = parentCode(code);
            levelByCode.put(code, level);
            String title = ISO_20000_CLAUSE_TITLES.getOrDefault(code, code);
            rows.add(synthetic(0, level, parent, code, title, order++));
        }
        for (LeafRow leaf : leaves) {
            String parent = parentCode(leaf.ref);
            int level = parent == null ? 1 : levelByCode.get(parent) + 1;
            rows.add(ParsedFrameworkRow.legacy(
                    leaf.excelRow, level, parent, leaf.ref, leaf.title,
                    leaf.definition, leaf.controlType, null, order++));
        }
        return rows;
    }

    private List<ParsedFrameworkRow> buildIso27017(List<LeafRow> leaves) {
        List<ParsedFrameworkRow> rows = new ArrayList<>();
        int order = 1;
        rows.add(synthetic(0, 1, null, "CLD", "ISO/IEC 27017 cloud controls", order++));
        for (LeafRow leaf : leaves) {
            if (!leaf.ref.toUpperCase(Locale.ROOT).startsWith("CLD.")) {
                throw new ImportValidationException(List.of(new ImportRowError(leaf.excelRow, "ref",
                        "Expected CLD.* ref, got: " + leaf.ref)));
            }
            rows.add(ParsedFrameworkRow.legacy(
                    leaf.excelRow, 2, "CLD", leaf.ref, leaf.title,
                    leaf.definition, leaf.controlType, null, order++));
        }
        return rows;
    }

    private List<ParsedFrameworkRow> buildUaeIa(List<LeafRow> leaves) {
        Set<String> ancestors = new LinkedHashSet<>();
        for (LeafRow leaf : leaves) {
            String[] parts = leaf.ref.split("\\.");
            for (int i = 1; i < parts.length; i++) {
                ancestors.add(joinParts(parts, i));
            }
        }
        List<String> sortedAncestors = new ArrayList<>(ancestors);
        sortedAncestors.sort(Comparator
                .comparingInt((String c) -> c.split("\\.").length)
                .thenComparing(String::compareTo));

        List<ParsedFrameworkRow> rows = new ArrayList<>();
        int order = 1;
        Map<String, Integer> levelByCode = new HashMap<>();
        for (String code : sortedAncestors) {
            int level = code.split("\\.").length;
            String parent = parentCode(code);
            levelByCode.put(code, level);
            String title = UAE_IA_FAMILY_TITLES.getOrDefault(code, code);
            rows.add(synthetic(0, level, parent, code, title, order++));
        }
        for (LeafRow leaf : leaves) {
            String parent = parentCode(leaf.ref);
            int level = parent == null ? 1 : levelByCode.get(parent) + 1;
            rows.add(ParsedFrameworkRow.legacy(
                    leaf.excelRow, level, parent, leaf.ref, leaf.title,
                    leaf.definition, leaf.controlType, null, order++));
        }
        return rows;
    }

    private static ParsedFrameworkRow synthetic(int excelRow, int level, String parent,
                                                String code, String title, int order) {
        return ParsedFrameworkRow.legacy(excelRow, level, parent, code, title, null, null, null, order);
    }

    private static String iso27001Domain(String ref) {
        // A.5.1 → A.5
        String[] parts = ref.split("\\.");
        if (parts.length < 3 || !parts[0].equalsIgnoreCase("A")) {
            return null;
        }
        return "A." + parts[1];
    }

    private static String parentCode(String code) {
        int dot = code.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        return code.substring(0, dot);
    }

    private static String joinParts(String[] parts, int count) {
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < count; i++) {
            sb.append('.').append(parts[i]);
        }
        return sb.toString();
    }

    private static String clauseSortKey(String code) {
        // numeric-aware-ish: pad leading clause number
        String[] parts = code.split("\\.", 2);
        try {
            int n = Integer.parseInt(parts[0]);
            return String.format("%03d.%s", n, parts.length > 1 ? parts[1] : "");
        } catch (NumberFormatException e) {
            return code;
        }
    }

    private static String combineDefinition(String description, String requirement) {
        if (description != null && requirement != null) {
            return description + "\n\n" + requirement;
        }
        return description != null ? description : requirement;
    }

    private Map<String, Integer> mapHeaders(Row header, String sheetName, List<ImportRowError> errors) {
        Map<String, Integer> col = new LinkedHashMap<>();
        for (Cell cell : header) {
            String name = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            if (!name.isBlank()) {
                col.put(name, cell.getColumnIndex());
            }
        }
        for (String required : List.of("ref", "control", "control description", "control requirement", "control type")) {
            if (!col.containsKey(required)) {
                errors.add(new ImportRowError(1, required,
                        "Missing column '" + required + "' on sheet '" + sheetName + "'"));
            }
        }
        return col;
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

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private static Workbook open(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return new XSSFWorkbook(in);
        }
    }

    private record LeafRow(int excelRow, String ref, String title, String definition, String controlType) {
    }
}
