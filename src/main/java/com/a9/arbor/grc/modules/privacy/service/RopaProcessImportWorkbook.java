package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.privacy.domain.RopaDesignation;
import com.a9.arbor.grc.modules.privacy.domain.RopaLegalBasis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
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

/**
 * Champion ROPA process template: sheet headers from
 * {@code Record of Processing Activities RoPA.xlsx} row 3 (Contributor columns).
 * Admin DPIA-required column is omitted. Multi-select cells use semicolons.
 */
@Component
public class RopaProcessImportWorkbook {

    static final String SHEET_PROCESSES = "Processes";
    static final String SHEET_REF = "Reference";
    static final int MAX_PROCESSES = 200;
    static final int DATA_ROWS = 200;
    static final int HEADER_ROW = 1;
    static final String SAMPLE_ACTIVITY = "SAMPLE";
    static final String SAMPLE_IGNORED_MESSAGE =
            "Sample row ignored. You can delete it or keep it. Arbor does not import Processing Activity SAMPLE.";

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String COL_ACTIVITY = "Processing Activity";
    static final String COL_DESIGNATION = "Data Controller or Data Processor?";
    static final String COL_DESCRIPTION = "Description of processing";
    static final String COL_PERSONAL = "List of Personal Data";
    static final String COL_SENSITIVE = "Categories of Sensitive Data";
    static final String COL_SUBJECTS = "Data Subject categories";
    static final String COL_VOLUME = "No. of data subjects whose personal data is processed";
    static final String COL_FREQUENCY = "Frequency of processing";
    static final String COL_SOURCE = "Source of Personal Data";
    static final String COL_METHOD = "Method of receiving Personal Data";
    static final String COL_LEGAL = "Legal basis for processing";
    static final String COL_INTERNAL = "Internal department";
    static final String COL_EXTERNAL = "External entities";
    static final String COL_EXTERNAL_DETAIL = "External entity detail";
    static final String COL_LOCATION = "Location";
    static final String COL_ADEQUACY = "Does the country have an adequate level of data protection? (Y/N)";
    static final String COL_SAFEGUARDS = "What safeguards have been implemented?";
    static final String COL_STORAGE = "Systems / other location where the personal data is stored";
    static final String COL_RETENTION = "Retention period";
    static final String COL_SECURITY =
            "Organizational, administrative, and technical measures for personal data security";
    static final String COL_COMMENTS = "Comments";

    static final String[] HEADERS = {
            COL_ACTIVITY,
            COL_DESIGNATION,
            COL_DESCRIPTION,
            COL_PERSONAL,
            COL_SENSITIVE,
            COL_SUBJECTS,
            COL_VOLUME,
            COL_FREQUENCY,
            COL_SOURCE,
            COL_METHOD,
            COL_LEGAL,
            COL_INTERNAL,
            COL_EXTERNAL,
            COL_EXTERNAL_DETAIL,
            COL_LOCATION,
            COL_ADEQUACY,
            COL_SAFEGUARDS,
            COL_STORAGE,
            COL_RETENTION,
            COL_SECURITY,
            COL_COMMENTS
    };

    private static final Map<String, RopaLegalBasis> LEGAL_LABELS = legalLabels();
    private static final Map<String, RopaDesignation> DESIGNATION_LABELS = designationLabels();

    private static Map<String, RopaDesignation> designationLabels() {
        Map<String, RopaDesignation> map = new LinkedHashMap<>();
        map.put(key("Controller"), RopaDesignation.CONTROLLER);
        map.put(key("Joint Controller"), RopaDesignation.JOINT_CONTROLLER);
        map.put(key("Processor"), RopaDesignation.PROCESSOR);
        for (RopaDesignation d : RopaDesignation.values()) {
            map.put(key(d.name()), d);
        }
        return map;
    }

    public record ParsedProcess(
            int excelRow,
            String activityName,
            String designationRaw,
            String processingDescription,
            String personalDataRaw,
            String sensitiveDataRaw,
            String dataSubjectsRaw,
            String dataSubjectVolume,
            String processingFrequency,
            String sourceOfPersonalData,
            String methodOfReceiving,
            String legalBasesRaw,
            String internalDepartment,
            String externalEntitiesRaw,
            String externalEntityDetail,
            String location,
            String adequacyRaw,
            String safeguardsImplemented,
            String storageSystems,
            String retentionPeriod,
            String securityMeasures,
            String comments
    ) {
    }

