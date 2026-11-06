package com.a9.arbor.grc.catalog.excel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser for the PCI DSS 4.0.1 workbook (layout S2). Imports the consolidated
 * {@code All 12 Requirements - Master} sheet only.
 *
 * <p>Hierarchy: L1 requirement family (1..12), L2 section {@code N.N} synthesised from req IDs,
 * L3+ every master row as a routed leaf (parents that also appear as rows stay assessable).
 */
@Component
public class PciDssPackParser {

    private static final int MAX_HEADER_SEARCH_ROWS = 8;
    private static final int MAX_CODE = 64;
    private static final int MAX_TITLE = 512;

    private static final String REQ_ID = "req id";
    private static final String GAP_QUESTION = "gap assessment question";
    private static final String CUSTOMIZED = "customized approach";
    private static final String REQUIREMENT_TEXT = "requirement text";
    private static final String TESTING_PROCEDURE = "testing procedure";
    private static final String APPLICABILITY = "applicability";
    private static final String EVIDENCE_EXAMPLES = "evidence examples";
    private static final String REQUIREMENT_FAMILY = "requirement family";

    private static final Pattern FAMILY_NUMBER = Pattern.compile("requirement\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    static final String ATTR_TESTING_PROCEDURE = "testing_procedure";
    static final String ATTR_CUSTOMIZED_APPROACH = "customized_approach";
    static final String ATTR_EVIDENCE_EXAMPLES = "evidence_examples";
    static final String ATTR_APPLICABILITY = "applicability";

    private static final Routing ALL_ROUTED = new Routing(true, true, true, true);
    private static final Routing UNROUTED = new Routing(false, false, false, false);

    private final DataFormatter formatter = new DataFormatter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isPciDssPack(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (layout(workbook.getSheetAt(i)) != null) {
                return true;
            }
        }
        return false;
    }

