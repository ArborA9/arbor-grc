package com.a9.arbor.grc.catalog.excel;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the client's reference packs (layout S4).
 *
 * <p>Shape notes:
 * <ul>
 *   <li>Headers sit on row 3 in the ISO packs and row 4 in the UAE IA pack, so the header row is
 *       found rather than assumed.</li>
 *   <li>The ref column is not always the first: the ISO 27001 Annex A sheet leads with
 *       {@code Theme}, and the UAE IA pack with {@code Priority} / {@code Applicability}.</li>
 *   <li>The question block is labelled {@code Q1..Q5} in three packs and {@code Question 1..6} in
 *       two others, and its width varies per sheet (4, 5 or 6).</li>
 *   <li>Section banners are single-cell rows interleaved with data, as in the Master workbook.</li>
 * </ul>
 *
 * <p>A sheet is only importable when it has a ref column, at least one <em>numbered</em> question
 * column, and a token in its name that maps to a catalog framework. The rest are reported as skipped
 * rather than guessed at: the standalone ISO 22301 and PCI/NIST packs carry a single
 * {@code Gap Assessment Question} column and stay reference-only.
 *
 * <p>Since F3b the parse output feeds two different paths — enrichment of a framework the Master
 * import created, and creation of one it did not (see {@link PackFrameworkBuilder}). Which applies is
 * decided downstream; this class only reports what the workbook says.
 */
@Component
public class ReferencePackParser {

    /** Rows 1–2 are titles in every pack seen; 6 leaves room without risking a data row. */
    private static final int MAX_HEADER_SEARCH_ROWS = 6;

    /**
     * The ref column's header, which each pack words differently: {@code Ref} (UAE IA),
     * {@code Clause Ref}, {@code Control Ref}, {@code Article Ref} (PDPL), {@code Category Ref}
     * (NIST CSF). Anchored on both ends so it cannot swallow {@code Control Name},
     * {@code Article / Topic Title} or {@code Category Title}, which sit next to it.
     */
    private static final Pattern REF_HEADER = Pattern.compile(
            "^(?:clause\\s*/\\s*(?:control|sub-?control|principle)"
                    + "|clause|control|article|category|sub-?control|ctrl\\s*#?|req"
                    + "|ref|reference)"
                    + "(?:\\s*(?:ref|reference|id|#))?$");

    private static final Pattern SUB_CONTROL_REF_HEADER = Pattern.compile(
            "^sub-?control\\s*(?:ref|reference|id|#)?$");

    private static final Pattern QUESTION_HEADER = Pattern.compile("^(?:q|question)\\s*(\\d+)$");

    private static final Pattern TITLE_HEADER = Pattern.compile("^.*\\b(title|name)$");

    private static final String WHAT_IT_MEANS_PREFIX = "what it means";
    private static final String EVIDENCE_PREFIX = "evidence";
    private static final String GAP_QUESTION_HEADER = "gap assessment question";
    private static final String PRIORITY_PREFIX = "priority";
    private static final String APPLICABILITY_PREFIX = "applicability";

    /**
     * The verbatim requirement text, whose header differs per pack: {@code Statement / Sub-Control
     * Requirement}, {@code Legal Requirement}, {@code Description}, {@code Outcome}. Ordered, because
     * a sheet can carry more than one — UAE IA has {@code Statement}, NIST CSF has {@code Outcome}.
     */
    private static final List<String> DEFINITION_PREFIXES =
            List.of("statement", "legal requirement", "description", "outcome", "requirement",
                    "control consideration");

