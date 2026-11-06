package com.a9.arbor.grc.modules.bcms.service;

import com.a9.arbor.grc.modules.bcms.pack.BiaLookups;
import com.a9.arbor.grc.modules.export.ExportFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
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
 * Arbor BIA process template: one visible Processes sheet (one row = one process).
 * A hidden Reference sheet feeds dropdowns only.
 */
@Component
public class BiaProcessImportWorkbook {

    static final String SHEET_PROCESSES = "Processes";
    static final String SHEET_REF = "Reference";
    static final int MAX_PROCESSES = 200;
    static final int DATA_ROWS = 200;
    static final int HEADER_ROW = 1;
    static final String SAMPLE_CODE = "SAMPLE";
    static final String SAMPLE_IGNORED_MESSAGE =
            "Sample row ignored. You can delete it or keep it. Arbor does not import Process Code SAMPLE.";

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    static final String COL_STAFF = "Dependency - Minimum Staff (emails)";
    static final String COL_ASSETS = "Dependency - Asset Type (Type: Detail; ...)";
    static final String COL_MOR_STAFF = "MOR Minimum Staff (emails)";
    static final String COL_CONTACTS = "Third party contacts (Name | Email | Phone)";

    static final String[] IMPACT_HEADERS = impactHeaders();

    static final String[] HEADERS = concat(
            new String[]{
                    "Process Code",
                    "Business Process Name",
                    "Process Description",
                    "Process Flow - Trigger / Inflow",
                    "Process Flow - Key Steps / Activities Performed by the Organization",
                    "Process Flow - Output / Outflow",
                    "Process Owner Email",
                    "Process Frequency",
                    "Third Party Involved",
                    "Activities Performed by Third Party",
                    "Third Party / Supplier Name",
                    COL_STAFF,
                    "Dependency - Premises / Facilities",
                    COL_ASSETS,
                    "Dependency - Vital Records / Data",
                    "Dependency - Third Party / Suppliers",
                    "Additional Notes"
            },
            IMPACT_HEADERS,
            new String[]{
                    COL_MOR_STAFF,
                    "Recovery Strategies",
                    "Internet",
                    "Intranet",
                    "VPN",
                    "Site",
                    "Software / Application names",
                    "Laptops",
                    "Microsoft 365",
                    COL_CONTACTS
            });

    public record ParsedProcess(
            int excelRow,
            String processCode,
            String processName,
            String description,
            String flowTrigger,
            String flowSteps,
            String flowOutput,
            String ownerEmail,
            String frequency,
            String thirdPartyInvolved,
            String thirdPartyActivities,
            String supplierName,
            String depPremises,
            String depRecords,
            String depThirdParty,
            String notes
    ) {
    }

    public record ParsedStaff(int excelRow, String processCode, String role, String email) {
    }

    public record ParsedAsset(int excelRow, String processCode, String type, String detail) {
    }

    public record ParsedImpact(int excelRow, String processCode, String category, Integer[] ratings) {
    }

    public record ParsedMor(
            int excelRow,
            String processCode,
            String recoveryStrategies,
            String internet,
            String intranet,
            String vpn,
            String site,
            String softwareApps,
            String laptops,
            String m365
    ) {
    }

    public record ParsedContact(int excelRow, String processCode, String name, String email, String phone) {
    }

    public record ParsedWorkbook(
            List<ParsedProcess> processes,
            List<ParsedStaff> staff,
            List<ParsedAsset> assets,
            List<ParsedImpact> impact,
            List<ParsedMor> mor,
            List<ParsedContact> contacts,
            int skippedBlankCount
    ) {
    }