    public PackFrameworkPlan parse(byte[] bytes) {
        try (Workbook workbook = open(bytes)) {
            Sheet master = null;
            ColumnLayout columns = null;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                ColumnLayout layout = layout(sheet);
                if (layout != null) {
                    master = sheet;
                    columns = layout;
                    break;
                }
            }
            if (master == null) {
                throw new ImportValidationException(List.of(new ImportRowError(0, "file",
                        "Not a PCI DSS pack (no sheet with Req ID + Gap Assessment Question + "
                                + "Customized Approach headers)")));
            }
            return parseMaster(master, columns);
        } catch (IOException e) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "file", "Could not read Excel: " + e.getMessage())));
        }
    }

    private PackFrameworkPlan parseMaster(Sheet sheet, ColumnLayout columns) {
        Builder builder = new Builder(this, sheet.getSheetName(), columns);
        for (int rowIndex = columns.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String reqId = cell(row, columns.reqId());
            if (reqId == null || reqId.isBlank()) {
                continue;
            }
            reqId = reqId.trim();
            String familyLabel = cell(row, columns.requirementFamily());
            String requirementText = cell(row, columns.requirementText());
            String testingProcedure = cell(row, columns.testingProcedure());
            String customizedApproach = cell(row, columns.customizedApproach());
            String gapQuestion = cell(row, columns.gapQuestion());
            String applicability = cell(row, columns.applicability());
            String evidenceExamples = cell(row, columns.evidenceExamples());

            if (requirementText == null || requirementText.isBlank()) {
                builder.errors.add(new ImportRowError(rowIndex + 1, REQUIREMENT_TEXT,
                        "Requirement Text is required"));
                continue;
            }

            String[] parts = reqId.split("\\.");
            if (parts.length < 3) {
                builder.errors.add(new ImportRowError(rowIndex + 1, REQ_ID,
                        "Req ID must have at least three segments (e.g. 1.1.1), got '" + reqId + "'"));
                continue;
            }

            String familyCode = familyNumber(familyLabel, parts[0]);
            builder.ensureFamily(familyCode, familyLabel, rowIndex + 1);

            String sectionCode = parts[0] + "." + parts[1];
            builder.ensureSection(sectionCode, familyCode, rowIndex + 1);

            String parent = sectionCode;
            for (int depth = 3; depth <= parts.length; depth++) {
                String code = String.join(".", Arrays.copyOfRange(parts, 0, depth));
                if (depth < parts.length) {
                    builder.ensureAncestor(code, depth, parent, rowIndex + 1);
                    parent = code;
                } else {
                    builder.leaf(code, depth, parent, requirementText, testingProcedure,
                            customizedApproach, applicability, evidenceExamples, gapQuestion,
                            rowIndex + 1, row);
                }
            }
        }

        Map<String, Integer> written = new LinkedHashMap<>();
        builder.byCode.values().forEach(d -> d.attributes.keySet()
                .forEach(key -> written.merge(key, 1, Integer::sum)));
        if (!written.isEmpty()) {
            builder.info.add("attributes written: " + written.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ")));
        }

        List<ParsedFrameworkRow> nodes = builder.byCode.values().stream()
                .map(Builder::toRow)
                .toList();
        return new PackFrameworkPlan(nodes, builder.questions, builder.staging,
                builder.errors, builder.info);
    }

    private ColumnLayout layout(Sheet sheet) {
        for (int rowIndex = 0; rowIndex < MAX_HEADER_SEARCH_ROWS; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (Cell cell : row) {
                String header = normalizeHeader(formatter.formatCellValue(cell));
                if (!header.isBlank()) {
                    columns.put(header, cell.getColumnIndex());
                }
            }
            if (columns.containsKey(REQ_ID)
                    && columns.containsKey(GAP_QUESTION)
                    && columns.keySet().stream().anyMatch(h -> h.contains(CUSTOMIZED))) {
                return new ColumnLayout(rowIndex, columns);
            }
        }
        return null;
    }

    private static String familyNumber(String familyLabel, String fallback) {
        if (familyLabel != null) {
            Matcher matcher = FAMILY_NUMBER.matcher(familyLabel);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return fallback;
    }

    private String cell(Row row, int columnIndex) {
        if (columnIndex < 0) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell);
        return value == null || value.isBlank() ? null : value.trim();
    }

    String rawJson(Row row, ColumnLayout columns) {
        Map<String, String> values = new LinkedHashMap<>();
        columns.columns().forEach((header, index) -> {
            Cell cell = row.getCell(index);
            if (cell != null) {
                String value = formatter.formatCellValue(cell);
                if (value != null && !value.isBlank()) {
                    values.put(header, value.trim());
                }
            }
        });
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String normalizeHeader(String header) {
        return header == null ? "" : header.replace('\n', ' ').replaceAll("\\s+", " ")
                .trim().toLowerCase(Locale.ROOT);
    }

    private static Workbook open(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return new XSSFWorkbook(in);
        }
    }

    private record ColumnLayout(int headerRowIndex, Map<String, Integer> columns) {
        int reqId() {
            return columns.getOrDefault(REQ_ID, -1);
        }

        int requirementFamily() {
            return columns.getOrDefault(REQUIREMENT_FAMILY, -1);
        }

        int requirementText() {
            return columns.keySet().stream()
                    .filter(h -> h.startsWith(REQUIREMENT_TEXT))
                    .findFirst()
                    .map(columns::get)
                    .orElse(-1);
        }

        int testingProcedure() {
            return columns.keySet().stream()
                    .filter(h -> h.startsWith(TESTING_PROCEDURE))
                    .findFirst()
                    .map(columns::get)
                    .orElse(-1);
        }

        int customizedApproach() {
            return columns.keySet().stream()
                    .filter(h -> h.contains(CUSTOMIZED))
                    .findFirst()
                    .map(columns::get)
                    .orElse(-1);
        }

        int gapQuestion() {
            return columns.getOrDefault(GAP_QUESTION, -1);
        }

        int applicability() {
            return columns.getOrDefault(APPLICABILITY, -1);
        }

        int evidenceExamples() {
            return columns.keySet().stream()
                    .filter(h -> h.startsWith(EVIDENCE_EXAMPLES))
                    .findFirst()
                    .map(columns::get)
                    .orElse(-1);
        }
    }

    private record Routing(boolean inSoa, boolean inGap, boolean inAudit, boolean inRisk) {
    }

    private static final class Builder {
        private final PciDssPackParser parser;
        private final String sheetName;
        private final ColumnLayout columns;
        private final Map<String, Draft> byCode = new LinkedHashMap<>();
        private final List<ParsedMasterWorkbook.Question> questions = new ArrayList<>();
        private final List<ImportStagingRow> staging = new ArrayList<>();
        private final List<ImportRowError> errors = new ArrayList<>();
        private final List<String> info = new ArrayList<>();
        private int order;

        Builder(PciDssPackParser parser, String sheetName, ColumnLayout columns) {
            this.parser = parser;
            this.sheetName = sheetName;
            this.columns = columns;
        }

        void ensureFamily(String code, String title, int excelRow) {
            if (!byCode.containsKey(code)) {
                add(code, 1, null, title == null || title.isBlank() ? "Requirement " + code : title,
                        excelRow, false, UNROUTED, Map.of());
            }
        }

        void ensureSection(String code, String parent, int excelRow) {
            if (!byCode.containsKey(code)) {
                add(code, 2, parent, code, excelRow, false, UNROUTED, Map.of());
            }
        }

        void ensureAncestor(String code, int level, String parent, int excelRow) {
            if (!byCode.containsKey(code)) {
                add(code, level, parent, code, excelRow, false, UNROUTED, Map.of());
            }
        }

        void leaf(String code, int level, String parent, String requirementText,
                  String testingProcedure, String customizedApproach, String applicability,
                  String evidenceExamples, String gapQuestion, int excelRow, Row row) {
            Map<String, String> attributes = new LinkedHashMap<>();
            put(attributes, ATTR_TESTING_PROCEDURE, testingProcedure);
            put(attributes, ATTR_CUSTOMIZED_APPROACH, customizedApproach);
            put(attributes, ATTR_APPLICABILITY, applicability);
            put(attributes, ATTR_EVIDENCE_EXAMPLES, evidenceExamples);

            Draft existing = byCode.get(code);
            if (existing != null && existing.authored) {
                errors.add(new ImportRowError(excelRow, REQ_ID,
                        "Duplicate Req ID '" + code + "'"));
                return;
            }

            Draft draft = existing != null
                    ? existing
                    : add(code, level, parent, truncate(requirementText, MAX_TITLE), excelRow,
                    true, ALL_ROUTED, attributes);
            draft.authored = true;
            draft.title = truncate(requirementText, MAX_TITLE);
            draft.definition = requirementText;
            draft.routing = ALL_ROUTED;
            draft.attributes = attributes;

            if (gapQuestion != null && !gapQuestion.isBlank()) {
                questions.add(new ParsedMasterWorkbook.Question(
                        excelRow, code, "Q1", null, gapQuestion, null, null, false,
                        questions.size() + 1));
            }

            staging.add(new ImportStagingRow(sheetName, excelRow, ImportStagingRow.Kind.NODE,
                    parser.rawJson(row, columns), ImportStagingRow.Status.OK,
                    "level " + level + " req '" + code + "'"));
        }

        private Draft add(String code, int level, String parent, String title, int excelRow,
                          boolean authored, Routing routing, Map<String, String> attributes) {
            Draft draft = new Draft();
            draft.code = truncate(code, MAX_CODE);
            draft.level = level;
            draft.parent = parent;
            draft.title = title;
            draft.displayOrder = ++order;
            draft.excelRow = excelRow;
            draft.authored = authored;
            draft.routing = routing;
            draft.attributes = new LinkedHashMap<>(attributes);
            byCode.put(draft.code, draft);
            return draft;
        }

        private static ParsedFrameworkRow toRow(Draft draft) {
            return new ParsedFrameworkRow(
                    draft.excelRow,
                    draft.level,
                    draft.parent,
                    draft.code,
                    draft.title,
                    draft.definition,
                    null,
                    null,
                    draft.displayOrder,
                    draft.authored ? "CONTROL" : "SECTION",
                    null,
                    draft.routing.inSoa(),
                    draft.routing.inGap(),
                    draft.routing.inAudit(),
                    draft.routing.inRisk(),
                    draft.attributes);
        }
    }

    private static final class Draft {
        private String code;
        private int level;
        private String parent;
        private String title;
        private String definition;
        private int displayOrder;
        private int excelRow;
        private boolean authored;
        private Routing routing = UNROUTED;
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    private static void put(Map<String, String> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
