package com.a9.arbor.grc.catalog.excel;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Parser for the client's Master Import workbook (layout S1).
 *
 * <p>Shape notes, all verified against {@code Arbor_GRC_Framework_Master_Import.xlsx}:
 * <ul>
 *   <li>Headers are on Excel row 3; rows 1–2 are titles, and single-cell banner rows are
 *       interleaved between data rows.</li>
 *   <li>Hierarchy is wide, not tall: every row repeats its {@code level1}–{@code level4}
 *       code/name pairs, so ancestors are emitted once and the leaf is the deepest pair.</li>
 *   <li>Adjacent level codes are frequently repeated ({@code level2_code == level3_code} on 71 rows,
 *       {@code level3_code == level4_code} on 31) and collapse to a single node.</li>
 *   <li>A leaf code may come from {@code official_ref} — see
 *       {@link #substitutedLeafCode(String, String)}.</li>
 * </ul>
 */
@Component
public class MasterImportParser {

    /** Every sheet in this workbook puts its headers on Excel row 3. */
    private static final int HEADER_ROW_INDEX = 2;

    private static final String FRAMEWORK_CODE = "framework_code";
    private static final String FRAMEWORK_VERSION = "framework_version";
    private static final String MODULE_CODE = "module_code";
    private static final String RECORD_TYPE = "record_type";
    private static final String OFFICIAL_REF = "official_ref";
    private static final String DEFINITION_TEXT = "definition_text";
    private static final String DISPLAY_ORDER = "display_order";
    private static final String CONTROL_CODE = "control_code";
    private static final String QUESTION_NUMBER = "question_number";
    private static final String QUESTION_TYPE = "question_type";
    private static final String QUESTION_TEXT = "question_text";
    private static final String ANSWER_OPTIONS = "answer_options";
    private static final String EVIDENCE_GUIDANCE = "evidence_guidance";
    private static final String IS_MANDATORY = "is_mandatory";

    private static final List<String> ROUTING_FLAGS =
            List.of("in_soa", "in_gap_assessment", "in_audit_checklist", "in_risk_register");

    /** Columns kept verbatim in {@code framework_node_attribute} rather than typed columns. */
    private static final List<String> ATTRIBUTE_COLUMNS = List.of(
            "section_type", IS_MANDATORY, "cross_ref_framework", "cross_ref_control", "notes");

    private static final String PLACEHOLDER_PREFIX = "[TO BE FILLED";

    /**
     * The sheet's {@code framework_code} is a detection alias only — {@code framework.code} stays the
     * enum name so re-import updates the existing draft instead of forking a parallel row.
     */
    private static final Map<String, FrameworkTypeCode> TYPE_BY_SOURCE_CODE = Map.of(
            "ISO27001_2022", FrameworkTypeCode.ISO_27001,
            "ISO27017_2015", FrameworkTypeCode.ISO_27017,
            "ISO27701_2019", FrameworkTypeCode.ISO_27701,
            "ISO22301_2019", FrameworkTypeCode.ISO_22301,
            "ISO20000_2018", FrameworkTypeCode.ISO_20000,
            "ISO42001_2023", FrameworkTypeCode.ISO_42001,
            "ISO31000_2018", FrameworkTypeCode.ISO_31000);

    private static final int MAX_LEVEL = 4;
    private static final int MAX_CODE_LENGTH = 64;

    private final DataFormatter formatter = new DataFormatter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static boolean supportsSourceCode(String sourceFrameworkCode) {
        return TYPE_BY_SOURCE_CODE.containsKey(sourceFrameworkCode);
    }

    public boolean isMasterWorkbook(byte[] bytes) {
        try (Workbook workbook = open(bytes)) {
            return isMasterWorkbook(workbook);
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isMasterWorkbook(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (isFrameworkShaped(columns(workbook.getSheetAt(i)))) {
                return true;
            }
        }
        return false;
    }

    public ParsedMasterWorkbook parse(byte[] bytes) {
        try (Workbook workbook = open(bytes)) {
            return parse(workbook);
        } catch (IOException e) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "file", "Could not read Excel: " + e.getMessage())));
        }
    }

    private ParsedMasterWorkbook parse(Workbook workbook) {
        List<SheetTable> frameworkSheets = new ArrayList<>();
        Map<String, List<DataRow>> questionsBySourceCode = new LinkedHashMap<>();
        Map<String, String> questionSheetBySourceCode = new HashMap<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            Map<String, Integer> columns = columns(sheet);
            if (isFrameworkShaped(columns)) {
                frameworkSheets.add(read(sheet, columns));
            } else if (isQuestionShaped(columns)) {
                SheetTable table = read(sheet, columns);
                for (DataRow row : table.rows()) {
                    String code = row.get(FRAMEWORK_CODE);
                    questionsBySourceCode.computeIfAbsent(code, k -> new ArrayList<>()).add(row);
                    questionSheetBySourceCode.putIfAbsent(code, table.name());
                }
            }
        }

        if (frameworkSheets.isEmpty()) {
            throw new ImportValidationException(List.of(new ImportRowError(0, "file",
                    "Not a Master Import workbook (no sheet with framework_code + level1_code "
                            + "headers on row 3)")));
        }

        List<ParsedMasterWorkbook.FrameworkSheet> frameworks = new ArrayList<>();
        List<ParsedMasterWorkbook.SkippedSheet> skipped = new ArrayList<>();

        for (SheetTable table : frameworkSheets) {
            // A sheet can hold more than one framework — 15 UAE_Template carries three.
            Map<String, List<DataRow>> byCode = new LinkedHashMap<>();
            for (DataRow row : table.rows()) {
                byCode.computeIfAbsent(row.get(FRAMEWORK_CODE), k -> new ArrayList<>()).add(row);
            }
            boolean bannersPending = true;
            for (Map.Entry<String, List<DataRow>> group : byCode.entrySet()) {
                String sourceCode = group.getKey();
                List<DataRow> rows = group.getValue();
                if (rows.stream().allMatch(DataRow::isPlaceholder)) {
                    skipped.add(new ParsedMasterWorkbook.SkippedSheet(
                            table.name(), sourceCode,
                            "skipped, placeholder — every row is " + PLACEHOLDER_PREFIX + "…]",
                            placeholderStaging(table.name(), rows)));
                    continue;
                }
                FrameworkTypeCode type = TYPE_BY_SOURCE_CODE.get(sourceCode);
                if (type == null) {
                    skipped.add(new ParsedMasterWorkbook.SkippedSheet(
                            table.name(), sourceCode,
                            "skipped, unmapped framework_code — no FrameworkTypeCode alias",
                            staging(table.name(), rows, ImportStagingRow.Status.SKIPPED,
                                    "Unmapped framework_code " + sourceCode)));
                    continue;
                }
                frameworks.add(buildFramework(table, sourceCode, type, rows,
                        questionsBySourceCode.getOrDefault(sourceCode, List.of()),
                        questionSheetBySourceCode.get(sourceCode),
                        bannersPending));
                bannersPending = false;
            }
        }

        return new ParsedMasterWorkbook(List.copyOf(frameworks), List.copyOf(skipped));
    }

    // ------------------------------------------------------------------ framework sheet

    private ParsedMasterWorkbook.FrameworkSheet buildFramework(SheetTable table,
                                                               String sourceCode,
                                                               FrameworkTypeCode type,
                                                               List<DataRow> rows,
                                                               List<DataRow> questionRows,
                                                               String questionSheetName,
                                                               boolean includeBanners) {
        List<ImportRowError> errors = new ArrayList<>();
        List<String> info = new ArrayList<>();
        List<ImportStagingRow> staging =
                includeBanners ? new ArrayList<>(table.banners()) : new ArrayList<>();

        String version = rows.stream()
                .map(r -> r.get(FRAMEWORK_VERSION))
                .filter(v -> !v.isBlank())
                .findFirst()
                .orElse(null);
        if (version == null) {
            errors.add(new ImportRowError(rows.get(0).excelRow(), FRAMEWORK_VERSION,
                    "framework_version is required — cannot resolve the framework version"));
            version = type.defaultVersion();
        }

        Map<Integer, List<String>> chains = new LinkedHashMap<>();
        Map<Integer, String> rejected = new LinkedHashMap<>();
        Map<Integer, Map<String, String>> names = new LinkedHashMap<>();

        // Pass 1 — collapse the wide level columns, then decide official_ref substitutions. The
        // substitution has to be settled before any node is emitted, because it changes which code
        // is the leaf and which is its parent.
        for (DataRow row : rows) {
            String rowVersion = row.get(FRAMEWORK_VERSION);
            if (!rowVersion.isBlank() && !rowVersion.equals(version)) {
                reject(rejected, errors, row, FRAMEWORK_VERSION,
                        "framework_version '" + rowVersion + "' differs from '" + version
                                + "' used by the rest of " + sourceCode);
                continue;
            }
            String rowModule = row.get(MODULE_CODE);
            if (!rowModule.equalsIgnoreCase(type.moduleCode().name())) {
                reject(rejected, errors, row, MODULE_CODE,
                        "module_code '" + rowModule + "' does not match " + type.name()
                                + " module " + type.moduleCode().name());
                continue;
            }
            if (row.isPlaceholder()) {
                reject(rejected, errors, row, DEFINITION_TEXT,
                        "definition_text is still " + PLACEHOLDER_PREFIX + "…]");
                continue;
            }
            List<String> chain = new ArrayList<>();
            Map<String, String> chainNames = new LinkedHashMap<>();
            for (int level = 1; level <= MAX_LEVEL; level++) {
                String code = row.get("level" + level + "_code");
                String name = row.get("level" + level + "_name");
                if (code.isBlank()) {
                    continue;
                }
                if (!chain.isEmpty() && chain.get(chain.size() - 1).equals(code)) {
                    // Adjacent repeat of the same code — one node, keep the first non-blank name.
                    chainNames.merge(code, name, (kept, next) -> kept.isBlank() ? next : kept);
                    continue;
                }
                chain.add(code);
                chainNames.merge(code, name, (kept, next) -> kept.isBlank() ? next : kept);
            }
            if (chain.isEmpty()) {
                reject(rejected, errors, row, "level1_code",
                        "no level code on the row — level1_code…level4_code are all blank");
                continue;
            }
            chains.put(row.excelRow(), chain);
            names.put(row.excelRow(), chainNames);
        }

        applySubstitutions(rows, chains, names, rejected, info);

        // Pass 2 — emit ancestors once, then the leaf carrying the row's routing and content.
        Map<String, NodeDraft> nodes = new LinkedHashMap<>();
        Set<String> claimedLeaves = new HashSet<>();
        for (DataRow row : rows) {
            List<String> chain = chains.get(row.excelRow());
            if (chain == null) {
                continue;
            }
            if (chain.size() > MAX_LEVEL) {
                reject(rejected, errors, row, "level" + MAX_LEVEL + "_code",
                        "hierarchy is " + chain.size() + " levels deep; the catalog supports "
                                + MAX_LEVEL);
                continue;
            }
            String leafCode = chain.get(chain.size() - 1);
            if (!claimedLeaves.add(leafCode)) {
                reject(rejected, errors, row, "level_code",
                        "duplicate node code '" + leafCode + "' — already imported from an earlier row");
                continue;
            }
            if (leafCode.length() > MAX_CODE_LENGTH) {
                reject(rejected, errors, row, "level_code",
                        "node code '" + leafCode + "' exceeds " + MAX_CODE_LENGTH + " characters");
                continue;
            }
            Integer conflict = levelConflict(nodes, chain);
            if (conflict != null) {
                reject(rejected, errors, row, "level_code",
                        "code '" + chain.get(conflict) + "' already exists at level "
                                + nodes.get(chain.get(conflict)).level + ", not " + (conflict + 1));
                continue;
            }

            Map<String, String> chainNames = names.get(row.excelRow());
            int displayOrder = parseOrder(row.get(DISPLAY_ORDER), row.excelRow());
            boolean leafIsNew = !nodes.containsKey(leafCode);
            if (!leafIsNew) {
                // The code was emitted as an ancestor by an earlier row, so this row's content has
                // nowhere unambiguous to land. Report it rather than overwrite the parent.
                reject(rejected, errors, row, "level_code",
                        "code '" + leafCode + "' is already a parent node — it cannot also carry "
                                + "row content");
                continue;
            }
            for (int i = 0; i < chain.size(); i++) {
                String code = chain.get(i);
                if (nodes.containsKey(code)) {
                    continue;
                }
                String title = chainNames.getOrDefault(code, "");
                nodes.put(code, new NodeDraft(
                        row.excelRow(),
                        i + 1,
                        i == 0 ? null : chain.get(i - 1),
                        code,
                        title.isBlank() ? code : title,
                        displayOrder));
            }
            nodes.get(leafCode).applyLeaf(row);
        }

        List<ParsedFrameworkRow> parsed = new ArrayList<>();
        for (NodeDraft draft : nodes.values()) {
            parsed.add(draft.toParsedRow());
        }

        for (DataRow row : rows) {
            String message = rejected.get(row.excelRow());
            staging.add(new ImportStagingRow(
                    table.name(), row.excelRow(),
                    ImportStagingRow.Kind.NODE,
                    json(row.values()),
                    message == null
                            ? ImportStagingRow.Status.OK
                            : ImportStagingRow.Status.ERROR,
                    message));
        }

        List<ParsedMasterWorkbook.Question> questions = buildQuestions(
                questionSheetName, questionRows, nodes.keySet(), errors, staging);

        // Belt and braces: parentCode/level are derived from the chain so this cannot fail on
        // well-formed input. If it ever does, the row detail is worth surfacing over a 500.
        ParsedFrameworkRowValidator.validateOrThrow(parsed, List.of());

        return new ParsedMasterWorkbook.FrameworkSheet(
                table.name(),
                questionSheetName,
                sourceCode,
                type,
                version,
                type.moduleCode(),
                List.copyOf(parsed),
                List.copyOf(questions),
                List.copyOf(staging),
                List.copyOf(errors),
                List.copyOf(info),
                rows.size());
    }

    /**
     * Decides which rows take their leaf code from {@code official_ref} instead of the deepest level
     * code, and records one INFO line per substitution.
     *
     * <p>Needed because ISO 42001's {@code level3_code} is authored at section granularity (10 rows
     * collapse onto 8 codes) while {@code official_ref} and the client's own question bank are at
     * control granularity. Deliberately independent of whether questions resolve, so hierarchy never
     * depends on parse order.
     */
    private void applySubstitutions(List<DataRow> rows,
                                    Map<Integer, List<String>> chains,
                                    Map<Integer, Map<String, String>> names,
                                    Map<Integer, String> rejected,
                                    List<String> info) {
        Map<Integer, String> candidate = new LinkedHashMap<>();
        Set<String> occupied = new HashSet<>();
        for (Map.Entry<Integer, List<String>> entry : chains.entrySet()) {
            occupied.addAll(entry.getValue());
        }
        Map<String, Integer> candidateCount = new HashMap<>();
        for (DataRow row : rows) {
            List<String> chain = chains.get(row.excelRow());
            if (chain == null || rejected.containsKey(row.excelRow())) {
                continue;
            }
            String substituted = substitutedLeafCode(chain.get(chain.size() - 1), row.get(OFFICIAL_REF));
            if (substituted != null && !occupied.contains(substituted)) {
                candidate.put(row.excelRow(), substituted);
                candidateCount.merge(substituted, 1, Integer::sum);
            }
        }
        for (Map.Entry<Integer, String> entry : candidate.entrySet()) {
            String substituted = entry.getValue();
            if (candidateCount.get(substituted) > 1) {
                // Not unique, so it is not an identity — leave the row on its level code and let the
                // duplicate-code check report it.
                continue;
            }
            List<String> chain = chains.get(entry.getKey());
            String parent = chain.get(chain.size() - 1);
            chain.add(substituted);
            names.get(entry.getKey()).put(substituted,
                    names.get(entry.getKey()).getOrDefault(parent, substituted));
            info.add("Row " + entry.getKey() + ": node code taken from official_ref '"
                    + substituted + "' (deepest level code '" + parent + "' is a section, "
                    + "now its parent)");
        }
    }

    /**
     * The substitution trigger: {@code official_ref} is a bare ref rather than prose, and the
     * deepest level code is a proper prefix of it. Uniqueness is checked by the caller.
     *
     * @return the code to use, or {@code null} to keep the level code
     */
    private static String substitutedLeafCode(String leafCode, String officialRef) {
        if (officialRef == null || officialRef.isBlank()) {
            return null;
        }
        for (int i = 0; i < officialRef.length(); i++) {
            if (Character.isWhitespace(officialRef.charAt(i))) {
                return null;  // prose, e.g. "Clause 6.2"
            }
        }
        if (officialRef.length() <= leafCode.length() || !officialRef.startsWith(leafCode)) {
            return null;
        }
        return officialRef;
    }

    private static Integer levelConflict(Map<String, NodeDraft> nodes, List<String> chain) {
        for (int i = 0; i < chain.size(); i++) {
            NodeDraft existing = nodes.get(chain.get(i));
            if (existing != null && existing.level != i + 1) {
                return i;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ questions

    private List<ParsedMasterWorkbook.Question> buildQuestions(String sheetName,
                                                               List<DataRow> rows,
                                                               Set<String> nodeCodes,
                                                               List<ImportRowError> errors,
                                                               List<ImportStagingRow> staging) {
        List<ParsedMasterWorkbook.Question> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int order = 0;
        for (DataRow row : rows) {
            order++;
            String controlCode = row.get(CONTROL_CODE);
            String questionNumber = row.get(QUESTION_NUMBER);
            String questionText = row.get(QUESTION_TEXT);
            String message = null;
            if (controlCode.isBlank() || questionNumber.isBlank() || questionText.isBlank()) {
                message = "control_code, question_number and question_text are all required";
            } else if (!nodeCodes.contains(controlCode)) {
                message = "control_code '" + controlCode + "' does not match any imported node code";
            } else if (!seen.add(controlCode + '\u0000' + questionNumber)) {
                message = "duplicate question " + questionNumber + " for control " + controlCode;
            } else if (controlCode.length() + questionNumber.length() + 1 > MAX_CODE_LENGTH) {
                message = "control_code + question_number exceeds " + MAX_CODE_LENGTH + " characters";
            }

            if (message == null) {
                out.add(new ParsedMasterWorkbook.Question(
                        row.excelRow(),
                        controlCode,
                        questionNumber,
                        blankToNull(row.get(QUESTION_TYPE)),
                        questionText,
                        blankToNull(row.get(ANSWER_OPTIONS)),
                        blankToNull(row.get(EVIDENCE_GUIDANCE)),
                        "YES".equalsIgnoreCase(row.get(IS_MANDATORY)),
                        parseOrder(row.get(DISPLAY_ORDER), order)));
            } else {
                errors.add(new ImportRowError(row.excelRow(), CONTROL_CODE, message));
            }
            staging.add(new ImportStagingRow(
                    sheetName, row.excelRow(),
                    ImportStagingRow.Kind.QUESTION,
                    json(row.values()),
                    message == null
                            ? ImportStagingRow.Status.OK
                            : ImportStagingRow.Status.ERROR,
                    message));
        }
        return out;
    }

    // ------------------------------------------------------------------ sheet reading

    private SheetTable read(Sheet sheet, Map<String, Integer> columns) {
        List<DataRow> rows = new ArrayList<>();
        List<ImportStagingRow> banners = new ArrayList<>();
        for (int r = HEADER_ROW_INDEX + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int excelRow = r + 1;
            Map<String, String> values = new LinkedHashMap<>();
            boolean anyValue = false;
            boolean anyBeyondFirst = false;
            for (Map.Entry<String, Integer> column : columns.entrySet()) {
                String value = cell(row, column.getValue());
                values.put(column.getKey(), value);
                if (!value.isBlank()) {
                    anyValue = true;
                    if (column.getValue() > 0) {
                        anyBeyondFirst = true;
                    }
                }
            }
            if (!anyValue) {
                continue;
            }
            if (!anyBeyondFirst) {
                // Section banner spanning the sheet, e.g. "ANNEX A CONTROLS — 93 controls".
                banners.add(new ImportStagingRow(
                        sheet.getSheetName(), excelRow,
                        ImportStagingRow.Kind.BANNER,
                        json(values),
                        ImportStagingRow.Status.SKIPPED,
                        "banner row"));
                continue;
            }
            rows.add(new DataRow(excelRow, values));
        }
        return new SheetTable(sheet.getSheetName(), rows, banners);
    }

    private Map<String, Integer> columns(Sheet sheet) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        Row header = sheet.getRow(HEADER_ROW_INDEX);
        if (header == null) {
            return columns;
        }
        for (Cell cell : header) {
            String name = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            if (!name.isBlank()) {
                columns.putIfAbsent(name, cell.getColumnIndex());
            }
        }
        return columns;
    }

    private static boolean isFrameworkShaped(Map<String, Integer> columns) {
        return columns.containsKey(FRAMEWORK_CODE) && columns.containsKey("level1_code");
    }

    private static boolean isQuestionShaped(Map<String, Integer> columns) {
        return columns.containsKey(FRAMEWORK_CODE) && columns.containsKey(QUESTION_NUMBER);
    }

    private List<ImportStagingRow> placeholderStaging(String sheetName,
                                                                     List<DataRow> rows) {
        List<ImportStagingRow> out = new ArrayList<>();
        for (DataRow row : rows) {
            out.add(new ImportStagingRow(
                    sheetName, row.excelRow(),
                    ImportStagingRow.Kind.PLACEHOLDER,
                    json(row.values()),
                    ImportStagingRow.Status.SKIPPED,
                    "skipped, placeholder"));
        }
        return out;
    }

    private List<ImportStagingRow> staging(String sheetName,
                                                          List<DataRow> rows,
                                                          ImportStagingRow.Status status,
                                                          String message) {
        List<ImportStagingRow> out = new ArrayList<>();
        for (DataRow row : rows) {
            out.add(new ImportStagingRow(
                    sheetName, row.excelRow(),
                    ImportStagingRow.Kind.NODE,
                    json(row.values()), status, message));
        }
        return out;
    }

    private static void reject(Map<Integer, String> rejected,
                               List<ImportRowError> errors,
                               DataRow row,
                               String field,
                               String message) {
        if (rejected.putIfAbsent(row.excelRow(), message) == null) {
            errors.add(new ImportRowError(row.excelRow(), field, message));
        }
    }

    private String json(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String cell(Row row, Integer index) {
        if (index == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static int parseOrder(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Workbook open(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return new XSSFWorkbook(in);
        }
    }

    private record SheetTable(String name,
                              List<DataRow> rows,
                              List<ImportStagingRow> banners) {
    }

    private record DataRow(int excelRow, Map<String, String> values) {
        String get(String column) {
            return values.getOrDefault(column, "");
        }

        boolean isPlaceholder() {
            return get(DEFINITION_TEXT).toUpperCase(Locale.ROOT).startsWith(PLACEHOLDER_PREFIX);
        }
    }

    /** Mutable while the wide rows are folded together; becomes a {@link ParsedFrameworkRow}. */
    private static final class NodeDraft {
        private final int excelRow;
        private final int level;
        private final String parentCode;
        private final String code;
        private final String title;
        private final int displayOrder;

        private String definition;
        private String recordType;
        private String officialRef;
        private boolean inSoa;
        private boolean inGapAssessment;
        private boolean inAuditChecklist;
        private boolean inRiskRegister;
        private final Map<String, String> attributes = new LinkedHashMap<>();

        NodeDraft(int excelRow, int level, String parentCode, String code, String title, int displayOrder) {
            this.excelRow = excelRow;
            this.level = level;
            this.parentCode = parentCode;
            this.code = code;
            this.title = title;
            this.displayOrder = displayOrder;
        }

        void applyLeaf(DataRow row) {
            definition = blankToNull(row.get(DEFINITION_TEXT));
            recordType = blankToNull(row.get(RECORD_TYPE));
            officialRef = blankToNull(row.get(OFFICIAL_REF));
            inSoa = isYes(row.get(ROUTING_FLAGS.get(0)));
            inGapAssessment = isYes(row.get(ROUTING_FLAGS.get(1)));
            inAuditChecklist = isYes(row.get(ROUTING_FLAGS.get(2)));
            inRiskRegister = isYes(row.get(ROUTING_FLAGS.get(3)));
            for (String column : ATTRIBUTE_COLUMNS) {
                String value = row.get(column);
                if (!value.isBlank()) {
                    attributes.put(column, value);
                }
            }
        }

        ParsedFrameworkRow toParsedRow() {
            return new ParsedFrameworkRow(
                    excelRow, level, parentCode, code, title, definition,
                    null, null, displayOrder, recordType, officialRef,
                    inSoa, inGapAssessment, inAuditChecklist, inRiskRegister, attributes);
        }

        private static boolean isYes(String value) {
            return "YES".equalsIgnoreCase(value);
        }
    }
}
