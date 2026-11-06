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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parser for NIST SP 800-53 Rev5 (layout S3). Reads the 20 family tabs and skips
 * {@code Withdrawn Controls}.
 *
 * <p>Hierarchy: L1 family code from the sheet name ({@code AC}, {@code AT}, …), L2 base control
 * {@code XX-N}, L3 enhancement {@code XX-N(n)}. Every active control row is a routed leaf with one
 * {@code Q1} gap question.
 */
@Component
public class Nist80053PackParser {

    private static final int MAX_HEADER_SEARCH_ROWS = 8;
    private static final int MAX_CODE = 64;
    private static final int MAX_TITLE = 512;

    private static final String CONTROL_ID = "control id";
    private static final String TITLE = "title";
    private static final String GAP_QUESTION = "gap assessment question";
    private static final String BASELINE = "baseline (l/m/h)";
    private static final String DISCUSSION = "discussion";
    private static final String RELATED_CONTROLS = "related controls";
    private static final String CONTROL_STATEMENT = "control statement";

    private static final String WITHDRAWN_SHEET = "Withdrawn Controls";
    private static final Pattern CONTROL_ID_PATTERN =
            Pattern.compile("^([A-Z]{2,3})-(\\d+)(?:\\((\\d+)\\))?$");

    static final String ATTR_DISCUSSION = "discussion";
    static final String ATTR_RELATED_CONTROLS = "related_controls";
    static final String ATTR_BASELINE = "baseline";

    private static final Routing ALL_ROUTED = new Routing(true, true, true, true);
    private static final Routing UNROUTED = new Routing(false, false, false, false);

