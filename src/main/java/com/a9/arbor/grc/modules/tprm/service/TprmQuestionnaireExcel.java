package com.a9.arbor.grc.modules.tprm.service;

import com.a9.arbor.grc.modules.tprm.domain.TprmAnswer;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessmentItem;
import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.pack.TprmControlDisplay;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
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

/** Sample Questionnaire layout from the client pack. */
@Component
public class TprmQuestionnaireExcel {

    static final String[] HEADERS = {
            "Control ID", "Domain", "Requirement", "Required evidence", "Evidence mandatory",
            "Vendor answer", "Justification", "Evidence reference"
    };

    static final String[] TEMPLATE_HEADERS = {
            "Control ID", "Domain", "Control requirement", "Required evidence", "Evidence Mandatory"
    };

    private static final int TEMPLATE_DATA_ROWS = 1000;
    private static final int EVIDENCE_MANDATORY_COL = 4;

    public record TemplateParsed(
            String controlCode,
            String domain,
            String requirement,
            String requiredEvidence,
            boolean evidenceMandatory
    ) {
    }

    /** Admin upload template: header row only. No library or example questions. */
    public byte[] writeBlankTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Questionnaire");
            Row header = sheet.createRow(0);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                header.createCell(i).setCellValue(TEMPLATE_HEADERS[i]);
                sheet.setColumnWidth(i, 22 * 256);
            }
            sheet.setColumnWidth(EVIDENCE_MANDATORY_COL, 28 * 256);
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidationConstraint yesNo = helper.createExplicitListConstraint(new String[]{"Yes", "No"});
            CellRangeAddressList address = new CellRangeAddressList(
                    1, TEMPLATE_DATA_ROWS, EVIDENCE_MANDATORY_COL, EVIDENCE_MANDATORY_COL);
            DataValidation validation = helper.createValidation(yesNo, address);
            validation.setSuppressDropDownArrow(false);
            validation.setShowErrorBox(true);
            validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            validation.createErrorBox("Evidence Mandatory", "Select Yes or No.");
            sheet.addValidationData(validation);
            sheet.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write TPRM questionnaire template", e);
        }
    }

    public List<TemplateParsed> parseTemplate(byte[] bytes) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook has no sheets");
            }
            Row header = sheet.getRow(0);
            Map<String, Integer> cols = headerIndex(header);
            requireCol(cols, "control id");
            DataFormatter fmt = new DataFormatter();
            List<TemplateParsed> out = new ArrayList<>();
            LinkedHashMap<String, String> seen = new LinkedHashMap<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String code = cell(fmt, row, cols.get("control id"));
                if (code == null || code.isBlank()) {
                    continue;
                }
                String trimmed = code.trim();
                String key = trimmed.toLowerCase(Locale.ROOT);
                if (seen.put(key, trimmed) != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Control ID \"" + trimmed + "\" is duplicated in the workbook");
                }
                String mand = cell(fmt, row, col(cols, "evidence mandatory"));
                boolean mandatory = isYes(mand);
                out.add(new TemplateParsed(
                        trimmed,
                        cell(fmt, row, col(cols, "domain")),
                        cell(fmt, row, col(cols, "control requirement", "requirement")),
                        cell(fmt, row, col(cols, "required evidence")),
                        mandatory));
            }
            return out;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read questionnaire Excel");
        }
    }

    public record RowView(
            TprmControl control,
            TprmAssessmentItem item,
            boolean evidenceMandatory
    ) {
    }

    public record ParsedRow(
            String controlCode,
            TprmAnswer answer,
            String justification,
            String evidenceRef
    ) {
    }

    private static final int VENDOR_ANSWER_COL = 5;

    public byte[] write(List<RowView> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Questionnaire");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                sheet.setColumnWidth(i, 22 * 256);
            }
            sheet.setColumnWidth(2, 40 * 256);
            sheet.setColumnWidth(VENDOR_ANSWER_COL, 18 * 256);
            int r = 1;
            for (RowView view : rows) {
                TprmControl c = view.control();
                TprmAssessmentItem item = view.item();
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(TprmControlDisplay.code(item, c));
                row.createCell(1).setCellValue(TprmControlDisplay.domain(item, c));
                row.createCell(2).setCellValue(TprmControlDisplay.requirement(item, c));
                row.createCell(3).setCellValue(TprmControlDisplay.requiredEvidence(item, c));
                row.createCell(4).setCellValue(view.evidenceMandatory() ? "Yes" : "No");
                row.createCell(5).setCellValue(item.getVendorAnswer() != null ? item.getVendorAnswer().label() : "");
                row.createCell(6).setCellValue(item.getJustification() != null ? item.getJustification() : "");
                row.createCell(7).setCellValue(item.getEvidenceRef() != null ? item.getEvidenceRef() : "");
            }
            if (!rows.isEmpty()) {
                String[] answerOptions = new String[TprmAnswer.values().length];
                int i = 0;
                for (TprmAnswer answer : TprmAnswer.values()) {
                    answerOptions[i++] = answer.label();
                }
                DataValidationHelper helper = sheet.getDataValidationHelper();
                DataValidationConstraint list = helper.createExplicitListConstraint(answerOptions);
                CellRangeAddressList address = new CellRangeAddressList(
                        1, rows.size(), VENDOR_ANSWER_COL, VENDOR_ANSWER_COL);
                DataValidation validation = helper.createValidation(list, address);
                validation.setSuppressDropDownArrow(false);
                validation.setShowErrorBox(true);
                validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                validation.setEmptyCellAllowed(true);
                validation.createErrorBox("Vendor answer", "Select Yes, Partial, No, or N/A.");
                sheet.addValidationData(validation);
            }
            sheet.createFreezePane(0, 1);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write TPRM questionnaire Excel", e);
        }
    }

    public List<ParsedRow> parse(byte[] bytes) {
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook has no sheets");
            }
            Row header = sheet.getRow(0);
            Map<String, Integer> cols = headerIndex(header);
            requireCol(cols, "control id");
            DataFormatter fmt = new DataFormatter();
            List<ParsedRow> out = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String code = cell(fmt, row, cols.get("control id"));
                if (code == null || code.isBlank()) {
                    continue;
                }
                String answerRaw = cell(fmt, row, cols.getOrDefault("vendor answer", -1));
                TprmAnswer answer = null;
                if (answerRaw != null && !answerRaw.isBlank()) {
                    try {
                        answer = TprmAnswer.parse(answerRaw);
                    } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Row " + (i + 1) + ": " + e.getMessage());
                    }
                }
                out.add(new ParsedRow(
                        code.trim(),
                        answer,
                        cell(fmt, row, cols.getOrDefault("justification", -1)),
                        cell(fmt, row, cols.getOrDefault("evidence reference", -1))));
            }
            return out;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read questionnaire Excel");
        }
    }

    private static boolean isYes(String mand) {
        if (mand == null) {
            return false;
        }
        String v = mand.trim();
        return "yes".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v) || "1".equals(v);
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

    private static void requireCol(Map<String, Integer> cols, String name) {
        if (!cols.containsKey(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing column: " + name);
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