    public record ParsedWorkbook(List<ParsedProcess> processes, int skippedBlankCount) {
    }

    public ExportFile template(String departmentLabel) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet processes = wb.createSheet(SHEET_PROCESSES);
            writeHowTo(processes, departmentLabel);
            Row header = processes.createRow(HEADER_ROW);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                processes.setColumnWidth(i, columnWidth(HEADERS[i]));
            }
            writeSampleRow(processes);
            processes.createFreezePane(1, HEADER_ROW + 1);
            writeReference(wb);
            addValidations(processes);
            int refIdx = wb.getSheetIndex(SHEET_REF);
            if (refIdx >= 0) {
                wb.setSheetVisibility(refIdx, SheetVisibility.HIDDEN);
            }
            wb.write(out);
            String slug = departmentLabel == null || departmentLabel.isBlank()
                    ? "ropa"
                    : departmentLabel.replaceAll("[^a-zA-Z0-9._-]+", "-");
            return new ExportFile(out.toByteArray(), slug + "-process-template.xlsx", CONTENT_TYPE);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build ROPA process template");
        }
    }

    public ParsedWorkbook parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an Arbor ROPA process Excel file");
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = processSheet(wb);
            DataFormatter fmt = new DataFormatter();
            int headerRow = findHeaderRow(sheet, fmt);
            Map<String, Integer> col = requireHeaders(sheet.getRow(headerRow));
            List<ParsedProcess> processes = new ArrayList<>();
            int skipped = 0;
            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, fmt, HEADERS.length)) {
                    skipped++;
                    continue;
                }
                processes.add(readProcess(row, col, fmt, i + 1));
            }
            if (processes.size() > MAX_PROCESSES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "At most " + MAX_PROCESSES + " processes per import (found " + processes.size() + ")");
            }
            return new ParsedWorkbook(processes, skipped);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not read workbook. Use the Arbor ROPA process template.");
        }
    }

    public static boolean isSampleActivity(String name) {
        return name != null && SAMPLE_ACTIVITY.equalsIgnoreCase(name.trim());
    }

    public static RopaDesignation parseDesignation(String raw) {
        if (raw == null || raw.isBlank()) {
            return RopaDesignation.CONTROLLER;
        }
        RopaDesignation d = DESIGNATION_LABELS.get(key(raw));
        if (d == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data Controller or Data Processor? must be Controller, Joint Controller, or Processor");
        }
        return d;
    }

    public static String toJsonList(String raw, String fieldLabel) {
        List<String> values = splitMulti(raw);
        if (values.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " could not be encoded");
        }
    }

    public static String toLegalBasesJson(String raw) {
        List<String> labels = splitMulti(raw);
        if (labels.isEmpty()) {
            return null;
        }
        List<String> enums = new ArrayList<>();
        for (String label : labels) {
            RopaLegalBasis basis = LEGAL_LABELS.get(key(label));
            if (basis == null) {
                try {
                    basis = RopaLegalBasis.valueOf(label.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Legal basis for processing contains an unsupported value: " + label);
                }
            }
            enums.add(basis.name());
        }
        try {
            return MAPPER.writeValueAsString(enums);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Legal basis for processing could not be encoded");
        }
    }

    public static Boolean parseYesNo(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.equals("yes") || v.equals("y") || v.equals("true")) {
            return true;
        }
        if (v.equals("no") || v.equals("n") || v.equals("false")) {
            return false;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected Yes or No, got: " + raw);
    }

    public static String adequacyNote(String raw) {
        Boolean yn = parseYesNo(raw);
        if (yn == null) {
            return blankToNull(raw);
        }
        return yn ? "Yes" : "No";
    }

    private static Map<String, RopaLegalBasis> legalLabels() {
        Map<String, RopaLegalBasis> map = new LinkedHashMap<>();
        map.put(key("Consent"), RopaLegalBasis.CONSENT);
        map.put(key("Protect public interest"), RopaLegalBasis.PROTECT_PUBLIC_INTEREST);
        map.put(key("Data subject's actions"), RopaLegalBasis.DATA_SUBJECT_ACTIONS);
        map.put(key("Legal claims/defense"), RopaLegalBasis.LEGAL_CLAIMS_DEFENSE);
        map.put(key("Judicial/security procedures"), RopaLegalBasis.JUDICIAL_SECURITY_PROCEDURES);
        map.put(key("Occupational health assessments"), RopaLegalBasis.OCCUPATIONAL_HEALTH_ASSESSMENTS);
        map.put(key("Public health protection"), RopaLegalBasis.PUBLIC_HEALTH_PROTECTION);
        map.put(key("Archival/scientific studies"), RopaLegalBasis.ARCHIVAL_SCIENTIFIC_STUDIES);
        map.put(key("Protecting data subject's interests"), RopaLegalBasis.PROTECTING_DATA_SUBJECT_INTERESTS);
        map.put(key("Employment/social security obligations"), RopaLegalBasis.EMPLOYMENT_SOCIAL_SECURITY);
        map.put(key("Contract performance"), RopaLegalBasis.CONTRACT_PERFORMANCE);
        map.put(key("Compliance with other laws"), RopaLegalBasis.COMPLIANCE_WITH_OTHER_LAWS);
        map.put(key("Other cases as per regulations"), RopaLegalBasis.OTHER_CASES_AS_PER_REGULATIONS);
        for (RopaLegalBasis b : RopaLegalBasis.values()) {
            map.put(key(b.name()), b);
        }
        return map;
    }

    private static void writeHowTo(Sheet sheet, String departmentLabel) {
        String dept = departmentLabel == null || departmentLabel.isBlank() ? "this department" : departmentLabel;
        Row row = sheet.createRow(0);
        row.setHeightInPoints(56);
        Cell cell = row.createCell(0);
        cell.setCellValue(
                "Arbor ROPA process template (" + dept + "). One row = one processing activity. "
                        + "Multi-select columns (Personal Data, Sensitive Data, Data Subjects, Legal basis) "
                        + "use a semicolon between values. External entities is Yes/No; put the third-party "
                        + "name in External entity detail when Yes. Import creates drafts - open each draft "
                        + "and use Save Process before Submit ROPA. The grey SAMPLE row is an example and is "
                        + "ignored on upload. Max " + MAX_PROCESSES + " rows. Admin DPIA required is not "
                        + "in this template (set during DPO review).");
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
    }

    private static void writeSampleRow(Sheet sheet) {
        Row row = sheet.createRow(HEADER_ROW + 1);
        CellStyle style = sampleStyle(sheet);
        set(row, style, COL_ACTIVITY, SAMPLE_ACTIVITY);
        set(row, style, COL_DESIGNATION, "Controller");
        set(row, style, COL_DESCRIPTION, "Example only. Delete this row or keep it. Arbor ignores SAMPLE.");
        set(row, style, COL_PERSONAL, "Name; Email");
        set(row, style, COL_SENSITIVE, "Health-related information");
        set(row, style, COL_SUBJECTS, "Employees");
        set(row, style, COL_VOLUME, "0-1000");
        set(row, style, COL_FREQUENCY, "Daily");
        set(row, style, COL_SOURCE, "Data Subject");
        set(row, style, COL_METHOD, "Online form");
        set(row, style, COL_LEGAL, "Consent");
        set(row, style, COL_INTERNAL, "HR");
        set(row, style, COL_EXTERNAL, "No");
        set(row, style, COL_LOCATION, "UAE");
        set(row, style, COL_ADEQUACY, "Yes");
        set(row, style, COL_SAFEGUARDS, "Contractual clauses");
        set(row, style, COL_STORAGE, "SharePoint");
        set(row, style, COL_RETENTION, "7 years");
        set(row, style, COL_SECURITY, "Access control; encryption");
        set(row, style, COL_COMMENTS, SAMPLE_IGNORED_MESSAGE);
        for (int i = 0; i < HEADERS.length; i++) {
            if (row.getCell(i) == null) {
                Cell empty = row.createCell(i);
                empty.setCellStyle(style);
            }
        }
    }

    private static CellStyle sampleStyle(Sheet sheet) {
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static void writeReference(XSSFWorkbook wb) {
        Sheet ref = wb.createSheet(SHEET_REF);
        String[] headers = {
                "Designation", "Volume", "Frequency", "Yes / No", "Legal basis",
                "Personal Data", "Sensitive Data", "Data Subjects"
        };
        Row h = ref.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            h.createCell(i).setCellValue(headers[i]);
            ref.setColumnWidth(i, 5600);
        }
        String[] designations = {"Controller", "Joint Controller", "Processor"};
        List<String> volumes = RopaSheetFields.DATA_SUBJECT_VOLUMES;
        List<String> freqs = RopaSheetFields.FREQUENCIES;
        String[] yn = {"Yes", "No"};
        List<String> legal = List.of(
                "Consent", "Protect public interest", "Data subject's actions", "Legal claims/defense",
                "Judicial/security procedures", "Occupational health assessments", "Public health protection",
                "Archival/scientific studies", "Protecting data subject's interests",
                "Employment/social security obligations", "Contract performance",
                "Compliance with other laws", "Other cases as per regulations");
        List<String> personal = RopaSheetFields.PERSONAL_DATA;
        List<String> sensitive = RopaSheetFields.SENSITIVE_DATA;
        List<String> subjects = RopaSheetFields.DATA_SUBJECT_CATEGORIES;
        int max = Math.max(legal.size(), Math.max(personal.size(), Math.max(sensitive.size(), subjects.size())));
        for (int i = 0; i < max; i++) {
            Row r = ref.createRow(i + 1);
            if (i < designations.length) {
                r.createCell(0).setCellValue(designations[i]);
            }
            if (i < volumes.size()) {
                r.createCell(1).setCellValue(volumes.get(i));
            }
            if (i < freqs.size()) {
                r.createCell(2).setCellValue(freqs.get(i));
            }
            if (i < yn.length) {
                r.createCell(3).setCellValue(yn[i]);
            }
            if (i < legal.size()) {
                r.createCell(4).setCellValue(legal.get(i));
            }
            if (i < personal.size()) {
                r.createCell(5).setCellValue(personal.get(i));
            }
            if (i < sensitive.size()) {
                r.createCell(6).setCellValue(sensitive.get(i));
            }
            if (i < subjects.size()) {
                r.createCell(7).setCellValue(subjects.get(i));
            }
        }
        defineListName(wb, "RopaDesignations", 0, designations.length);
        defineListName(wb, "RopaVolumes", 1, volumes.size());
        defineListName(wb, "RopaFrequencies", 2, freqs.size());
        defineListName(wb, "RopaYesNo", 3, yn.length);
        defineListName(wb, "RopaLegalBases", 4, legal.size());
    }

    private static void addValidations(Sheet processes) {
        DataValidationHelper helper = processes.getDataValidationHelper();
        addNamed(helper, processes, COL_DESIGNATION, "RopaDesignations");
        addNamed(helper, processes, COL_VOLUME, "RopaVolumes");
        addNamed(helper, processes, COL_FREQUENCY, "RopaFrequencies");
        addNamed(helper, processes, COL_EXTERNAL, "RopaYesNo");
        addNamed(helper, processes, COL_ADEQUACY, "RopaYesNo");
    }

    private static void addNamed(DataValidationHelper helper, Sheet sheet, String header, String namedRange) {
        int c = col(HEADERS, header);
        if (c < 0) {
            return;
        }
        DataValidationConstraint constraint = helper.createFormulaListConstraint(namedRange);
        CellRangeAddressList address = new CellRangeAddressList(HEADER_ROW + 1, HEADER_ROW + DATA_ROWS, c, c);
        DataValidation validation = helper.createValidation(constraint, address);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        sheet.addValidationData(validation);
    }

    private static void defineListName(XSSFWorkbook wb, String name, int colIndex, int count) {
        if (count <= 0) {
            return;
        }
        Name n = wb.createName();
        n.setNameName(name);
        String col = excelCol(colIndex);
        n.setRefersToFormula(SHEET_REF + "!$" + col + "$2:$" + col + "$" + (count + 1));
    }

    private static String excelCol(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private static Sheet processSheet(XSSFWorkbook wb) {
        Sheet sheet = wb.getSheet(SHEET_PROCESSES);
        if (sheet == null) {
            sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
        }
        if (sheet == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing sheet \"" + SHEET_PROCESSES + "\". Use the Arbor ROPA process template.");
        }
        return sheet;
    }

    private static int findHeaderRow(Sheet sheet, DataFormatter fmt) {
        int last = Math.min(sheet.getLastRowNum(), 20);
        for (int i = 0; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            String first = cell(row, 0, fmt);
            if (first != null && key(first).equals(key(COL_ACTIVITY))) {
                return i;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Missing header row. Use the Arbor ROPA process template.");
    }

    private static ParsedProcess readProcess(Row row, Map<String, Integer> col, DataFormatter fmt, int excelRow) {
        return new ParsedProcess(
                excelRow,
                cell(row, col.get(key(COL_ACTIVITY)), fmt),
                cell(row, col.get(key(COL_DESIGNATION)), fmt),
                cell(row, col.get(key(COL_DESCRIPTION)), fmt),
                cell(row, col.get(key(COL_PERSONAL)), fmt),
                cell(row, col.get(key(COL_SENSITIVE)), fmt),
                cell(row, col.get(key(COL_SUBJECTS)), fmt),
                cell(row, col.get(key(COL_VOLUME)), fmt),
                cell(row, col.get(key(COL_FREQUENCY)), fmt),
                cell(row, col.get(key(COL_SOURCE)), fmt),
                cell(row, col.get(key(COL_METHOD)), fmt),
                cell(row, col.get(key(COL_LEGAL)), fmt),
                cell(row, col.get(key(COL_INTERNAL)), fmt),
                cell(row, col.get(key(COL_EXTERNAL)), fmt),
                cell(row, col.get(key(COL_EXTERNAL_DETAIL)), fmt),
                cell(row, col.get(key(COL_LOCATION)), fmt),
                cell(row, col.get(key(COL_ADEQUACY)), fmt),
                cell(row, col.get(key(COL_SAFEGUARDS)), fmt),
                cell(row, col.get(key(COL_STORAGE)), fmt),
                cell(row, col.get(key(COL_RETENTION)), fmt),
                cell(row, col.get(key(COL_SECURITY)), fmt),
                cell(row, col.get(key(COL_COMMENTS)), fmt));
    }

    private static Map<String, Integer> requireHeaders(Row header) {
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header row on Processes");
        }
        Map<String, Integer> col = mapHeader(header);
        for (String h : HEADERS) {
            if (!col.containsKey(key(h))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Missing column \"" + h + "\". Use the Arbor ROPA process template.");
            }
        }
        return col;
    }

    private static Map<String, Integer> mapHeader(Row header) {
        DataFormatter fmt = new DataFormatter();
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Cell cell : header) {
            String v = fmt.formatCellValue(cell);
            if (v != null && !v.isBlank()) {
                map.put(key(v), cell.getColumnIndex());
            }
        }
        return map;
    }

    private static void set(Row row, CellStyle style, String header, String value) {
        int c = col(HEADERS, header);
        if (c < 0) {
            return;
        }
        Cell cell = row.createCell(c);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static int columnWidth(String header) {
        if (header.length() > 40) {
            return 28 * 256;
        }
        if (header.contains("Description") || header.contains("safeguards") || header.contains("measures")) {
            return 26 * 256;
        }
        return 20 * 256;
    }

    private static int col(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private static String cell(Row row, Integer idx, DataFormatter fmt) {
        if (idx == null) {
            return null;
        }
        Cell cell = row.getCell(idx);
        if (cell == null) {
            return null;
        }
        String v = fmt.formatCellValue(cell);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static boolean isBlankRow(Row row, DataFormatter fmt, int cols) {
        for (int i = 0; i < cols; i++) {
            Cell c = row.getCell(i);
            if (c != null) {
                String v = fmt.formatCellValue(c);
                if (v != null && !v.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<String> splitMulti(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[;|]")) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String key(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