    private final DataFormatter formatter = new DataFormatter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isNist80053Pack(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (WITHDRAWN_SHEET.equalsIgnoreCase(sheet.getSheetName().trim())) {
                continue;
            }
            if (layout(sheet) != null) {
                return true;
            }
        }
        return false;
    }

    public PackFrameworkPlan parse(byte[] bytes) {
        try (Workbook workbook = open(bytes)) {
            Builder builder = new Builder(this);
            int withdrawnSkipped = 0;
            Sheet withdrawn = workbook.getSheet(WITHDRAWN_SHEET);
            if (withdrawn != null) {
                withdrawnSkipped = countWithdrawn(withdrawn);
            }

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                if (WITHDRAWN_SHEET.equalsIgnoreCase(sheetName.trim())) {
                    continue;
                }
                ColumnLayout columns = layout(sheet);
                if (columns == null) {
                    continue;
                }
                parseFamilySheet(builder, sheet, columns);
            }

            if (builder.contentRows == 0) {
                throw new ImportValidationException(List.of(new ImportRowError(0, "file",
                        "Not a NIST SP 800-53 pack (no family sheet with Control ID + Baseline + "
                                + "Gap Assessment Question headers)")));
            }

            if (withdrawnSkipped > 0) {
                builder.info.add(withdrawnSkipped + " withdrawn control(s) skipped on tab '"
                        + WITHDRAWN_SHEET + "'");
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
        } catch (IOException e) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "file", "Could not read Excel: " + e.getMessage())));
        }
    }

    private void parseFamilySheet(Builder builder, Sheet sheet, ColumnLayout columns) {
        String sheetName = sheet.getSheetName();
        String familyCode = familyCode(sheetName);
        String familyTitle = familyTitle(sheetName);
        builder.ensureFamily(familyCode, familyTitle, sheetName);

        for (int rowIndex = columns.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            String controlId = cell(row, columns.controlId());
            if (controlId == null || controlId.isBlank()) {
                continue;
            }
            controlId = controlId.trim();
            Matcher matcher = CONTROL_ID_PATTERN.matcher(controlId);
            if (!matcher.matches()) {
                builder.errors.add(new ImportRowError(rowIndex + 1, CONTROL_ID,
                        "Unrecognised Control ID '" + controlId + "'"));
                continue;
            }

            String title = cell(row, columns.title());
            String statement = cell(row, columns.controlStatement());
            String discussion = cell(row, columns.discussion());
            String gapQuestion = cell(row, columns.gapQuestion());
            String relatedControls = cell(row, columns.relatedControls());
            String baseline = cell(row, columns.baseline());

            if (statement == null || statement.isBlank()) {
                builder.errors.add(new ImportRowError(rowIndex + 1, CONTROL_STATEMENT,
                        "Control Statement is required for " + controlId));
                continue;
            }

            builder.contentRows++;
            String baseCode = matcher.group(1) + "-" + matcher.group(2);
            Map<String, String> attributes = new LinkedHashMap<>();
            put(attributes, ATTR_DISCUSSION, discussion);
            put(attributes, ATTR_RELATED_CONTROLS, relatedControls);
            put(attributes, ATTR_BASELINE, baseline);

            if (matcher.group(3) == null) {
                builder.leaf(baseCode, 2, familyCode, title, statement, attributes, gapQuestion,
                        rowIndex + 1, row, columns, sheetName);
            } else {
                builder.ensureBase(baseCode, familyCode, title, statement, rowIndex + 1, sheetName);
                builder.leaf(controlId, 3, baseCode, title, statement, attributes, gapQuestion,
                        rowIndex + 1, row, columns, sheetName);
            }
        }
    }

    private static int countWithdrawn(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        int count = 0;
        for (int rowIndex = 2; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Cell cell = row.getCell(0);
            if (cell == null) {
                continue;
            }
            String value = formatter.formatCellValue(cell);
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static String familyCode(String sheetName) {
        int dash = sheetName.indexOf(" - ");
        return dash > 0 ? sheetName.substring(0, dash).trim() : sheetName.trim();
    }

    private static String familyTitle(String sheetName) {
        int dash = sheetName.indexOf(" - ");
        return dash > 0 ? sheetName.substring(dash + 3).trim() : sheetName.trim();
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
            if (columns.containsKey(CONTROL_ID)
                    && columns.containsKey(GAP_QUESTION)
                    && columns.containsKey(BASELINE)) {
                return new ColumnLayout(rowIndex, columns);
            }
        }
        return null;
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
        int controlId() {
            return columns.getOrDefault(CONTROL_ID, -1);
        }

        int title() {
            return columns.getOrDefault(TITLE, -1);
        }

        int controlStatement() {
            return columns.keySet().stream()
                    .filter(h -> h.startsWith(CONTROL_STATEMENT))
                    .findFirst()
                    .map(columns::get)
                    .orElse(-1);
        }

        int discussion() {
            return columns.keySet().stream()
                    .filter(h -> h.startsWith(DISCUSSION))
                    .findFirst()
                    .map(columns::get)
                    .orElse(-1);
        }

        int gapQuestion() {
            return columns.getOrDefault(GAP_QUESTION, -1);
        }

        int relatedControls() {
            return columns.getOrDefault(RELATED_CONTROLS, -1);
        }

        int baseline() {
            return columns.getOrDefault(BASELINE, -1);
        }
    }

    private record Routing(boolean inSoa, boolean inGap, boolean inAudit, boolean inRisk) {
    }

    private static final class Builder {
        private final Nist80053PackParser parser;
        private final Map<String, Draft> byCode = new LinkedHashMap<>();
        private final List<ParsedMasterWorkbook.Question> questions = new ArrayList<>();
        private final List<ImportStagingRow> staging = new ArrayList<>();
        private final List<ImportRowError> errors = new ArrayList<>();
        private final List<String> info = new ArrayList<>();
        private int order;
        private int contentRows;

        Builder(Nist80053PackParser parser) {
            this.parser = parser;
        }

        void ensureFamily(String code, String title, String sheetName) {
            if (!byCode.containsKey(code)) {
                add(code, 1, null, title, 0, false, UNROUTED, Map.of(), sheetName);
            }
        }

        void ensureBase(String code, String familyCode, String title, String statement,
                        int excelRow, String sheetName) {
            Draft existing = byCode.get(code);
            if (existing != null) {
                if (existing.authored) {
                    return;
                }
                existing.title = truncate(title == null ? code : title, MAX_TITLE);
                existing.definition = statement;
                existing.authored = true;
                existing.routing = ALL_ROUTED;
                return;
            }
            add(code, 2, familyCode, truncate(title == null ? code : title, MAX_TITLE), excelRow,
                    true, ALL_ROUTED, Map.of(), sheetName);
            byCode.get(code).definition = statement;
        }

        void leaf(String code, int level, String parent, String title, String statement,
                  Map<String, String> attributes, String gapQuestion, int excelRow, Row row,
                  ColumnLayout columns, String sheetName) {
            Draft existing = byCode.get(code);
            if (existing != null && existing.authored) {
                errors.add(new ImportRowError(excelRow, CONTROL_ID,
                        "Duplicate Control ID '" + code + "'"));
                return;
            }

            Draft draft = existing != null
                    ? existing
                    : add(code, level, parent, truncate(title == null ? code : title, MAX_TITLE),
                    excelRow, true, ALL_ROUTED, attributes, sheetName);
            draft.authored = true;
            draft.title = truncate(title == null ? code : title, MAX_TITLE);
            draft.definition = statement;
            draft.routing = ALL_ROUTED;
            draft.attributes = new LinkedHashMap<>(attributes);

            if (gapQuestion != null && !gapQuestion.isBlank()) {
                questions.add(new ParsedMasterWorkbook.Question(
                        excelRow, code, "Q1", null, gapQuestion, null, null, false,
                        questions.size() + 1));
            }

            staging.add(new ImportStagingRow(sheetName, excelRow, ImportStagingRow.Kind.NODE,
                    parser.rawJson(row, columns), ImportStagingRow.Status.OK,
                    "level " + level + " control '" + code + "'"));
        }

        private Draft add(String code, int level, String parent, String title, int excelRow,
                          boolean authored, Routing routing, Map<String, String> attributes,
                          String sheetName) {
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
            draft.sheetName = sheetName;
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
        @SuppressWarnings("unused")
        private String sheetName;
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