    /**
     * Sheet-name token to catalog framework. Keyed on the standard's number because the pack sheet
     * names are prose and inconsistent ({@code ISO27001 Annex A Controls}, {@code ISO 20000-1 ITSM},
     * {@code ISO 22301 BCMS}); the two UAE frameworks are keyed on letters because their sheets carry
     * no distinguishing number.
     *
     * <p>Resolving a type here does not imply the framework may be created from the pack — that is
     * {@code FrameworkLibraryService.CREATE_FROM_PACK}'s call. The frameworks still absent (NCEMA,
     * GDPR, CIS, NIST CSF) land in F3b-2 / F3c.
     */
    private static final Map<String, FrameworkTypeCode> TYPE_BY_TOKEN = Map.ofEntries(
            Map.entry("27001", FrameworkTypeCode.ISO_27001),
            // Cloud sheet lives inside the ISO 27001 workbook as a tab of that framework (V2).
            Map.entry("27017", FrameworkTypeCode.ISO_27001),
            Map.entry("27701", FrameworkTypeCode.ISO_27701),
            Map.entry("22301", FrameworkTypeCode.ISO_22301),
            Map.entry("20000", FrameworkTypeCode.ISO_20000),
            Map.entry("42001", FrameworkTypeCode.ISO_42001),
            Map.entry("31000", FrameworkTypeCode.ISO_31000),
            Map.entry("9001", FrameworkTypeCode.ISO_9001),
            Map.entry("45001", FrameworkTypeCode.ISO_45001),
            Map.entry("UAEIA", FrameworkTypeCode.UAE_IA),
            Map.entry("PDPL", FrameworkTypeCode.UAE_PDPL),
            Map.entry("NISTCSF", FrameworkTypeCode.NIST_CSF),
            Map.entry("CISCONTROLS", FrameworkTypeCode.CIS_V8),
            Map.entry("CISCONTROL", FrameworkTypeCode.CIS_V8),
            Map.entry("GDPR", FrameworkTypeCode.GDPR),
            Map.entry("NCEMA", FrameworkTypeCode.NCEMA_V3),
            Map.entry("SAMACSF", FrameworkTypeCode.SAMA_CSF),
            Map.entry("SAMA", FrameworkTypeCode.SAMA_CSF),
            Map.entry("ERMGUIDE", FrameworkTypeCode.ISO_31000));