    public ExportFile template(String displayId, List<String> ownerEmails) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet processes = wb.createSheet(SHEET_PROCESSES);
            writeHowTo(processes, displayId);
            Row header = processes.createRow(HEADER_ROW);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                processes.setColumnWidth(i, columnWidth(HEADERS[i]));
            }
            writeSampleRow(processes);
            processes.createFreezePane(2, HEADER_ROW + 1);
            writeReference(wb, ownerEmails == null ? List.of() : ownerEmails);
            addValidations(processes, ownerEmails == null ? 0 : ownerEmails.size());
            int refIdx = wb.getSheetIndex(SHEET_REF);
            if (refIdx >= 0) {
                wb.setSheetVisibility(refIdx, SheetVisibility.HIDDEN);
            }
            wb.write(out);
            String slug = displayId == null || displayId.isBlank()
                    ? "bia"
                    : displayId.replaceAll("[^a-zA-Z0-9._-]+", "-");
            return new ExportFile(out.toByteArray(), slug + "-process-template.xlsx", CONTENT_TYPE);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build BIA process template");
        }
    }

    public ParsedWorkbook parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an Arbor BIA process Excel file");
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = processSheet(wb);
            DataFormatter fmt = new DataFormatter();
            int headerRow = findHeaderRow(sheet, fmt);
            Map<String, Integer> col = requireHeaders(sheet.getRow(headerRow), HEADERS, SHEET_PROCESSES);
            List<ParsedProcess> processes = new ArrayList<>();
            List<ParsedStaff> staff = new ArrayList<>();
            List<ParsedAsset> assets = new ArrayList<>();
            List<ParsedImpact> impact = new ArrayList<>();
            List<ParsedMor> mor = new ArrayList<>();
            List<ParsedContact> contacts = new ArrayList<>();
            int skipped = 0;
            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, fmt, HEADERS.length)) {
                    skipped++;
                    continue;
                }
                int excelRow = i + 1;
                ParsedProcess process = readProcess(row, col, fmt, excelRow);
                processes.add(process);
                String code = process.processCode();
                staff.addAll(readStaff(excelRow, code, cell(row, col.get(key(COL_STAFF)), fmt), "Identity"));
                staff.addAll(readStaff(excelRow, code, cell(row, col.get(key(COL_MOR_STAFF)), fmt), "MOR"));
                assets.addAll(readAssets(excelRow, code, cell(row, col.get(key(COL_ASSETS)), fmt)));
                impact.addAll(readImpact(excelRow, code, row, col, fmt));
                ParsedMor morRow = readMor(excelRow, code, row, col, fmt);
                if (morRow != null) {
                    mor.add(morRow);
                }
                contacts.addAll(readContacts(excelRow, code, cell(row, col.get(key(COL_CONTACTS)), fmt)));
            }
            if (processes.size() > MAX_PROCESSES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "At most " + MAX_PROCESSES + " processes per import (found " + processes.size() + ")");
            }
            return new ParsedWorkbook(processes, staff, assets, impact, mor, contacts, skipped);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read workbook. Use the Arbor BIA process template.");
        }
    }

    static String monotonicError(Integer[] ratings) {
        if (ratings == null || ratings.length != 5) {
            return "Impact ratings must cover five time bands";
        }
        Integer last = null;
        for (Integer v : ratings) {
            if (v == null) {
                return "Fill all five time bands for this category (1-5)";
            }
            if (v < 1 || v > 5) {
                return "Impact rating must be 1-5";
            }
            if (last != null && v < last) {
                return "Later time bands cannot be lower than an earlier rating";
            }
            last = v;
        }
        return null;
    }

    private static String[] impactHeaders() {
        String[] cats = {"Strategic", "Financial", "Operational", "Compliance"};
        String[] bands = {"0-4 hours", "4-8 hours", "8-24 hours", "24-48 hours", "48-72 hours"};
        String[] out = new String[20];
        int i = 0;
        for (String cat : cats) {
            for (String band : bands) {
                out[i++] = cat + " " + band;
            }
        }
        return out;
    }

    private static String[] concat(String[] a, String[] b, String[] c) {
        String[] out = new String[a.length + b.length + c.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        System.arraycopy(c, 0, out, a.length + b.length, c.length);
        return out;
    }

    private static void writeHowTo(Sheet sheet, String displayId) {
        String bia = displayId == null || displayId.isBlank() ? "this department BIA" : displayId;
        Row row = sheet.createRow(0);
        row.setHeightInPoints(48);
        Cell cell = row.createCell(0);
        cell.setCellValue(
                "Arbor BIA process template (" + bia + "). One row = one process. "
                        + "Repeatable values use a semicolon. Staff / MOR staff: person@company.com; other@company.com. "
                        + "Assets: Software / Application: Email; Hardware / Endpoint: 2. "
                        + "Contacts: Name | email | phone. Recovery strategies: paste labels separated by semicolons. "
                        + "Rate impact 1-5; later time bands cannot be lower. Arbor calculates Tier, RTO and MAO. "
                        + "The grey SAMPLE row is an example. Delete it or keep it. Arbor ignores it on upload. "
                        + "Duplicate Process Codes are rejected. Max " + MAX_PROCESSES + " rows.");
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        cell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
    }

    public static boolean isSampleCode(String code) {
        return code != null && SAMPLE_CODE.equalsIgnoreCase(code.trim());
    }

    private static void writeSampleRow(Sheet sheet) {
        Row row = sheet.createRow(HEADER_ROW + 1);
        CellStyle style = sampleStyle(sheet);
        set(row, style, "Process Code", SAMPLE_CODE);
        set(row, style, "Business Process Name", "Sample process (ignored on upload)");
        set(row, style, "Process Description",
                "Example only. Delete this row or keep it. Arbor ignores Process Code SAMPLE.");
        set(row, style, "Process Flow - Trigger / Inflow", "Trigger");
        set(row, style, "Process Flow - Key Steps / Activities Performed by the Organization", "Step 1");
        set(row, style, "Process Flow - Output / Outflow", "Output");
        set(row, style, "Process Owner Email", "person@company.com");
        set(row, style, "Process Frequency", "Daily");
        set(row, style, "Third Party Involved", BiaLookups.NO);
        set(row, style, COL_STAFF, "person@company.com");
        set(row, style, "Dependency - Premises / Facilities", "HQ");
        set(row, style, COL_ASSETS, "Software / Application: Email");
        set(row, style, "Dependency - Vital Records / Data", "SharePoint");
        set(row, style, "Additional Notes", SAMPLE_IGNORED_MESSAGE);
        for (String impact : IMPACT_HEADERS) {
            setNum(row, style, impact, 1);
        }
        for (int i = 0; i < HEADERS.length; i++) {
            if (row.getCell(i) == null) {
                Cell empty = row.createCell(i);
                empty.setCellStyle(style);
            }
        }
    }

    private static CellStyle sampleStyle(Sheet sheet) {
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
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

    private static void setNum(Row row, CellStyle style, String header, int value) {
        int c = col(HEADERS, header);
        if (c < 0) {
            return;
        }
        Cell cell = row.createCell(c);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static int columnWidth(String header) {
        if (header.startsWith("Strategic ") || header.startsWith("Financial ")
                || header.startsWith("Operational ") || header.startsWith("Compliance ")) {
            return 14 * 256;
        }
        if (header.contains("Description") || header.contains("Key Steps") || header.contains("Asset Type")
                || header.contains("contacts") || header.contains("Recovery")) {
            return 28 * 256;
        }
        if (header.contains("Email") || header.contains("emails")) {
            return 24 * 256;
        }
        return 20 * 256;
    }

    private static void writeReference(XSSFWorkbook wb, List<String> ownerEmails) {
        Sheet ref = wb.createSheet(SHEET_REF);
        String[] headers = {
                "Process Frequency", "Yes / No", "Site", "Rating", "Recovery Strategy", "Process Owner Email"
        };
        Row h = ref.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            h.createCell(i).setCellValue(headers[i]);
            ref.setColumnWidth(i, 5600);
        }
        List<String> freqs = BiaLookups.FREQUENCIES;
        String[] yn = {BiaLookups.YES, BiaLookups.NO};
        String[] sites = {BiaLookups.SITE_PRIMARY, BiaLookups.SITE_SECONDARY, BiaLookups.SITE_OTHER};
        String[] ratings = {"1", "2", "3", "4", "5"};
        List<String> strategies = BiaLookups.RECOVERY_STRATEGIES;
        int max = Math.max(freqs.size(), Math.max(strategies.size(), Math.max(ownerEmails.size(), 5)));
        for (int i = 0; i < max; i++) {
            Row r = ref.createRow(i + 1);
            if (i < freqs.size()) {
                r.createCell(0).setCellValue(freqs.get(i));
            }
            if (i < yn.length) {
                r.createCell(1).setCellValue(yn[i]);
            }
            if (i < sites.length) {
                r.createCell(2).setCellValue(sites[i]);
            }
            if (i < ratings.length) {
                r.createCell(3).setCellValue(ratings[i]);
            }
            if (i < strategies.size()) {
                r.createCell(4).setCellValue(strategies.get(i));
            }
            if (i < ownerEmails.size()) {
                r.createCell(5).setCellValue(ownerEmails.get(i));
            }
        }
        defineListName(wb, "BiaFrequencies", 0, freqs.size());
        defineListName(wb, "BiaYesNo", 1, yn.length);
        defineListName(wb, "BiaSites", 2, sites.length);
        defineListName(wb, "BiaRatings", 3, ratings.length);
        defineListName(wb, "BiaStrategies", 4, strategies.size());
        if (!ownerEmails.isEmpty()) {
            defineListName(wb, "BiaOwners", 5, ownerEmails.size());
        }
    }

    private static void addValidations(Sheet processes, int ownerCount) {
        DataValidationHelper helper = processes.getDataValidationHelper();
        addNamed(helper, processes, "Process Frequency", "BiaFrequencies");
        addNamed(helper, processes, "Third Party Involved", "BiaYesNo");
        if (ownerCount > 0) {
            addNamed(helper, processes, "Process Owner Email", "BiaOwners");
        }
        for (String impact : IMPACT_HEADERS) {
            addNamed(helper, processes, impact, "BiaRatings");
        }
        addNamed(helper, processes, "Internet", "BiaYesNo");
        addNamed(helper, processes, "Intranet", "BiaYesNo");
        addNamed(helper, processes, "VPN", "BiaYesNo");
        addNamed(helper, processes, "Site", "BiaSites");
        addNamed(helper, processes, "Microsoft 365", "BiaYesNo");
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
                    "Missing sheet \"" + SHEET_PROCESSES + "\". Use the Arbor BIA process template.");
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
            if (first != null && key(first).equals(key("Process Code"))) {
                return i;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Missing header row. Use the Arbor BIA process template (one Processes sheet).");
    }

    private ParsedProcess readProcess(Row row, Map<String, Integer> col, DataFormatter fmt, int excelRow) {
        return new ParsedProcess(
                excelRow,
                cell(row, col.get(key("Process Code")), fmt),
                cell(row, col.get(key("Business Process Name")), fmt),
                cell(row, col.get(key("Process Description")), fmt),
                cell(row, col.get(key("Process Flow - Trigger / Inflow")), fmt),
                cell(row, col.get(key("Process Flow - Key Steps / Activities Performed by the Organization")), fmt),
                cell(row, col.get(key("Process Flow - Output / Outflow")), fmt),
                cell(row, col.get(key("Process Owner Email")), fmt),
                cell(row, col.get(key("Process Frequency")), fmt),
                cell(row, col.get(key("Third Party Involved")), fmt),
                cell(row, col.get(key("Activities Performed by Third Party")), fmt),
                cell(row, col.get(key("Third Party / Supplier Name")), fmt),
                cell(row, col.get(key("Dependency - Premises / Facilities")), fmt),
                cell(row, col.get(key("Dependency - Vital Records / Data")), fmt),
                cell(row, col.get(key("Dependency - Third Party / Suppliers")), fmt),
                cell(row, col.get(key("Additional Notes")), fmt));
    }

    private static List<ParsedStaff> readStaff(int excelRow, String code, String raw, String role) {
        List<ParsedStaff> out = new ArrayList<>();
        for (String email : splitSemi(raw)) {
            out.add(new ParsedStaff(excelRow, code, role, email));
        }
        return out;
    }

    private static List<ParsedAsset> readAssets(int excelRow, String code, String raw) {
        List<ParsedAsset> out = new ArrayList<>();
        for (String part : splitSemi(raw)) {
            int colon = part.indexOf(':');
            if (colon < 0) {
                out.add(new ParsedAsset(excelRow, code, part, ""));
            } else {
                out.add(new ParsedAsset(excelRow, code, part.substring(0, colon).trim(), part.substring(colon + 1).trim()));
            }
        }
        return out;
    }

    private static List<ParsedImpact> readImpact(int excelRow, String code, Row row,
                                                 Map<String, Integer> col, DataFormatter fmt) {
        String[] cats = {"Strategic", "Financial", "Operational", "Compliance"};
        String[] bands = {"0-4 hours", "4-8 hours", "8-24 hours", "24-48 hours", "48-72 hours"};
        boolean any = false;
        List<ParsedImpact> out = new ArrayList<>();
        for (String cat : cats) {
            Integer[] ratings = new Integer[5];
            for (int i = 0; i < bands.length; i++) {
                ratings[i] = parseRating(cell(row, col.get(key(cat + " " + bands[i])), fmt));
                if (ratings[i] != null) {
                    any = true;
                }
            }
            out.add(new ParsedImpact(excelRow, code, cat.toUpperCase(Locale.ROOT), ratings));
        }
        return any ? out : List.of();
    }

    private static ParsedMor readMor(int excelRow, String code, Row row, Map<String, Integer> col, DataFormatter fmt) {
        String recovery = cell(row, col.get(key("Recovery Strategies")), fmt);
        String internet = cell(row, col.get(key("Internet")), fmt);
        String intranet = cell(row, col.get(key("Intranet")), fmt);
        String vpn = cell(row, col.get(key("VPN")), fmt);
        String site = cell(row, col.get(key("Site")), fmt);
        String apps = cell(row, col.get(key("Software / Application names")), fmt);
        String laptops = cell(row, col.get(key("Laptops")), fmt);
        String m365 = cell(row, col.get(key("Microsoft 365")), fmt);
        String morStaff = cell(row, col.get(key(COL_MOR_STAFF)), fmt);
        if (isBlank(recovery) && isBlank(internet) && isBlank(intranet) && isBlank(vpn)
                && isBlank(site) && isBlank(apps) && isBlank(laptops) && isBlank(m365) && isBlank(morStaff)) {
            return null;
        }
        return new ParsedMor(excelRow, code, recovery, internet, intranet, vpn, site, apps, laptops, m365);
    }

    private static List<ParsedContact> readContacts(int excelRow, String code, String raw) {
        List<ParsedContact> out = new ArrayList<>();
        for (String part : splitSemi(raw)) {
            String[] bits = part.split("\\|");
            String name = bits.length > 0 ? bits[0].trim() : "";
            String email = bits.length > 1 ? bits[1].trim() : "";
            String phone = bits.length > 2 ? bits[2].trim() : "";
            out.add(new ParsedContact(excelRow, code, name, email, phone));
        }
        return out;
    }

    private static List<String> splitSemi(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split("[;,]")) {
            if (part != null && !part.isBlank()) {
                out.add(part.trim());
            }
        }
        return out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static Map<String, Integer> requireHeaders(Row header, String[] expected, String sheetName) {
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header row on " + sheetName);
        }
        Map<String, Integer> col = mapHeader(header);
        for (String h : expected) {
            if (!col.containsKey(key(h))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Missing column \"" + h + "\". Use the Arbor BIA process template.");
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

    private static String key(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
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

    private static Integer parseRating(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }
}