    private final DataFormatter formatter = new DataFormatter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isReferencePack(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (layout(workbook.getSheetAt(i)) != null) {
                return true;
            }
        }
        return false;
    }

    public ParsedReferencePack parse(byte[] bytes) {
        return parse(bytes, null);
    }

    public ParsedReferencePack parse(byte[] bytes, FrameworkTypeCode defaultType) {
        try (Workbook workbook = open(bytes)) {
            return parse(workbook, defaultType);
        } catch (IOException e) {
            throw new ImportValidationException(List.of(
                    new ImportRowError(0, "file", "Could not read Excel: " + e.getMessage())));
        }
    }

    private ParsedReferencePack parse(Workbook workbook) {
        return parse(workbook, null);
    }

    private ParsedReferencePack parse(Workbook workbook, FrameworkTypeCode defaultType) {
        List<ParsedReferencePack.Sheet> sheets = new ArrayList<>();
        List<ParsedReferencePack.SkippedSheet> skipped = new ArrayList<>();
        List<ParsedReferencePack.DomainObjective> domainObjectives = new ArrayList<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            List<ParsedReferencePack.DomainObjective> objectives = readDomainObjectives(sheet);
            if (!objectives.isEmpty()) {
                domainObjectives.addAll(objectives);
                continue;
            }
            Layout layout = layout(sheet);
            if (layout == null) {
                continue;  // Read Me tabs and other prose — not worth a report line.
            }
            FrameworkTypeCode type = typeFor(sheet.getSheetName());
            if (type == null && defaultType != null) {
                type = defaultType;
            }
            if (type == null) {
                skipped.add(new ParsedReferencePack.SkippedSheet(sheet.getSheetName(),
                        "skipped, no catalog framework — the sheet name carries no known standard "
                                + "token (and no filename default type was supplied)",
                        List.of()));
                continue;
            }
            sheets.add(read(sheet, layout, type));
        }

        if (sheets.isEmpty() && skipped.isEmpty()) {
            throw new ImportValidationException(List.of(new ImportRowError(0, "file",
                    "Not a reference pack (no sheet with a clause/control ref column)")));
        }
        return new ParsedReferencePack(List.copyOf(sheets), List.copyOf(skipped),
                List.copyOf(domainObjectives));
    }

    /**
     * UAE IA {@code Domain Objectives} sheet — Domain / Sub-Domain / Title / Objective.
     * Returns empty when the sheet is not that layout.
     */
    private List<ParsedReferencePack.DomainObjective> readDomainObjectives(Sheet sheet) {
        Integer domainCol = null;
        Integer subDomainCol = null;
        Integer titleCol = null;
        Integer objectiveCol = null;
        int headerRow = -1;
        int last = Math.min(sheet.getLastRowNum(), MAX_HEADER_SEARCH_ROWS - 1);
        for (int r = 0; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (Cell cell : row) {
                String name = header(cell);
                if (!name.isBlank()) {
                    columns.putIfAbsent(name, cell.getColumnIndex());
                }
            }
            Integer domain = columns.get("domain");
            Integer objective = columns.get("objective");
            if (domain == null || objective == null) {
                continue;
            }
            // Require Domain + Objective; Sub-Domain / Title optional but expected for UAE IA.
            domainCol = domain;
            subDomainCol = columns.get("sub-domain");
            if (subDomainCol == null) {
                subDomainCol = columns.get("sub domain");
            }
            titleCol = columns.get("title");
            objectiveCol = objective;
            headerRow = r;
            break;
        }
        if (headerRow < 0) {
            return List.of();
        }

        List<ParsedReferencePack.DomainObjective> out = new ArrayList<>();
        for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String domain = cell(row, domainCol);
            String sub = cell(row, subDomainCol);
            String title = cell(row, titleCol);
            String objective = cell(row, objectiveCol);
            if (objective.isBlank()) {
                continue;
            }
            String code = !sub.isBlank() ? sub.trim() : domain.trim();
            if (code.isBlank()) {
                continue;
            }
            out.add(new ParsedReferencePack.DomainObjective(
                    code,
                    blankToNull(title),
                    objective.trim()));
        }
        return List.copyOf(out);
    }

    private ParsedReferencePack.Sheet read(Sheet sheet, Layout layout, FrameworkTypeCode type) {
        List<ParsedReferencePack.ReferenceRow> rows = new ArrayList<>();
        List<ParsedReferencePack.Banner> banners = new ArrayList<>();
        List<ImportRowError> errors = new ArrayList<>();
        int contentRows = 0;
        // SAMA stores What it means / Evidence / Questions / Applicability once at the top of a
        // vertical merge. Excel keeps the value only in that top cell. Copy it down the block.
        // Horizontal banner merges (a title across the row) are left alone so they stay banners.
        Map<Long, String> mergedDown = verticalMergeFill(sheet);

        for (int r = layout.headerRowIndex() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            int excelRow = r + 1;
            Map<String, String> values = new LinkedHashMap<>();
            boolean anyValue = false;
            boolean anyBeyondFirst = false;
            for (Map.Entry<String, Integer> column : layout.columns().entrySet()) {
                String value = cell(row, column.getValue(), mergedDown);
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
                // Only column 0 is populated, which is what makes it a banner, so that is its text.
                banners.add(new ParsedReferencePack.Banner(excelRow, cell(row, 0, mergedDown),
                        new ImportStagingRow(sheet.getSheetName(), excelRow,
                                ImportStagingRow.Kind.BANNER, json(values),
                                ImportStagingRow.Status.SKIPPED, "banner row")));
                continue;
            }
            contentRows++;

            String controlRef = cell(row, layout.refColumn(), mergedDown);
            String subRef = cell(row, layout.subControlRefColumn(), mergedDown);
            String domain = cell(row, layout.domainColumn(), mergedDown);
            String subdomainRaw = cell(row, layout.subdomainColumn(), mergedDown);
            String ctrlNum = cell(row, layout.ctrlColumn(), mergedDown);

            // SAMA-style sheets: Domain/Subdomain header rows have no Ctrl # — skip.
            if (layout.ctrlColumn() != null && ctrlNum.isBlank()) {
                continue;
            }

            String ref;
            String structuralParent = null;
            if (layout.ctrlColumn() != null && !ctrlNum.isBlank() && !subdomainRaw.isBlank()) {
                // SAMA: unique leaf = subdomainCode-ctrl#
                String subdomainCode = firstToken(subdomainRaw);
                structuralParent = subdomainCode;
                ref = subdomainCode + "-" + ctrlNum.trim();
            } else if (!subRef.isBlank()) {
                // UAE IA sub-control rows: Sub-Control Ref is the assessable leaf.
                ref = subRef;
            } else {
                ref = controlRef;
            }
            if (ref.isBlank()) {
                errors.add(new ImportRowError(excelRow, "ref",
                        "no clause/control ref on the row — nothing to match against the framework"));
                continue;
            }
            String title = blankToNull(cell(row, layout.titleColumn(), mergedDown));
            String definition = blankToNull(cell(row, layout.definitionColumn(), mergedDown));
            // SAMA: title header often matches "Subdomain … Name"; use control consideration as title.
            if (layout.ctrlColumn() != null) {
                if (definition != null) {
                    title = definition;
                } else if (title == null && !domain.isBlank()) {
                    title = blankToNull(subdomainRaw);
                }
            }
            List<String> questions = new ArrayList<>();
            for (Integer column : layout.questionColumns().values()) {
                String text = cell(row, column, mergedDown);
                if (!text.isBlank()) {
                    questions.add(text);
                }
            }
            // SAMA single "Assessment Questions" column.
            if (questions.isEmpty() && layout.assessmentQuestionsColumn() != null) {
                String text = cell(row, layout.assessmentQuestionsColumn(), mergedDown);
                if (!text.isBlank()) {
                    questions.add(text);
                }
            }
            rows.add(new ParsedReferencePack.ReferenceRow(
                    excelRow,
                    ref,
                    title,
                    definition,
                    blankToNull(cell(row, layout.priorityColumn(), mergedDown)),
                    blankToNull(cell(row, layout.applicabilityColumn(), mergedDown)),
                    List.copyOf(questions),
                    blankToNull(cell(row, layout.whatItMeansColumn(), mergedDown)),
                    blankToNull(cell(row, layout.evidenceColumn(), mergedDown)),
                    json(values),
                    structuralParent));
        }

        return new ParsedReferencePack.Sheet(sheet.getSheetName(), type, List.copyOf(rows),
                List.copyOf(banners), List.copyOf(errors), contentRows);
    }

    // ------------------------------------------------------------------ layout discovery

    /**
     * @return the sheet's column layout, or null when no row in the first
     *         {@value #MAX_HEADER_SEARCH_ROWS} looks like a reference-pack header
     */
    private Layout layout(Sheet sheet) {
        int last = Math.min(sheet.getLastRowNum(), MAX_HEADER_SEARCH_ROWS - 1);
        for (int r = 0; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (Cell cell : row) {
                String name = header(cell);
                if (!name.isBlank()) {
                    columns.putIfAbsent(name, cell.getColumnIndex());
                }
            }
            Integer ref = match(columns, REF_HEADER);
            if (ref == null) {
                continue;
            }
            Map<Integer, Integer> questions = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> column : columns.entrySet()) {
                Matcher matcher = QUESTION_HEADER.matcher(column.getKey());
                if (matcher.matches()) {
                    questions.put(Integer.parseInt(matcher.group(1)), column.getValue());
                }
            }
            boolean gapQuestion = columns.containsKey(GAP_QUESTION_HEADER);
            Integer whatItMeans = prefixed(columns, WHAT_IT_MEANS_PREFIX);
            Integer assessmentQs = assessmentQuestionsColumn(columns);
            if (questions.isEmpty() && !gapQuestion && whatItMeans == null && assessmentQs == null) {
                continue;  // a "Clause" column alone is not enough — could be any spreadsheet.
            }
            Map<Integer, Integer> ordered = new LinkedHashMap<>();
            questions.keySet().stream().sorted().forEach(k -> ordered.put(k, questions.get(k)));
            return new Layout(r, columns, ref, match(columns, SUB_CONTROL_REF_HEADER),
                    match(columns, TITLE_HEADER), definition(columns),
                    prefixed(columns, PRIORITY_PREFIX), prefixed(columns, APPLICABILITY_PREFIX),
                    whatItMeans, prefixed(columns, EVIDENCE_PREFIX), ordered,
                    columns.get("domain"),
                    subdomainColumn(columns),
                    ctrlColumn(columns),
                    assessmentQs);
        }
        return null;
    }

    private static Integer subdomainColumn(Map<String, Integer> columns) {
        for (Map.Entry<String, Integer> column : columns.entrySet()) {
            String key = column.getKey();
            if (key.startsWith("subdomain") || key.startsWith("sub-domain") || key.startsWith("sub domain")) {
                return column.getValue();
            }
        }
        return null;
    }

    private static Integer ctrlColumn(Map<String, Integer> columns) {
        for (Map.Entry<String, Integer> column : columns.entrySet()) {
            if (column.getKey().equals("ctrl #") || column.getKey().equals("ctrl#")
                    || column.getKey().equals("ctrl")) {
                return column.getValue();
            }
        }
        return null;
    }

    private static Integer assessmentQuestionsColumn(Map<String, Integer> columns) {
        for (Map.Entry<String, Integer> column : columns.entrySet()) {
            if (column.getKey().startsWith("assessment question")) {
                return column.getValue();
            }
        }
        return null;
    }

    private static String firstToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim().split("\\s+")[0];
        // "3.1.1" from "3.1.1 Cyber Security Governance"
        return t.replaceAll("[^0-9.]+$", "");
    }

    private static FrameworkTypeCode typeFor(String sheetName) {
        // Upper-cased because the client's sheet names are not consistently cased: "NIST CSF v2.0"
        // would match a token that "CIS Controls v8" would not.
        String squashed = sheetName.replace(" ", "").toUpperCase(Locale.ROOT);
        for (Map.Entry<String, FrameworkTypeCode> entry : TYPE_BY_TOKEN.entrySet()) {
            if (squashed.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Integer match(Map<String, Integer> columns, Pattern pattern) {
        for (Map.Entry<String, Integer> column : columns.entrySet()) {
            if (pattern.matcher(column.getKey()).matches()) {
                return column.getValue();
            }
        }
        return null;
    }

    /** First prefix that hits wins, so {@code Statement} beats {@code Requirement} on the UAE IA sheet. */
    private static Integer definition(Map<String, Integer> columns) {
        for (String prefix : DEFINITION_PREFIXES) {
            Integer column = prefixed(columns, prefix);
            if (column != null) {
                return column;
            }
        }
        return null;
    }

    private static Integer prefixed(Map<String, Integer> columns, String prefix) {
        for (Map.Entry<String, Integer> column : columns.entrySet()) {
            if (column.getKey().startsWith(prefix)) {
                return column.getValue();
            }
        }
        return null;
    }

    /** Headers carry embedded newlines ({@code "Clause\nRef"}) and parenthesised notes. */
    private String header(Cell cell) {
        String raw = formatter.formatCellValue(cell);
        int note = raw.indexOf('(');
        if (note > 0) {
            raw = raw.substring(0, note);
        }
        return raw.replace('\n', ' ').replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String cell(Row row, Integer index) {
        return cell(row, index, Map.of());
    }

    private String cell(Row row, Integer index, Map<Long, String> mergedDown) {
        if (index == null) {
            return "";
        }
        Cell cell = row.getCell(index);
        String direct = cell == null ? "" : formatter.formatCellValue(cell).trim();
        if (!direct.isBlank()) {
            return direct;
        }
        String filled = mergedDown.get(mergeKey(row.getRowNum(), index));
        return filled == null ? "" : filled;
    }

    /**
     * Values to copy onto blank cells that sit under the top of a single-column merge.
     * A merge that spans several columns (a SAMA section banner across the row) is ignored.
     */
    private Map<Long, String> verticalMergeFill(Sheet sheet) {
        Map<Long, String> fill = new HashMap<>();
        for (CellRangeAddress range : sheet.getMergedRegions()) {
            if (range.getFirstColumn() != range.getLastColumn()
                    || range.getFirstRow() >= range.getLastRow()) {
                continue;
            }
            int col = range.getFirstColumn();
            Row top = sheet.getRow(range.getFirstRow());
            String value = "";
            if (top != null) {
                Cell topCell = top.getCell(col);
                if (topCell != null) {
                    value = formatter.formatCellValue(topCell).trim();
                }
            }
            if (value.isBlank()) {
                continue;
            }
            for (int r = range.getFirstRow() + 1; r <= range.getLastRow(); r++) {
                fill.put(mergeKey(r, col), value);
            }
        }
        return fill;
    }

    private static long mergeKey(int row, int column) {
        return ((long) row << 32) | (column & 0xffffffffL);
    }

    private String json(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Workbook open(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            return new XSSFWorkbook(in);
        }
    }

    /**
     * @param columns         header name to zero-based column index, for the raw staging JSON
     * @param questionColumns question ordinal to column index, ascending
     */
    private record Layout(int headerRowIndex,
                          Map<String, Integer> columns,
                          Integer refColumn,
                          Integer subControlRefColumn,
                          Integer titleColumn,
                          Integer definitionColumn,
                          Integer priorityColumn,
                          Integer applicabilityColumn,
                          Integer whatItMeansColumn,
                          Integer evidenceColumn,
                          Map<Integer, Integer> questionColumns,
                          Integer domainColumn,
                          Integer subdomainColumn,
                          Integer ctrlColumn,
                          Integer assessmentQuestionsColumn) {
    }
}
